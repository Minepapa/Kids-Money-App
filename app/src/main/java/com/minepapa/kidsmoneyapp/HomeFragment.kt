package com.minepapa.kidsmoneyapp

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.minepapa.kidsmoneyapp.databinding.DialogBankSettingsBinding
import com.minepapa.kidsmoneyapp.databinding.FragmentHomeBinding
import java.time.LocalDate
import java.time.YearMonth

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private val prefs by lazy { requireContext().getSharedPreferences("kids_money", Context.MODE_PRIVATE) }
    private var selectedDate: String = LocalDate.now().toString()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = DatabaseHelper(requireContext())

        applyInterestIfNeeded()

        binding.etDate.text = selectedDate
        binding.etDate.setOnClickListener { openDatePicker() }

        binding.typeToggleGroup.check(R.id.btnTypeExpense)

        listOf(
            binding.btnAmt500 to 500,
            binding.btnAmt1000 to 1000,
            binding.btnAmt5000 to 5000,
            binding.btnAmt10000 to 10000
        ).forEach { (btn, amount) ->
            btn.setOnClickListener { binding.etAmount.setText(amount.toString()) }
        }

        binding.btnAdd.setOnClickListener { addRecord() }
        binding.btnBankSettings.setOnClickListener { openBankSettings() }

        render()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openDatePicker() {
        val d = runCatching { LocalDate.parse(selectedDate) }.getOrElse { LocalDate.now() }
        DatePickerDialog(requireContext(), { _, y, m, day ->
            selectedDate = "%04d-%02d-%02d".format(y, m + 1, day)
            binding.etDate.text = selectedDate
        }, d.year, d.monthValue - 1, d.dayOfMonth).show()
    }

    private fun openBankSettings() {
        val dialogView = DialogBankSettingsBinding.inflate(layoutInflater)
        val currentRate = prefs.getInt("bank_interest_rate", 10)
        dialogView.seekBarRate.progress = currentRate - 1
        dialogView.tvRateValue.text = "${currentRate}%"
        dialogView.seekBarRate.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                dialogView.tvRateValue.text = "${progress + 1}%"
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        AlertDialog.Builder(requireContext())
            .setView(dialogView.root)
            .setPositiveButton("저장") { _, _ ->
                val rate = dialogView.seekBarRate.progress + 1
                prefs.edit().putInt("bank_interest_rate", rate).apply()
                prefs.edit().remove("last_interest_month").apply()
                applyInterestIfNeeded()
                render()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun applyInterestIfNeeded() {
        val rate = prefs.getInt("bank_interest_rate", 10)
        val lastMonth = prefs.getString("last_interest_month", null)
        val newLastMonth = db.applyMonthlyInterestIfNeeded(rate, lastMonth)
        if (newLastMonth != null) {
            prefs.edit().putString("last_interest_month", newLastMonth).apply()
        }
    }

    private fun addRecord() {
        val date = selectedDate
        val memo = binding.etMemo.text.toString()
        val amountStr = binding.etAmount.text.toString()

        if (memo.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "내용을 모두 입력해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toIntOrNull() ?: run {
            Toast.makeText(requireContext(), "금액을 숫자로 입력해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        val type = when (binding.typeToggleGroup.checkedButtonId) {
            R.id.btnTypeIncome -> "income"
            R.id.btnTypeBank -> "tobank"
            else -> "expense"
        }

        val (wallet, _) = getBalances()
        if ((type == "expense" || type == "tobank") && amount > wallet) {
            Toast.makeText(requireContext(), "잔액이 부족해요! 현재: ${wallet.formatted()}원", Toast.LENGTH_SHORT).show()
            return
        }

        db.insertRecord(Record(date = date, type = type, memo = memo, amount = amount))
        binding.etMemo.setText("")
        binding.etAmount.setText("")

        val newlyUnlocked = AchievementManager.checkAndUnlock(db)
        newlyUnlocked.firstOrNull()?.let { a ->
            Snackbar.make(binding.root, "${a.emoji} ${a.titleKo} 뱃지를 받았어요!", Snackbar.LENGTH_LONG).show()
            db.markAchievementSeen(a.id)
        }

        render()
        showDetail(date)
    }

    private fun getBalances(): Pair<Int, Int> {
        var wallet = 0; var bank = 0
        db.getAllRecords().forEach {
            when (it.type) {
                "income"    -> wallet += it.amount
                "expense"   -> wallet -= it.amount
                "tobank"    -> { wallet -= it.amount; bank += it.amount }
                "frombank"  -> { wallet += it.amount; bank -= it.amount }
                "direct_in" -> bank += it.amount
                "interest"  -> bank += it.amount
            }
        }
        return Pair(wallet, bank)
    }

    private fun render() {
        val (wallet, bank) = getBalances()
        binding.tvWallet.text = "${wallet.formatted()}원"
        binding.tvBank.text = "${bank.formatted()}원"

        val (principal, interest) = db.getBankBalancesBreakdown()
        val rate = prefs.getInt("bank_interest_rate", 10)
        if (interest > 0) {
            binding.bankDetailLayout.visibility = View.VISIBLE
            binding.tvBankPrincipal.text = "원금  ${principal.formatted()}원"
            binding.tvBankInterest.text = "이자 +${interest.formatted()}원 🌟"
            binding.tvBankRate.text = "월 이자율 ${rate}%"
        } else {
            binding.bankDetailLayout.visibility = View.GONE
            binding.tvBankRate.text = "월 이자율 ${rate}%"
        }

        val streak = db.getRecordingStreak()
        if (streak >= 2) {
            binding.tvStreak.visibility = View.VISIBLE
            binding.tvStreak.text = "🔥 ${streak}일 연속 기록 중!"
        } else {
            binding.tvStreak.visibility = View.GONE
        }
    }

    private fun showDetail(dateStr: String) {
        val records = db.getRecordsByDate(dateStr)
        binding.detailView.visibility = View.VISIBLE
        binding.tvDetailDate.text = "📍 $dateStr"
        binding.llDayList.removeAllViews()

        if (records.isEmpty()) {
            val tv = TextView(requireContext())
            tv.text = "내역 없음"
            tv.setTextColor(0xFF999999.toInt())
            binding.llDayList.addView(tv)
            return
        }

        records.forEach { r ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 6, 0, 6)
            }
            val color = when {
                r.isIncome -> 0xFF2ecc71.toInt()
                r.isExpense -> 0xFFe74c3c.toInt()
                else -> 0xFFd4ac0d.toInt()
            }
            val label = when {
                r.isIncome -> "수입"
                r.isExpense -> "지출"
                r.type == "interest" -> "이자"
                else -> "금고"
            }
            row.addView(TextView(requireContext()).apply {
                text = label; setTextColor(0xFFFFFFFF.toInt()); setBackgroundColor(color)
                textSize = 9f; setPadding(6, 2, 6, 2)
            })
            row.addView(TextView(requireContext()).apply {
                text = "  ${r.memo}"; textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(requireContext()).apply {
                text = "${r.amount.formatted()}원"; setTextColor(color)
                textSize = 11f; typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            if (r.type != "interest") {
                row.addView(Button(requireContext()).apply {
                    text = "X"; textSize = 9f; setPadding(8, 2, 8, 2)
                    setOnClickListener { db.deleteRecord(r.id); render(); showDetail(dateStr) }
                })
            }
            binding.llDayList.addView(row)
        }
    }

    private fun Int.formatted() = String.format("%,d", this)
}
