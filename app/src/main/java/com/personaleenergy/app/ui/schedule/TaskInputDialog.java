package com.personaleenergy.app.ui.schedule;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.flowstate.app.R;
import com.flowstate.app.data.models.EnergyLevel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for adding multiple tasks with energy intensity for AI scheduling
 */
public class TaskInputDialog extends DialogFragment {
    
    public interface TaskInputCallback {
        void onTasksAdded(List<TaskWithEnergy> tasks);
        void onCancel();
    }
    
    private TaskInputCallback callback;
    private RecyclerView recyclerView;
    private TaskItemAdapter adapter;
    private List<TaskWithEnergy> tasks;
    
    public static TaskInputDialog newInstance(TaskInputCallback callback) {
        TaskInputDialog dialog = new TaskInputDialog();
        dialog.callback = callback;
        return dialog;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("Add Tasks with Energy Levels");
        return dialog;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_task_input, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerViewTasks);
        MaterialButton btnAddTask = view.findViewById(R.id.btnAddTask);
        MaterialButton btnCancel = view.findViewById(R.id.btnCancel);
        MaterialButton btnGenerate = view.findViewById(R.id.btnGenerate);
        
        // Initialize tasks list with one empty task
        tasks = new ArrayList<>();
        tasks.add(new TaskWithEnergy("", EnergyLevel.MEDIUM));
        
        adapter = new TaskItemAdapter(tasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        // Set up Add Task button click listener
        if (btnAddTask != null) {
            btnAddTask.setOnClickListener(v -> {
                android.util.Log.d("TaskInputDialog", "Add Task button clicked, adding new task. Current count: " + tasks.size());
                try {
                    adapter.addTask();
                    android.util.Log.d("TaskInputDialog", "Task added. New count: " + tasks.size());
                    // Scroll to the new item
                    recyclerView.post(() -> {
                        if (adapter.getItemCount() > 0) {
                            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("TaskInputDialog", "Error adding task", e);
                }
            });
            android.util.Log.d("TaskInputDialog", "Add Task button initialized");
        } else {
            android.util.Log.e("TaskInputDialog", "Add Task button not found in layout!");
        }
        
        btnCancel.setOnClickListener(v -> {
            if (callback != null) {
                callback.onCancel();
            }
            dismiss();
        });
        
        btnGenerate.setOnClickListener(v -> {
            // Validate tasks
            List<TaskWithEnergy> validTasks = new ArrayList<>();
            for (TaskWithEnergy task : tasks) {
                if (!TextUtils.isEmpty(task.getTaskName().trim())) {
                    validTasks.add(task);
                    android.util.Log.d("TaskInputDialog", "Task: " + task.getTaskName() + ", Energy: " + task.getEnergyLevel());
                }
            }
            
            if (validTasks.isEmpty()) {
                Toast.makeText(getContext(), "Please enter at least one task", Toast.LENGTH_SHORT).show();
                return;
            }
            
            android.util.Log.d("TaskInputDialog", "Submitting " + validTasks.size() + " tasks with energy levels");
            if (callback != null) {
                callback.onTasksAdded(validTasks);
            }
            dismiss();
        });
        
        return view;
    }
}

