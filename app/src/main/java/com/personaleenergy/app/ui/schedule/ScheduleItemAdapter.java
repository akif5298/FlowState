package com.personaleenergy.app.ui.schedule;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.flowstate.app.R;
import com.flowstate.app.ai.SmartCalendarAI;
import com.flowstate.app.data.models.EnergyLevel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying schedule items in a RecyclerView
 */
public class ScheduleItemAdapter extends RecyclerView.Adapter<ScheduleItemAdapter.ScheduleItemViewHolder> {

    private List<SmartCalendarAI.ScheduledItem> scheduleItems;
    private SimpleDateFormat timeFormat;
    private OnTaskDeleteListener deleteListener;

    public interface OnTaskDeleteListener {
        void onTaskDelete(SmartCalendarAI.ScheduledItem item, int position);
    }

    public ScheduleItemAdapter(List<SmartCalendarAI.ScheduledItem> scheduleItems) {
        this.scheduleItems = scheduleItems;
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    public void setOnTaskDeleteListener(OnTaskDeleteListener listener) {
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ScheduleItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_schedule_task, parent, false);
        return new ScheduleItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleItemViewHolder holder, int position) {
        SmartCalendarAI.ScheduledItem item = scheduleItems.get(position);
        holder.bind(item, timeFormat, deleteListener, position);
    }

    @Override
    public int getItemCount() {
        return scheduleItems != null ? scheduleItems.size() : 0;
    }

    public void updateScheduleItems(List<SmartCalendarAI.ScheduledItem> newItems) {
        this.scheduleItems = newItems;
        notifyDataSetChanged();
    }

    static class ScheduleItemViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTime;
        private TextView tvTaskTitle;
        private TextView ivEnergyIcon;
        private Chip chipEnergyLevel;
        private TextView tvType;
        private TextView tvReasoning;
        private MaterialButton btnDeleteTask;

        ScheduleItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            ivEnergyIcon = itemView.findViewById(R.id.ivEnergyIcon);
            chipEnergyLevel = itemView.findViewById(R.id.chipEnergyLevel);
            tvType = itemView.findViewById(R.id.tvType);
            tvReasoning = itemView.findViewById(R.id.tvReasoning);
            btnDeleteTask = itemView.findViewById(R.id.btnDeleteTask);
        }

        void bind(SmartCalendarAI.ScheduledItem item, SimpleDateFormat timeFormat, 
                  OnTaskDeleteListener deleteListener, int position) {
            // Set time
            java.util.Date startDate = new java.util.Date(item.startTime);
            java.util.Date endDate = new java.util.Date(item.endTime);
            tvTime.setText(timeFormat.format(startDate) + " - " + timeFormat.format(endDate));

            // Set task title
            tvTaskTitle.setText(item.title);

            // Set energy level and icon
            if (item.energyLevel != null && item.type == SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK) {
                chipEnergyLevel.setVisibility(View.VISIBLE);
                ivEnergyIcon.setVisibility(View.VISIBLE);
                
                String energyText = "";
                int energyColorRes = R.color.energy_medium;
                String energyEmoji = "⚡";
                
                switch (item.energyLevel) {
                    case HIGH:
                        energyText = "High energy";
                        energyColorRes = R.color.energy_high;
                        energyEmoji = "🔥";
                        break;
                    case MEDIUM:
                        energyText = "Medium energy";
                        energyColorRes = R.color.energy_medium;
                        energyEmoji = "⚡";
                        break;
                    case LOW:
                        energyText = "Low energy";
                        energyColorRes = R.color.energy_low;
                        energyEmoji = "🔋";
                        break;
                }
                
                chipEnergyLevel.setText(energyText);
                chipEnergyLevel.setChipBackgroundColorResource(energyColorRes);
                ivEnergyIcon.setText(energyEmoji);
                ivEnergyIcon.setVisibility(View.VISIBLE);
            } else {
                chipEnergyLevel.setVisibility(View.GONE);
                ivEnergyIcon.setVisibility(View.GONE);
            }

            // Set type indicator
            if (item.type == SmartCalendarAI.ScheduledItemType.EXISTING_EVENT) {
                tvType.setText("📅 Existing Event");
                tvType.setVisibility(View.VISIBLE);
            } else if (item.type == SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK) {
                tvType.setText("✨ AI Scheduled");
                tvType.setVisibility(View.VISIBLE);
            } else {
                tvType.setVisibility(View.GONE);
            }

            // Set reasoning
            if (item.reasoning != null && !item.reasoning.trim().isEmpty()) {
                tvReasoning.setText("💡 " + item.reasoning);
                tvReasoning.setVisibility(View.VISIBLE);
            } else {
                tvReasoning.setVisibility(View.GONE);
            }

            // Show delete button only for AI-scheduled tasks (not existing events)
            if (item.type == SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK) {
                btnDeleteTask.setVisibility(View.VISIBLE);
                btnDeleteTask.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onTaskDelete(item, position);
                    }
                });
            } else {
                btnDeleteTask.setVisibility(View.GONE);
            }
        }
    }
}

