package com.minepapa.kidsmoneyapp

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.minepapa.kidsmoneyapp.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: DatabaseHelper
    private var viewDate: YearMonth = YearMonth.now()
    private val prefs by lazy { getSharedPreferences("kids_money", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        // 타입 스피너 설정
        val types = listOf("📉 지출", "📈 수입", "🏦 금고 저축")
        binding.spinnerType.adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, types)

        // 오늘 날짜 기본값
        binding.etDate.setText(LocalDate.now().toString())

        // 월 이동 버튼
        binding.btnPrevMonth.setOnClickListener {
            viewDate = viewDate.minusMonths(1)
            render()
        }
        binding.btnNextMonth.setOnClickListener {
            viewDate = viewDate.plusMonths(1)
            render()
        }

        // 기록 추가
        binding.btnAdd.setOnClickListener { addRecord() }

        render()
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
            }
        }
        return Pair(wallet, bank)
    }

    private fun addRecord() {
        val date = binding.etDate.text.toString()
        val memo = binding.etMemo.text.toString()
        val amountStr = binding.etAmount.text.toString()

        if (date.isEmpty() || memo.isEmpty() || amountStr.isEmpty()) {
            Toast.makeText(this, "내용을 모두 입력해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toIntOrNull() ?: run {
            Toast.makeText(this, "금액을 숫자로 입력해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        val type = when (binding.spinnerType.selectedItemPosition) {
            1 -> "income"
            2 -> "tobank"
            else -> "expense"
        }

        val (wallet, _) = getBalances()
        if ((type == "expense" || type == "tobank") && amount > wallet) {
            Toast.makeText(this, "잔액이 부족해요! 현재: ${wallet.formatted()}원",
                Toast.LENGTH_SHORT).show()
            return
        }

        db.insertRecord(Record(date = date, type = type, memo = memo, amount = amount))
        binding.etMemo.setText("")
        binding.etAmount.setText("")
        render()
        showDetail(date)
    }

    private fun showDetail(dateStr: String) {
        val records = db.getRecordsByDate(dateStr)
        binding.detailView.visibility = android.view.View.VISIBLE
        binding.tvDetailDate.text = "📍 $dateStr"
        binding.llDayList.removeAllViews()

        if (records.isEmpty()) {
            val tv = TextView(this)
            tv.text = "내역 없음"
            tv.setTextColor(0xFF999999.toInt())
            binding.llDayList.addView(tv)
            return
        }

        records.forEach { r ->
            val row = LinearLayout(this).apply {
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
                else -> "금고"
            }

            val tag = TextView(this).apply {
                text = label
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(color)
                textSize = 9f
                setPadding(6, 2, 6, 2)
            }

            val memoTv = TextView(this).apply {
                text = "  ${r.memo}"
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val amountTv = TextView(this).apply {
                text = "${r.amount.formatted()}원"
                setTextColor(color)
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val delBtn = Button(this).apply {
                text = "X"
                textSize = 9f
                setPadding(8, 2, 8, 2)
                setOnClickListener {
                    db.deleteRecord(r.id)
                    render()
                    showDetail(dateStr)
                }
            }

            row.addView(tag)
            row.addView(memoTv)
            row.addView(amountTv)
            row.addView(delBtn)
            binding.llDayList.addView(row)
        }
    }

    private fun render() {
        val (wallet, bank) = getBalances()
        binding.tvWallet.text = "${wallet.formatted()}원"
        binding.tvBank.text = "${bank.formatted()}원"
        binding.tvMonth.text = "${viewDate.year}년 ${viewDate.monthValue}월"

        // 월 통계
        val allRecords = db.getAllRecords()
            .filter { it.date.startsWith(viewDate.toString()) }
        var mInc = 0; var mExp = 0; var mBnk = 0
        allRecords.forEach {
            when (it.type) {
                "income", "frombank" -> mInc += it.amount
                "expense" -> mExp += it.amount
                "tobank", "direct_in" -> mBnk += it.amount
            }
        }
        binding.tvMonthInc.text = "📈 수입\n${mInc.formatted()}원"
        binding.tvMonthExp.text = "📉 지출\n${mExp.formatted()}원"
        binding.tvMonthBnk.text = "🏦 저축\n${mBnk.formatted()}원"

        // 캘린더
        buildCalendar()
    }

    private fun buildCalendar() {
        binding.calendarGrid.removeAllViews()
        val days = listOf("일","월","화","수","목","금","토")
        days.forEach { d ->
            val tv = TextView(this).apply {
                text = d
                gravity = Gravity.CENTER
                textSize = 9f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            binding.calendarGrid.addView(tv)
        }

        val firstDay = viewDate.atDay(1).dayOfWeek.value % 7
        val lastDay = viewDate.lengthOfMonth()
        val today = LocalDate.now().toString()

        repeat(firstDay) {
            val empty = TextView(this)
            empty.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            binding.calendarGrid.addView(empty)
        }

        for (d in 1..lastDay) {
            val dStr = "%s-%02d".format(viewDate, d)
            val dayRecords = db.getRecordsByDate(dStr)
            var dInc = 0; var dExp = 0; var dBnk = 0
            dayRecords.forEach {
                when (it.type) {
                    "income", "frombank" -> dInc += it.amount
                    "expense" -> dExp += it.amount
                    "tobank", "direct_in" -> dBnk += it.amount
                }
            }

            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(2, 2, 2, 2)
                setBackgroundColor(if (dStr == today) 0xFFFFF9E6.toInt() else 0xFFFFFFFF.toInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(1, 1, 1, 1)
                }
                setOnClickListener { showDetail(dStr) }
            }

            cell.addView(TextView(this).apply {
                text = "$d"
                textSize = 9f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            if (dInc > 0) cell.addView(TextView(this).apply {
                text = "+$dInc"
                textSize = 7f
                setTextColor(0xFF2ecc71.toInt())
            })
            if (dExp > 0) cell.addView(TextView(this).apply {
                text = "-$dExp"
                textSize = 7f
                setTextColor(0xFFe74c3c.toInt())
            })
            if (dBnk > 0) cell.addView(TextView(this).apply {
                text = "🏦$dBnk"
                textSize = 7f
                setTextColor(0xFFd4ac0d.toInt())
            })

            binding.calendarGrid.addView(cell)
        }
    }

    private fun Int.formatted() = String.format("%,d", this)
}
