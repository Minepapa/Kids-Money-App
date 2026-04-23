package com.minepapa.kidsmoneyapp

import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.minepapa.kidsmoneyapp.databinding.FragmentCalendarBinding
import java.time.LocalDate
import java.time.YearMonth

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private var viewDate: YearMonth = YearMonth.now()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = DatabaseHelper(requireContext())

        binding.btnPrevMonth.setOnClickListener { viewDate = viewDate.minusMonths(1); render() }
        binding.btnNextMonth.setOnClickListener { viewDate = viewDate.plusMonths(1); render() }

        render()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render() {
        binding.tvMonth.text = "${viewDate.year}년 ${viewDate.monthValue}월"

        val allRecords = db.getAllRecords().filter { it.date.startsWith(viewDate.toString()) }
        var mInc = 0; var mExp = 0; var mBnk = 0
        allRecords.forEach {
            when (it.type) {
                "income", "frombank" -> mInc += it.amount
                "expense" -> mExp += it.amount
                "tobank", "direct_in", "interest" -> mBnk += it.amount
            }
        }
        binding.tvMonthInc.text = "📈 수입\n${mInc.formatted()}원"
        binding.tvMonthExp.text = "📉 지출\n${mExp.formatted()}원"
        binding.tvMonthBnk.text = "🏦 저축\n${mBnk.formatted()}원"

        buildCalendar()
    }

    private fun buildCalendar() {
        binding.calendarGrid.removeAllViews()

        // 화면 높이의 약 70%를 7행(헤더+6주)으로 나눠 셀 높이 계산
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val cellHeight = (screenHeight * 0.62 / 7).toInt()

        val days = listOf("일", "월", "화", "수", "목", "금", "토")
        days.forEach { d ->
            val tv = TextView(requireContext()).apply {
                text = d
                gravity = Gravity.CENTER
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = cellHeight
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            binding.calendarGrid.addView(tv)
        }

        val firstDay = viewDate.atDay(1).dayOfWeek.value % 7
        val lastDay = viewDate.lengthOfMonth()
        val today = LocalDate.now().toString()

        repeat(firstDay) {
            val empty = TextView(requireContext())
            empty.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = cellHeight
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
                    "tobank", "direct_in", "interest" -> dBnk += it.amount
                }
            }

            val cell = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(2, 2, 2, 2)
                setBackgroundColor(if (dStr == today) 0xFFFFF9E6.toInt() else 0xFFFFFFFF.toInt())
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = cellHeight
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(1, 1, 1, 1)
                }
                setOnClickListener { showDetail(dStr) }
            }

            cell.addView(TextView(requireContext()).apply {
                text = "$d"
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            if (dInc > 0) cell.addView(TextView(requireContext()).apply {
                text = "📈${dInc.formatted()}"
                textSize = 10f
                setTextColor(0xFF2ecc71.toInt())
            })
            if (dExp > 0) cell.addView(TextView(requireContext()).apply {
                text = "📉${dExp.formatted()}"
                textSize = 10f
                setTextColor(0xFFe74c3c.toInt())
            })
            if (dBnk > 0) cell.addView(TextView(requireContext()).apply {
                text = "🏦${dBnk.formatted()}"
                textSize = 10f
                setTextColor(0xFFd4ac0d.toInt())
            })

            binding.calendarGrid.addView(cell)
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
                textSize = 13f; setPadding(6, 2, 6, 2)
            })
            row.addView(TextView(requireContext()).apply {
                text = "  ${r.memo}"; textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(requireContext()).apply {
                text = "${r.amount.formatted()}원"; setTextColor(color)
                textSize = 13f; typeface = Typeface.DEFAULT_BOLD
            })
            row.addView(TextView(requireContext()).apply {
                text = "🗑️"; textSize = 16f; setPadding(8, 2, 8, 2)
                setOnClickListener { db.deleteRecord(r.id); render(); showDetail(dateStr) }
            })
            binding.llDayList.addView(row)
        }
    }

    private fun Int.formatted() = String.format("%,d", this)
}
