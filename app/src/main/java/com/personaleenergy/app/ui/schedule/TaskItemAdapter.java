package com.personaleenergy.app.ui.schedule;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.flowstate.app.R;
import com.flowstate.app.data.models.EnergyLevel;
import java.util.List;

/**
 * Adapter for task items with energy level selection
 */
public class TaskItemAdapter extends RecyclerView.Adapter<TaskItemAdapter.TaskViewHolder> {
    
    private List<TaskWithEnergy> tasks;
    
    public TaskItemAdapter(List<TaskWithEnergy> tasks) {
        this.tasks = tasks;
    }
    
    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_task_with_energy, parent, false);
        return new TaskViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskWithEnergy task = tasks.get(position);
        holder.bind(task);
    }
    
    @Override
    public int getItemCount() {
        return tasks.size();
    }
    
    public void addTask() {
        tasks.add(new TaskWithEnergy("", EnergyLevel.MEDIUM));
        notifyItemInserted(tasks.size() - 1);
    }
    
    public void removeTask(int position) {
        if (position >= 0 && position < tasks.size()) {
            tasks.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, tasks.size());
        }
    }
    
        public List<TaskWithEnergy> getTasks() {
            return tasks;
        }
        
        /**
         * Helper method to hide the keyboard
         */
        private void hideKeyboard(View view) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    
    class TaskViewHolder extends RecyclerView.ViewHolder {
        private EditText etTaskName;
        private Spinner spinnerEnergyLevel;
        private View btnRemove;
        private TextWatcher textWatcher;
        private ArrayAdapter<String> energyAdapter;
        
        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            etTaskName = itemView.findViewById(R.id.etTaskName);
            spinnerEnergyLevel = itemView.findViewById(R.id.spinnerEnergyLevel);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            
            // Setup spinner adapter
            energyAdapter = new ArrayAdapter<>(itemView.getContext(), 
                android.R.layout.simple_spinner_item,
                new String[]{"Low 🔋", "Medium ⚡", "High 🔥"});
            energyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerEnergyLevel.setAdapter(energyAdapter);
        }
        
        void bind(TaskWithEnergy task) {
            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            
            // Remove old text watcher if exists
            if (textWatcher != null) {
                etTaskName.removeTextChangedListener(textWatcher);
            }
            
            // Set task name
            etTaskName.setText(task.getTaskName());
            textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                
                @Override
                public void afterTextChanged(Editable s) {
                    int currentPosition = getAdapterPosition();
                    if (currentPosition >= 0 && currentPosition < tasks.size()) {
                        tasks.get(currentPosition).setTaskName(s.toString().trim());
                    }
                }
            };
            etTaskName.addTextChangedListener(textWatcher);
            
            // Set energy level in spinner
            int spinnerPosition = 1; // Default to Medium
            switch (task.getEnergyLevel()) {
                case LOW:
                    spinnerPosition = 0;
                    break;
                case MEDIUM:
                    spinnerPosition = 1;
                    break;
                case HIGH:
                    spinnerPosition = 2;
                    break;
            }
            spinnerEnergyLevel.setSelection(spinnerPosition, false);
            
            // Hide keyboard when spinner is clicked
            spinnerEnergyLevel.setOnTouchListener((v, event) -> {
                hideKeyboard(itemView);
                return false; // Let the spinner handle the touch event
            });
            
            // Set up spinner listener
            spinnerEnergyLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    int currentPosition = getAdapterPosition();
                    if (currentPosition >= 0 && currentPosition < tasks.size()) {
                        EnergyLevel selectedLevel;
                        switch (position) {
                            case 0:
                                selectedLevel = EnergyLevel.LOW;
                                break;
                            case 1:
                                selectedLevel = EnergyLevel.MEDIUM;
                                break;
                            case 2:
                                selectedLevel = EnergyLevel.HIGH;
                                break;
                            default:
                                selectedLevel = EnergyLevel.MEDIUM;
                        }
                        tasks.get(currentPosition).setEnergyLevel(selectedLevel);
                        android.util.Log.d("TaskItemAdapter", "Setting energy to " + selectedLevel + " for position " + currentPosition);
                    }
                    // Hide keyboard after selection
                    hideKeyboard(itemView);
                }
                
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });
            
            // Remove button
            btnRemove.setOnClickListener(v -> {
                int currentPosition = getAdapterPosition();
                if (currentPosition >= 0 && currentPosition < tasks.size()) {
                    removeTask(currentPosition);
                }
            });
        }
    }
}

