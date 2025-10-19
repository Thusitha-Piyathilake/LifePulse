package com.example.lifepulse.ui.main.fragments

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lifepulse.R
import com.example.lifepulse.data.PrefsManager

class DateDetailsFragment : Fragment() {

    private lateinit var recyclerActivities: RecyclerView
    private lateinit var adapter: ActivityAdapter
    private lateinit var tvDate: TextView
    private lateinit var tvWaterProgress: TextView
    private lateinit var progressWater: ProgressBar

    private var selectedDate: String? = null

    companion object {
        private const val ARG_DATE = "selectedDate"

        fun newInstance(date: String): DateDetailsFragment {
            val fragment = DateDetailsFragment()
            val args = Bundle()
            args.putString(ARG_DATE, date)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDate = arguments?.getString(ARG_DATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_date_details, container, false)

        recyclerActivities = view.findViewById(R.id.recyclerActivities)
        tvDate = view.findViewById(R.id.tvSelectedDate)
        tvWaterProgress = view.findViewById(R.id.tvWaterProgress)
        progressWater = view.findViewById(R.id.progressWater)

        selectedDate?.let { loadData(it) }

        return view
    }

    private fun loadData(date: String) {
        tvDate.text = "Activities on $date"

        val habits = PrefsManager.getHabitsForDate(requireContext(), date)

        adapter = ActivityAdapter(requireContext(), habits.toMutableList(), date)
        recyclerActivities.layoutManager = LinearLayoutManager(requireContext())
        recyclerActivities.adapter = adapter

        //  Swipe: Left = Done, Right = Skip with custom background
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.adapterPosition
                val habit = habits[pos]

                if (direction == ItemTouchHelper.LEFT) {
                    //  Swipe left → Done
                    PrefsManager.saveHabitStatus(requireContext(), date, habit.name, "done")
                    adapter.markDone(pos)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    //  Swipe right → Skip
                    PrefsManager.saveHabitStatus(requireContext(), date, habit.name, "skipped")
                    adapter.markSkipped(pos)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)

                val itemView = vh.itemView
                val paint = Paint()

                if (dX > 0) { // Swipe Right → Skip
                    paint.color = Color.DKGRAY
                    c.drawRect(
                        itemView.left.toFloat(), itemView.top.toFloat(),
                        itemView.left + dX, itemView.bottom.toFloat(), paint
                    )
                    paint.color = Color.WHITE
                    paint.textSize = 40f
                    c.drawText("Skip", itemView.left + 50f, itemView.top + 60f, paint)
                } else if (dX < 0) { // Swipe Left → Done
                    paint.color = Color.parseColor("#388E3C") // dark green
                    c.drawRect(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                    )
                    paint.color = Color.WHITE
                    paint.textSize = 40f
                    c.drawText("Done", itemView.right - 150f, itemView.top + 60f, paint)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerActivities)

        updateWaterUI(date)
    }

    private fun updateWaterUI(date: String) {
        val goal = PrefsManager.getWaterGoal(requireContext())
        val intake = PrefsManager.getWaterIntake(requireContext(), date)
        val percent = if (goal > 0) (intake * 100) / goal else 0

        progressWater.progress = percent.coerceAtMost(100)
        tvWaterProgress.text = "$intake / $goal ml"
    }
}
