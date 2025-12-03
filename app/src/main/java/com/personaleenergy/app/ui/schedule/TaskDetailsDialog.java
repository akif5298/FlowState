package com.personaleenergy.app.ui.schedule;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.flowstate.app.R;
import com.flowstate.app.data.models.EnergyLevel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Dialog for adding task details (title, description, time, energy level)
 */
public class TaskDetailsDialog extends DialogFragment {
    
    public interface TaskDetailsCallback {
        void onTaskAdded(String title, String description, long startTime, long endTime, EnergyLevel energyLevel);
    }
    
    private TaskDetailsCallback callback;
    private TextInputEditText etTaskTitle, etTaskDescription, etStartTime, etEndTime, etDuration;
    private ChipGroup chipGroupEnergy;
    private Chip chipEnergyLow, chipEnergyMedium, chipEnergyHigh;
    private Calendar startCalendar, endCalendar;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateTimeFormat;
    
    public static TaskDetailsDialog newInstance(TaskDetailsCallback callback) {
        TaskDetailsDialog dialog = new TaskDetailsDialog();
        dialog.callback = callback;
        return dialog;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_FlowState);
        
        startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.HOUR_OF_DAY, 1); // Default: 1 hour from now
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        
        endCalendar = (Calendar) startCalendar.clone();
        endCalendar.add(Calendar.HOUR_OF_DAY, 1); // Default: 1 hour duration
        
        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        dateTimeFormat = new SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault());
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_task, container, false);
        
        etTaskTitle = view.findViewById(R.id.etTaskTitle);
        etTaskDescription = view.findViewById(R.id.etTaskDescription);
        etStartTime = view.findViewById(R.id.etStartTime);
        etEndTime = view.findViewById(R.id.etEndTime);
        etDuration = view.findViewById(R.id.etDuration);
        chipGroupEnergy = view.findViewById(R.id.chipGroupEnergy);
        chipEnergyLow = view.findViewById(R.id.chipEnergyLow);
        chipEnergyMedium = view.findViewById(R.id.chipEnergyMedium);
        chipEnergyHigh = view.findViewById(R.id.chipEnergyHigh);
        
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnAddTask = view.findViewById(R.id.btnAddTask);
        
        // Set default times
        updateTimeDisplay();
        
        // Start time picker
        etStartTime.setOnClickListener(v -> showDateTimePicker(true));
        
        // End time picker
        etEndTime.setOnClickListener(v -> showDateTimePicker(false));
        
        // Duration change listener - update end time
        etDuration.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                updateEndTimeFromDuration();
            }
        });
        
        // Cancel button
        btnCancel.setOnClickListener(v -> dismiss());
        
        // Add task button
        btnAddTask.setOnClickListener(v -> {
            if (validateAndAddTask()) {
                dismiss();
            }
        });
        
        return view;
    }
    
    private void updateTimeDisplay() {
        etStartTime.setText(dateTimeFormat.format(startCalendar.getTime()));
        etEndTime.setText(dateTimeFormat.format(endCalendar.getTime()));
    }
    
    private void updateEndTimeFromDuration() {
        try {
            String durationText = etDuration.getText().toString().trim();
            if (!TextUtils.isEmpty(durationText)) {
                double durationHours = Double.parseDouble(durationText);
                endCalendar = (Calendar) startCalendar.clone();
                endCalendar.add(Calendar.MINUTE, (int) (durationHours * 60));
                updateTimeDisplay();
            }
        } catch (NumberFormatException e) {
            // Invalid duration, ignore
        }
    }
    
    private void showDateTimePicker(boolean isStartTime) {
        Calendar calendar = isStartTime ? startCalendar : endCalendar;
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            requireContext(),
            (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                
                // After date is selected, show time picker
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view1, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        calendar.set(Calendar.SECOND, 0);
                        
                        if (isStartTime) {
                            // If start time changed, update end time if needed
                            if (endCalendar.before(startCalendar) || endCalendar.equals(startCalendar)) {
                                endCalendar = (Calendar) startCalendar.clone();
                                endCalendar.add(Calendar.HOUR_OF_DAY, 1);
                            }
                        } else {
                            // If end time changed, update duration
                            long diffMillis = endCalendar.getTimeInMillis() - startCalendar.getTimeInMillis();
                            double durationHours = diffMillis / (1000.0 * 60.0 * 60.0);
                            etDuration.setText(String.format(Locale.getDefault(), "%.1f", durationHours));
                        }
                        
                        updateTimeDisplay();
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                );
                timePickerDialog.show();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }
    
    private boolean validateAndAddTask() {
        String title = etTaskTitle.getText().toString().trim();
        
        if (TextUtils.isEmpty(title)) {
            etTaskTitle.setError("Task title is required");
            etTaskTitle.requestFocus();
            return false;
        }
        
        // Validate times
        if (endCalendar.before(startCalendar) || endCalendar.equals(startCalendar)) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        // Get energy level
        EnergyLevel energyLevel = EnergyLevel.MEDIUM; // Default
        int checkedId = chipGroupEnergy.getCheckedChipId();
        if (checkedId == R.id.chipEnergyLow) {
            energyLevel = EnergyLevel.LOW;
        } else if (checkedId == R.id.chipEnergyMedium) {
            energyLevel = EnergyLevel.MEDIUM;
        } else if (checkedId == R.id.chipEnergyHigh) {
            energyLevel = EnergyLevel.HIGH;
        }
        
        // Get description
        String description = etTaskDescription.getText().toString().trim();
        
        // Callback with task details
        if (callback != null) {
            callback.onTaskAdded(
                title,
                description,
                startCalendar.getTimeInMillis(),
                endCalendar.getTimeInMillis(),
                energyLevel
            );
        }
        
        return true;
    }
}

