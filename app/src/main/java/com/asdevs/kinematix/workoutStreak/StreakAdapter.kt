package com.asdevs.kinematix.workoutStreak

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.asdevs.kinematix.R
import com.asdevs.kinematix.models.StreakItem

class StreakAdapter : RecyclerView.Adapter<StreakAdapter.StreakViewHolder>() {
    private var streakList = listOf<StreakItem>()

    class StreakViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.tv_date)
        val dayText: TextView = view.findViewById(R.id.tv_day)
        val container: View = view.findViewById(R.id.streak_item_container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreakViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.streak_item, parent, false)
        return StreakViewHolder(view)
    }

    override fun onBindViewHolder(holder: StreakViewHolder, position: Int) {
        val item = streakList[position]
        val context = holder.itemView.context

        holder.dateText.apply {
            text = item.date
            visibility = View.VISIBLE
        }

        holder.dayText.apply {
            text = item.day
            visibility = View.VISIBLE
        }

        // Set basic text
        holder.dateText.text = item.date
        holder.dayText.text = item.day

        // Handle workout day status and completion
        when {
            // Completed workout day
            item.isWorkoutDay && item.isCompleted -> {
                holder.dateText.setBackgroundResource(R.drawable.streak_circle_completed)
                holder.dateText.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            // Incomplete workout day
            item.isWorkoutDay -> {
                holder.dateText.setBackgroundResource(R.drawable.streak_circle)
                holder.dateText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
            // Non-workout day
            else -> {
                holder.dateText.setBackgroundResource(android.R.color.transparent)
                holder.dateText.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            }
        }

        // Handle today's highlight
        if (item.isToday) {
            holder.container.setBackgroundResource(R.drawable.streak_today_background)
            holder.dayText.setTextColor(ContextCompat.getColor(context, R.color.primary))
        } else {
            holder.container.setBackgroundResource(android.R.color.transparent)
            holder.dayText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        // Optional: Add completion animation
        if (item.isWorkoutDay && item.isCompleted) {
            holder.dateText.alpha = 1f
        } else {
            holder.dateText.alpha = if (item.isWorkoutDay) 0.8f else 0.6f
        }
    }

    override fun getItemCount(): Int = streakList.size

    fun updateStreak(newList: List<StreakItem>) {
        streakList = newList
        notifyDataSetChanged()
    }
}