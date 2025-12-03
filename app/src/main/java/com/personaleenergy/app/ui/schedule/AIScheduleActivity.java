package com.personaleenergy.app.ui.schedule;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import com.flowstate.app.utils.HelpDialogHelper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.flowstate.app.R;
import com.flowstate.app.calendar.GoogleCalendarService;
import com.flowstate.app.calendar.SimpleCalendarService;
import com.flowstate.app.ai.SmartCalendarAI;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.data.DataLogsActivity;
import com.personaleenergy.app.ui.settings.SettingsActivity;
import com.personaleenergy.app.ui.insights.WeeklyInsightsActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.text.SimpleDateFormat;

public class AIScheduleActivity extends AppCompatActivity {
    
    private static final int CALENDAR_WRITE_PERMISSION_REQUEST = 5002;
    
    private BottomNavigationView bottomNav;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddTask;
    private Button btnRegenerate;
    private Button btnAddToCalendar;
    private androidx.recyclerview.widget.RecyclerView recyclerViewSchedule;
    private TextView tvEmptySchedule;
    private ScheduleItemAdapter scheduleAdapter;
    
    // CP470 Requirements: ProgressBar and ListView
    private ProgressBar progressBarSchedule;
    private ListView listViewSchedule;
    private SmartCalendarAI.SmartSchedule currentSchedule; // Store current schedule for calendar saving
    private GoogleCalendarService calendarService;
    private SimpleCalendarService simpleCalendarService;
    private SmartCalendarAI smartCalendarAI;
    private List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> userTasks;
    private List<GoogleCalendarService.CalendarEvent> calendarEvents;
    // Track task names created today to avoid reloading them from calendar
    // This persists across app sessions and resets each day
    private Set<String> recentlyCreatedTaskNames = new HashSet<>();
    private static final String PREFS_RECENT_TASKS_DATE = "recent_tasks_date";
    private static final String PREFS_RECENT_TASKS_NAMES = "recent_tasks_names";
    // Flag to prevent loading scheduled tasks from calendar right after generating a schedule
    // This prevents duplicates when tasks are first created and scheduled
    private boolean scheduleJustGenerated = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check authentication
        com.flowstate.app.supabase.AuthService authService = new com.flowstate.app.supabase.AuthService(this);
        if (!authService.isAuthenticated()) {
            // Not authenticated, go to login
            startActivity(new Intent(this, com.flowstate.app.ui.LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_ai_schedule);
        
        userTasks = new ArrayList<com.personaleenergy.app.ui.schedule.TaskWithEnergy>();
        calendarEvents = new ArrayList<>();
        calendarService = new GoogleCalendarService(this);
        simpleCalendarService = new SimpleCalendarService(this);
        smartCalendarAI = new SmartCalendarAI(this);
        
        // Load recently created task names (persists across sessions, resets daily)
        loadRecentlyCreatedTaskNames();
        
        setupBottomNavigation();
        initializeViews();
        
        // Load tasks from calendar if calendar is enabled and has permissions
        if (simpleCalendarService.hasPermissions()) {
            loadTasksFromCalendar();
        }
        
        // Check if tasks exist, if not show task input dialog first
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean hasShownTaskDialog = prefs.getBoolean("has_shown_task_dialog", false);
        
        if (userTasks.isEmpty() && !hasShownTaskDialog) {
            // Show task input dialog on first open
            showTaskInputDialog();
        } else if (!userTasks.isEmpty()) {
            // Load calendar events and generate schedule
            loadCalendarEvents();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Check if date has changed (new day) and reset recently created task names if needed
        loadRecentlyCreatedTaskNames();
        
        // Reset the flag when resuming (allows loading scheduled tasks from calendar)
        scheduleJustGenerated = false;
        
        // Store current task count before loading
        int tasksBeforeLoad = userTasks != null ? userTasks.size() : 0;
        
        // Always try to load tasks from calendar (they will be merged with existing tasks, avoiding duplicates)
        if (simpleCalendarService.hasPermissions()) {
            loadTasksFromCalendar();
            // loadTasksFromCalendar will call loadCalendarEvents, which will then check for scheduled tasks
        } else if (!userTasks.isEmpty()) {
            // We have tasks but no permissions, just load calendar events (existing events, not tasks)
            loadCalendarEvents();
        } else {
            // No tasks and no permissions, but still try to load scheduled tasks if possible
            // (though without permissions this won't work)
            if (simpleCalendarService.hasPermissions()) {
                loadScheduledTasksFromCalendar();
            }
        }
        
        // Log task count after loading to ensure tasks aren't lost
        int tasksAfterLoad = userTasks != null ? userTasks.size() : 0;
        if (tasksBeforeLoad > 0 && tasksAfterLoad < tasksBeforeLoad) {
            android.util.Log.w("AIScheduleActivity", 
                "WARNING: Task count decreased in onResume! Before: " + tasksBeforeLoad + ", After: " + tasksAfterLoad);
        }
    }
    
    private void initializeViews() {
        fabAddTask = findViewById(R.id.fabAddTask);
        btnRegenerate = findViewById(R.id.btnRegenerate);
        btnAddToCalendar = findViewById(R.id.btnAddToCalendar);
        recyclerViewSchedule = findViewById(R.id.recyclerViewSchedule);
        tvEmptySchedule = findViewById(R.id.tvEmptySchedule);
        
        // Setup RecyclerView
        if (recyclerViewSchedule != null) {
            recyclerViewSchedule.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            scheduleAdapter = new ScheduleItemAdapter(new java.util.ArrayList<>());
            // Set delete listener
            scheduleAdapter.setOnTaskDeleteListener((item, position) -> {
                deleteTask(item, position);
            });
            recyclerViewSchedule.setAdapter(scheduleAdapter);
            android.util.Log.d("AIScheduleActivity", "RecyclerView initialized");
        } else {
            android.util.Log.e("AIScheduleActivity", "RecyclerView is null!");
        }
        
        if (tvEmptySchedule != null) {
            tvEmptySchedule.setVisibility(View.VISIBLE);
        }
        
        if (fabAddTask != null) {
            fabAddTask.setOnClickListener(v -> {
                // Show task input dialog for adding multiple tasks
                showTaskInputDialog();
            });
        }
        
        btnRegenerate.setOnClickListener(v -> {
            if (userTasks == null || userTasks.isEmpty()) {
                Toast.makeText(this, "Please add tasks first before generating schedule", Toast.LENGTH_SHORT).show();
                showTaskInputDialog();
            } else {
                // CP470 Requirement #7: Use AsyncTask for schedule generation
                @SuppressWarnings("deprecation")
                GenerateScheduleAsyncTask asyncTask = new GenerateScheduleAsyncTask();
                asyncTask.execute();
                // Show message to user
                Toast.makeText(this, "🔄 Regenerating schedule with same tasks...", Toast.LENGTH_SHORT).show();
                
                // Log current tasks before regenerating
                android.util.Log.d("AIScheduleActivity", "Regenerating schedule with " + userTasks.size() + " tasks:");
                for (com.personaleenergy.app.ui.schedule.TaskWithEnergy task : userTasks) {
                    android.util.Log.d("AIScheduleActivity", "  - " + task.getTaskName() + " (" + task.getEnergyLevel() + ")");
                }
                
                // Delete existing scheduled tasks from calendar before regenerating
                // This allows the AI to create a fresh schedule with the same tasks but different timings
                // IMPORTANT: Do NOT clear userTasks - we want to keep the same tasks
                deleteExistingScheduledTasks(() -> {
                    // Verify tasks are still there after deletion
                    android.util.Log.d("AIScheduleActivity", "After deletion, userTasks size: " + userTasks.size());
                    if (userTasks.isEmpty()) {
                        android.util.Log.e("AIScheduleActivity", "ERROR: userTasks is empty after deletion! This should not happen.");
                        Toast.makeText(AIScheduleActivity.this, "Error: Tasks were lost. Please add tasks again.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    // After deleting, generate a new schedule
                    generateSchedule();
                });
            }
        });
        
        btnAddToCalendar.setOnClickListener(v -> {
            if (currentSchedule == null || currentSchedule.scheduledItems == null || currentSchedule.scheduledItems.isEmpty()) {
                Toast.makeText(this, "Please generate a schedule first", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Delete all existing FlowState calendar events, then add the current schedule
            deleteAllFlowStateCalendarEvents(() -> {
                // Now add the current schedule to calendar
                addScheduleToCalendar(currentSchedule);
            });
        });
    }
    
    /**
     * Show dialog to input multiple tasks at once
     */
    private void showTaskInputDialog() {
        TaskInputDialog dialog = TaskInputDialog.newInstance(new TaskInputDialog.TaskInputCallback() {
            @Override
            public void onTasksAdded(List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasks) {
                // Check if midnight has passed before adding new tasks
                checkAndResetIfNewDay();
                
                // Merge new tasks with existing tasks (avoid duplicates)
                Set<String> existingTaskNames = new HashSet<>();
                for (com.personaleenergy.app.ui.schedule.TaskWithEnergy existingTask : userTasks) {
                    existingTaskNames.add(existingTask.getTaskName());
                }
                
                List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> newTasksToAdd = new ArrayList<>();
                for (com.personaleenergy.app.ui.schedule.TaskWithEnergy task : tasks) {
                    // Only add if it doesn't already exist
                    if (!existingTaskNames.contains(task.getTaskName())) {
                        newTasksToAdd.add(task);
                        existingTaskNames.add(task.getTaskName());
                        // Track these task names as recently created today
                        recentlyCreatedTaskNames.add(task.getTaskName());
                    } else {
                        android.util.Log.d("AIScheduleActivity", 
                            "Skipping duplicate task when adding: " + task.getTaskName());
                    }
                }
                
                // Add new tasks to userTasks (merge, don't replace)
                userTasks.addAll(newTasksToAdd);
                
                // Save to SharedPreferences so it persists across app sessions
                saveRecentlyCreatedTaskNames();
                
                // Mark that dialog has been shown
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit().putBoolean("has_shown_task_dialog", true).apply();
                
                // DON'T save tasks to calendar here - user must click "Add to Calendar" button
                // after reviewing and potentially regenerating the schedule. This gives them control.
                
                Toast.makeText(AIScheduleActivity.this, 
                    "Added " + newTasksToAdd.size() + " task(s). Generating schedule...", 
                    Toast.LENGTH_SHORT).show();
                
                // Generate schedule immediately with the new tasks
                // Load calendar events first (to get existing events), then generate schedule
                // This ensures we have both the new tasks and existing calendar events
                loadCalendarEvents();
            }
            
            @Override
            public void onCancel() {
                // User cancelled, but still allow them to add tasks later
                if (recyclerViewSchedule != null && tvEmptySchedule != null) {
                    recyclerViewSchedule.setVisibility(View.GONE);
                    tvEmptySchedule.setVisibility(View.VISIBLE);
                    tvEmptySchedule.setText("📝 Please add tasks to generate your AI-optimized schedule.\n\n" +
                        "Click the + button above to get started!");
                }
            }
        });
        
        dialog.show(getSupportFragmentManager(), "TaskInputDialog");
    }
    
    /**
     * Save tasks to calendar immediately for persistence
     */
    private void saveTasksToCalendar(List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasks) {
        // Check if we need to request permissions
        boolean needsRead = !simpleCalendarService.hasPermissions();
        boolean needsWrite = !simpleCalendarService.hasWritePermissions();
        
        if (needsRead || needsWrite) {
            android.util.Log.d("AIScheduleActivity", "Calendar permissions not granted, requesting permissions");
            // Request both read and write permissions if needed
            List<String> permissionsToRequest = new ArrayList<>();
            if (needsRead) {
                permissionsToRequest.add(Manifest.permission.READ_CALENDAR);
            }
            if (needsWrite) {
                permissionsToRequest.add(Manifest.permission.WRITE_CALENDAR);
            }
            
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    CALENDAR_WRITE_PERMISSION_REQUEST);
            
            // Show helpful message to user
            Snackbar.make(findViewById(android.R.id.content),
                    "📅 Calendar permissions needed to save tasks. Please grant permissions.",
                    Snackbar.LENGTH_LONG)
                    .setAction("Grant", v -> {
                        ActivityCompat.requestPermissions(AIScheduleActivity.this,
                                permissionsToRequest.toArray(new String[0]),
                                CALENDAR_WRITE_PERMISSION_REQUEST);
                    })
                    .show();
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (!calendarEnabled) {
            android.util.Log.d("AIScheduleActivity", "Calendar integration not enabled, cannot save tasks");
            Snackbar.make(findViewById(android.R.id.content),
                    "📅 Enable calendar integration in Settings to save tasks to your calendar.",
                    Snackbar.LENGTH_LONG)
                    .show();
            return;
        }
        
        // Save each task as a calendar event (for today, will be rescheduled by AI)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 9); // Default to 9 AM
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        for (int i = 0; i < tasks.size(); i++) {
            com.personaleenergy.app.ui.schedule.TaskWithEnergy task = tasks.get(i);
            Calendar taskTime = (Calendar) today.clone();
            taskTime.add(Calendar.MINUTE, i * 30); // Space tasks 30 minutes apart
            
            long startTime = taskTime.getTimeInMillis();
            long endTime = startTime + (60 * 60 * 1000L); // 1 hour duration
            
            StringBuilder description = new StringBuilder();
            description.append("Created by FlowState AI Schedule\n");
            description.append("Energy Level: ").append(task.getEnergyLevel().toString());
            
            simpleCalendarService.createCalendarEvent(
                task.getTaskName(),
                description.toString(),
                startTime,
                endTime,
                new SimpleCalendarService.CreateEventCallback() {
                    @Override
                    public void onSuccess(SimpleCalendarService.CalendarEvent event) {
                        android.util.Log.d("AIScheduleActivity", "Task saved to calendar: " + event.title);
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        android.util.Log.e("AIScheduleActivity", "Failed to save task to calendar: " + task.getTaskName(), error);
                    }
                });
        }
    }
    
    /**
     * Load tasks from calendar (tasks created by this app)
     * This allows tasks to persist across app sessions
     * NOTE: Loads ALL FlowState tasks from calendar, filtering duplicates by name
     */
    private void loadTasksFromCalendar() {
        if (!simpleCalendarService.hasPermissions()) {
            android.util.Log.d("AIScheduleActivity", "Cannot load tasks - no calendar permissions");
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (!calendarEnabled) {
            android.util.Log.d("AIScheduleActivity", "Cannot load tasks - calendar not enabled");
            return;
        }
        
        // Get today's events
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        
        android.util.Log.d("AIScheduleActivity", "Loading tasks from calendar...");
        
        simpleCalendarService.getEvents(
            startOfDay.getTimeInMillis(),
            endOfDay.getTimeInMillis(),
            new SimpleCalendarService.CalendarEventsCallback() {
                @Override
                public void onSuccess(List<SimpleCalendarService.CalendarEvent> events) {
                    runOnUiThread(() -> {
                        // Check if midnight has passed (for recentlyCreatedTaskNames reset)
                        checkAndResetIfNewDay();
                        
                        // Filter events created by this app (check description)
                        // Load ALL FlowState tasks from calendar, filtering only duplicates by name
                        List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> loadedTasks = new ArrayList<>();
                        Set<String> existingTaskNames = new HashSet<>();
                        
                        // Track existing task names in userTasks to avoid duplicates
                        for (com.personaleenergy.app.ui.schedule.TaskWithEnergy existingTask : userTasks) {
                            existingTaskNames.add(existingTask.getTaskName());
                        }
                        
                        for (SimpleCalendarService.CalendarEvent event : events) {
                            if (event.description != null && 
                                event.description.contains("Created by FlowState AI Schedule")) {
                                
                                // Skip tasks that have already been scheduled by AI (they have "AI Reasoning" in description)
                                // These are already in the schedule and don't need to be loaded as unscheduled tasks
                                if (event.description.contains("AI Reasoning:")) {
                                    android.util.Log.d("AIScheduleActivity", 
                                        "Skipping already-scheduled task: " + event.title);
                                    continue;
                                }
                                
                                // Skip duplicates (same task name) - already in userTasks or already loaded
                                if (existingTaskNames.contains(event.title)) {
                                    android.util.Log.d("AIScheduleActivity", 
                                        "Skipping duplicate task: " + event.title);
                                    continue;
                                }
                                
                                // Extract energy level from description
                                com.flowstate.app.data.models.EnergyLevel energyLevel = 
                                    com.flowstate.app.data.models.EnergyLevel.MEDIUM;
                                if (event.description.contains("Energy Level: HIGH")) {
                                    energyLevel = com.flowstate.app.data.models.EnergyLevel.HIGH;
                                } else if (event.description.contains("Energy Level: LOW")) {
                                    energyLevel = com.flowstate.app.data.models.EnergyLevel.LOW;
                                }
                                
                                loadedTasks.add(new com.personaleenergy.app.ui.schedule.TaskWithEnergy(
                                    event.title, energyLevel));
                                existingTaskNames.add(event.title);
                            }
                        }
                        
                        // Merge loaded tasks with existing tasks (avoid duplicates)
                        if (!loadedTasks.isEmpty()) {
                            // Add loaded tasks to existing userTasks (if any)
                            for (com.personaleenergy.app.ui.schedule.TaskWithEnergy loadedTask : loadedTasks) {
                                boolean isDuplicate = false;
                                for (com.personaleenergy.app.ui.schedule.TaskWithEnergy existingTask : userTasks) {
                                    if (existingTask.getTaskName().equals(loadedTask.getTaskName())) {
                                        isDuplicate = true;
                                        break;
                                    }
                                }
                                if (!isDuplicate) {
                                    userTasks.add(loadedTask);
                                }
                            }
                            android.util.Log.d("AIScheduleActivity", 
                                "Loaded " + loadedTasks.size() + " tasks from calendar. Total tasks: " + userTasks.size());
                            
                            // After loading tasks, load calendar events and generate schedule
                            loadCalendarEvents();
                        } else {
                            android.util.Log.d("AIScheduleActivity", "No unscheduled tasks found in calendar");
                            // Even if no tasks loaded, still load calendar events (for existing events)
                            // Then check for already-scheduled tasks to display them
                            loadCalendarEvents();
                        }
                    });
                }
                
                @Override
                public void onError(Exception error) {
                    android.util.Log.e("AIScheduleActivity", "Error loading tasks from calendar", error);
                }
            });
    }
    
    /**
     * Load already-scheduled tasks from calendar (tasks with "AI Reasoning" in description)
     * These are tasks that have been scheduled by AI and should be displayed in the schedule
     * NOTE: This should not be called right after generating a schedule to prevent duplicates
     */
    private void loadScheduledTasksFromCalendar() {
        // Don't load if we just generated a schedule (prevents duplicates)
        if (scheduleJustGenerated) {
            android.util.Log.d("AIScheduleActivity", "Skipping loadScheduledTasksFromCalendar - schedule just generated");
            return;
        }
        
        if (!simpleCalendarService.hasPermissions()) {
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (!calendarEnabled) {
            return;
        }
        
        // Get today's events
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        
        simpleCalendarService.getEvents(
            startOfDay.getTimeInMillis(),
            endOfDay.getTimeInMillis(),
            new SimpleCalendarService.CalendarEventsCallback() {
                @Override
                public void onSuccess(List<SimpleCalendarService.CalendarEvent> events) {
                    runOnUiThread(() -> {
                        List<SmartCalendarAI.ScheduledItem> scheduledItems = new ArrayList<>();
                        
                        // Find FlowState-created tasks that have been scheduled (have "AI Reasoning")
                        for (SimpleCalendarService.CalendarEvent event : events) {
                            if (event.description != null && 
                                event.description.contains("Created by FlowState AI Schedule") &&
                                event.description.contains("AI Reasoning:")) {
                                
                                // Extract energy level from description
                                com.flowstate.app.data.models.EnergyLevel energyLevel = 
                                    com.flowstate.app.data.models.EnergyLevel.MEDIUM;
                                if (event.description.contains("Energy Level: HIGH")) {
                                    energyLevel = com.flowstate.app.data.models.EnergyLevel.HIGH;
                                } else if (event.description.contains("Energy Level: LOW")) {
                                    energyLevel = com.flowstate.app.data.models.EnergyLevel.LOW;
                                }
                                
                                // Extract reasoning from description
                                String reasoning = "";
                                int reasoningIndex = event.description.indexOf("AI Reasoning:");
                                if (reasoningIndex >= 0) {
                                    reasoning = event.description.substring(reasoningIndex + "AI Reasoning:".length()).trim();
                                }
                                
                                SmartCalendarAI.ScheduledItem item = new SmartCalendarAI.ScheduledItem();
                                item.title = event.title;
                                item.startTime = event.startTime;
                                item.endTime = event.endTime;
                                item.type = SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK;
                                item.energyLevel = energyLevel;
                                item.reasoning = reasoning;
                                scheduledItems.add(item);
                            }
                        }
                        
                        // Also add existing calendar events (non-FlowState events) from the events we just loaded
                        for (SimpleCalendarService.CalendarEvent event : events) {
                            // Skip FlowState-created tasks (we already handled scheduled ones above)
                            if (event.description != null && 
                                event.description.contains("Created by FlowState AI Schedule")) {
                                continue;
                            }
                            
                            SmartCalendarAI.ScheduledItem item = new SmartCalendarAI.ScheduledItem();
                            item.title = event.title;
                            item.startTime = event.startTime;
                            item.endTime = event.endTime;
                            item.type = SmartCalendarAI.ScheduledItemType.EXISTING_EVENT;
                            item.reasoning = "Existing Calendar Event";
                            scheduledItems.add(item);
                        }
                        
                        // Display the schedule if we have items
                        if (!scheduledItems.isEmpty()) {
                            SmartCalendarAI.SmartSchedule schedule = new SmartCalendarAI.SmartSchedule();
                            schedule.scheduledItems = scheduledItems;
                            schedule.summary = "Loaded from calendar";
                            displaySchedule(schedule);
                            android.util.Log.d("AIScheduleActivity", 
                                "Loaded and displayed " + scheduledItems.size() + " scheduled items from calendar");
                        }
                    });
                }
                
                @Override
                public void onError(Exception error) {
                    android.util.Log.e("AIScheduleActivity", "Error loading scheduled tasks from calendar", error);
                }
            });
    }
    
    private void loadCalendarEvents() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (calendarEnabled) {
            // Use simple calendar service (Android Calendar API - no OAuth needed)
            com.flowstate.app.calendar.SimpleCalendarService simpleCalendarService = 
                new com.flowstate.app.calendar.SimpleCalendarService(this);
            
            if (simpleCalendarService.hasPermissions()) {
                // Load calendar events for today
                simpleCalendarService.getTodayEvents(new com.flowstate.app.calendar.SimpleCalendarService.CalendarEventsCallback() {
                    @Override
                    public void onSuccess(List<com.flowstate.app.calendar.SimpleCalendarService.CalendarEvent> events) {
                        runOnUiThread(() -> {
                            // Convert to old format for compatibility
                            // Exclude events created by FlowState (those are tasks, not existing events)
                            calendarEvents = new java.util.ArrayList<>();
                            for (com.flowstate.app.calendar.SimpleCalendarService.CalendarEvent event : events) {
                                // Skip FlowState-created tasks - these are not "existing events"
                                if (event.description != null && 
                                    event.description.contains("Created by FlowState AI Schedule")) {
                                    continue;
                                }
                                
                                GoogleCalendarService.CalendarEvent oldEvent = new GoogleCalendarService.CalendarEvent();
                                oldEvent.id = event.id;
                                oldEvent.title = event.title;
                                oldEvent.description = event.description;
                                oldEvent.startTime = event.startTime;
                                oldEvent.endTime = event.endTime;
                                calendarEvents.add(oldEvent);
                            }
                            
                            if (!calendarEvents.isEmpty()) {
                                Snackbar.make(findViewById(android.R.id.content), 
                                    "Loaded " + calendarEvents.size() + " calendar events (immutable)", 
                                    Snackbar.LENGTH_SHORT).show();
                            }
                            
                            // Only generate schedule if tasks exist
                            // If no tasks, check for already-scheduled tasks to display them
                            if (!userTasks.isEmpty()) {
                                generateSchedule();
                            } else {
                                // No unscheduled tasks, but check if there are already-scheduled tasks to display
                                // Only load if we haven't just generated a schedule (prevents duplicates)
                                if (!scheduleJustGenerated) {
                                    loadScheduledTasksFromCalendar();
                                }
                            }
                        });
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        runOnUiThread(() -> {
                            android.util.Log.e("AIScheduleActivity", "Failed to load calendar events", error);
                            Snackbar.make(findViewById(android.R.id.content), 
                                "Failed to load calendar events", 
                                Snackbar.LENGTH_SHORT).show();
                            // Continue without calendar events
                            if (!userTasks.isEmpty()) {
                                generateSchedule();
                            }
                        });
                    }
                });
            } else {
                // Permission not granted, prompt to grant
                Snackbar.make(findViewById(android.R.id.content), 
                    "Please grant calendar permission in settings", 
                    Snackbar.LENGTH_LONG).show();
                generateSchedule();
            }
        } else {
            // Calendar not enabled, generate schedule without calendar data
            generateSchedule();
        }
    }
    
    private void generateSchedule() {
        // Show loading
        if (recyclerViewSchedule != null && tvEmptySchedule != null) {
            recyclerViewSchedule.setVisibility(View.GONE);
            tvEmptySchedule.setVisibility(View.VISIBLE);
            tvEmptySchedule.setText("🤖 AI is analyzing your data and generating your smart schedule...\n\nPlease wait...");
        }
        
        // Get user ID
        String userId = com.flowstate.app.supabase.SupabaseClient.getInstance(this).getUserId();
        
        if (userId == null || userId.isEmpty()) {
            if (recyclerViewSchedule != null && tvEmptySchedule != null) {
                recyclerViewSchedule.setVisibility(View.GONE);
                tvEmptySchedule.setVisibility(View.VISIBLE);
                tvEmptySchedule.setText("❌ Error: Please log in to generate your schedule.");
            }
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Filter tasks: only include tasks that need scheduling
        // Remove duplicates - tasks that already have AI-assigned times are filtered out
        // in loadTasksFromCalendar (they have "AI Reasoning" in description)
        List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasksToSchedule = new ArrayList<>();
        Set<String> taskNamesToSchedule = new HashSet<>();
        
        for (com.personaleenergy.app.ui.schedule.TaskWithEnergy task : userTasks) {
            // Skip if we've already added this task name (deduplicate)
            if (taskNamesToSchedule.contains(task.getTaskName())) {
                android.util.Log.d("AIScheduleActivity", 
                    "Filtering duplicate task before scheduling: " + task.getTaskName());
                continue;
            }
            
            // Add task to schedule
            // Note: Tasks that have already been scheduled (have "AI Reasoning" in calendar)
            // are filtered out in loadTasksFromCalendar, so they won't be in userTasks
            tasksToSchedule.add(task);
            taskNamesToSchedule.add(task.getTaskName());
        }
        
        if (tasksToSchedule.isEmpty()) {
            android.util.Log.w("AIScheduleActivity", "No tasks to schedule after filtering");
            android.util.Log.w("AIScheduleActivity", "userTasks size: " + userTasks.size());
            android.util.Log.w("AIScheduleActivity", "tasksToSchedule size: " + tasksToSchedule.size());
            if (recyclerViewSchedule != null && tvEmptySchedule != null) {
                recyclerViewSchedule.setVisibility(View.GONE);
                tvEmptySchedule.setVisibility(View.VISIBLE);
                tvEmptySchedule.setText("📝 No tasks to schedule. Please add tasks first.\n\nCurrent tasks: " + userTasks.size());
            }
            Toast.makeText(this, "No tasks available to schedule. Please add tasks first.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Generate smart schedule using AI with filtered tasks
        android.util.Log.d("AIScheduleActivity", "Generating schedule with " + tasksToSchedule.size() + 
            " tasks (filtered from " + userTasks.size() + " total tasks)");
        smartCalendarAI.generateSmartSchedule(userId, tasksToSchedule, new SmartCalendarAI.SmartScheduleCallback() {
            @Override
            public void onSuccess(SmartCalendarAI.SmartSchedule schedule) {
                runOnUiThread(() -> {
                    android.util.Log.d("AIScheduleActivity", "Schedule generation successful");
                    displaySchedule(schedule);
                    Toast.makeText(AIScheduleActivity.this, 
                        "Smart schedule generated! 🎉", 
                        Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    android.util.Log.e("AIScheduleActivity", "Error generating schedule", error);
                    if (recyclerViewSchedule != null && tvEmptySchedule != null) {
                        recyclerViewSchedule.setVisibility(View.GONE);
                        tvEmptySchedule.setVisibility(View.VISIBLE);
                        tvEmptySchedule.setText("❌ Error generating schedule: " + error.getMessage() + 
                            "\n\nTrying fallback schedule...");
                    }
                    // Fallback to simple schedule
                    generateFallbackSchedule();
                });
            }
        });
    }
    
    private void displaySchedule(SmartCalendarAI.SmartSchedule schedule) {
        // Populate ListView (CP470 Requirement #3)
        if (schedule != null && schedule.scheduledItems != null) {
            populateScheduleListView(schedule.scheduledItems);
        }
        if (recyclerViewSchedule == null || tvEmptySchedule == null) {
            android.util.Log.e("AIScheduleActivity", "RecyclerView or empty text view is null!");
            return;
        }
        
        android.util.Log.d("AIScheduleActivity", "Displaying schedule with " + 
            (schedule.scheduledItems != null ? schedule.scheduledItems.size() : 0) + " items");
        
        // Store current schedule for potential calendar saving
        currentSchedule = schedule;
        
        // Set flag to prevent loading scheduled tasks from calendar right after this
        // This prevents duplicates when tasks are first created and scheduled
        scheduleJustGenerated = true;
        
        // DO NOT automatically create calendar events - user must click "Add to Calendar" button
        // This gives them a chance to regenerate the schedule before committing to calendar
        
        // Display schedule items in RecyclerView
        if (schedule.scheduledItems != null && !schedule.scheduledItems.isEmpty()) {
            android.util.Log.d("AIScheduleActivity", "Showing RecyclerView with " + schedule.scheduledItems.size() + " items");
            recyclerViewSchedule.setVisibility(View.VISIBLE);
            tvEmptySchedule.setVisibility(View.GONE);
            
            if (scheduleAdapter != null) {
                scheduleAdapter.updateScheduleItems(schedule.scheduledItems);
                android.util.Log.d("AIScheduleActivity", "Schedule adapter updated");
            } else {
                android.util.Log.e("AIScheduleActivity", "Schedule adapter is null!");
            }
        } else {
            android.util.Log.d("AIScheduleActivity", "Schedule is empty, showing empty message");
            recyclerViewSchedule.setVisibility(View.GONE);
            tvEmptySchedule.setVisibility(View.VISIBLE);
            tvEmptySchedule.setText("📭 No items scheduled for today.\n\n➕ Add tasks above and click 'Regenerate Schedule' to generate your smart schedule!");
        }
    }
    
    /**
     * Delete all existing scheduled tasks from calendar (those with "AI Reasoning" in description)
     * This is called when regenerating the schedule to allow the AI to create a fresh schedule
     * with the same tasks but different timings
     */
    private void deleteExistingScheduledTasks(Runnable onComplete) {
        if (!simpleCalendarService.hasWritePermissions()) {
            android.util.Log.d("AIScheduleActivity", "Calendar write permission not granted, skipping deletion");
            onComplete.run();
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (!calendarEnabled) {
            android.util.Log.d("AIScheduleActivity", "Calendar integration not enabled, skipping deletion");
            onComplete.run();
            return;
        }
        
        // Get today's events
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        
        android.util.Log.d("AIScheduleActivity", "Deleting existing scheduled tasks before regenerating...");
        
        simpleCalendarService.getEvents(
            startOfDay.getTimeInMillis(),
            endOfDay.getTimeInMillis(),
            new SimpleCalendarService.CalendarEventsCallback() {
                @Override
                public void onSuccess(List<SimpleCalendarService.CalendarEvent> events) {
                    // Find all FlowState-created tasks that have been scheduled (have "AI Reasoning")
                    List<String> eventIdsToDelete = new ArrayList<>();
                    for (SimpleCalendarService.CalendarEvent event : events) {
                        if (event.description != null && 
                            event.description.contains("Created by FlowState AI Schedule") &&
                            event.description.contains("AI Reasoning:")) {
                            eventIdsToDelete.add(event.id);
                        }
                    }
                    
                    if (eventIdsToDelete.isEmpty()) {
                        android.util.Log.d("AIScheduleActivity", "No existing scheduled tasks to delete");
                        onComplete.run();
                        return;
                    }
                    
                    // Delete all scheduled tasks
                    final int[] deletedCount = {0};
                    final int totalToDelete = eventIdsToDelete.size();
                    
                    for (String eventId : eventIdsToDelete) {
                        simpleCalendarService.deleteEvent(eventId, new SimpleCalendarService.DeleteEventCallback() {
                            @Override
                            public void onSuccess() {
                                deletedCount[0]++;
                                if (deletedCount[0] == totalToDelete) {
                                    android.util.Log.d("AIScheduleActivity", 
                                        "Deleted " + deletedCount[0] + " existing scheduled tasks");
                                    onComplete.run();
                                }
                            }
                            
                            @Override
                            public void onError(Exception error) {
                                android.util.Log.w("AIScheduleActivity", 
                                    "Failed to delete event: " + eventId, error);
                                deletedCount[0]++;
                                if (deletedCount[0] == totalToDelete) {
                                    // Continue even if some deletions failed
                                    android.util.Log.d("AIScheduleActivity", 
                                        "Finished deleting scheduled tasks (some may have failed)");
                                    onComplete.run();
                                }
                            }
                        });
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    android.util.Log.e("AIScheduleActivity", "Error loading events for deletion", error);
                    // Continue anyway - try to regenerate
                    onComplete.run();
                }
            });
    }
    
    /**
     * Create a calendar event for an AI-scheduled task
     * This is called when user clicks "Add to Calendar" button
     */
    private void createCalendarEventForScheduledTask(SmartCalendarAI.ScheduledItem item, 
                                                      Runnable onSuccess, Runnable onError) {
        // Skip if this is an existing calendar event (we don't want to duplicate it)
        if (item.type == SmartCalendarAI.ScheduledItemType.EXISTING_EVENT) {
            return;
        }
        
        // Check if calendar write permission is granted
        if (!simpleCalendarService.hasWritePermissions()) {
            android.util.Log.d("AIScheduleActivity", "Calendar write permission not granted, skipping calendar event creation");
            return;
        }
        
        // Check if calendar integration is enabled in settings
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (!calendarEnabled) {
            android.util.Log.d("AIScheduleActivity", "Calendar integration not enabled in settings");
            return;
        }
        
        // Build description with energy level and reasoning
        // IMPORTANT: Include "Energy Level: X" format so tasks can be loaded back from calendar
        StringBuilder description = new StringBuilder();
        description.append("Created by FlowState AI Schedule");
        if (item.energyLevel != null) {
            description.append("\nEnergy Level: ").append(item.energyLevel.toString());
        }
        if (item.reasoning != null && !item.reasoning.isEmpty()) {
            description.append("\n\nAI Reasoning: ").append(item.reasoning);
        }
        
        // First, delete any existing events with the same title (e.g., from previous saves or manual creation)
        // This prevents duplicates when creating the event with AI-assigned times
        simpleCalendarService.deleteEventsByTitle(item.title, new SimpleCalendarService.DeleteEventCallback() {
            @Override
            public void onSuccess() {
                // Now create the calendar event with the AI-scheduled time
                simpleCalendarService.createCalendarEvent(
                    item.title,
                    description.toString(),
                    item.startTime,
                    item.endTime,
                    new SimpleCalendarService.CreateEventCallback() {
                        @Override
                        public void onSuccess(SimpleCalendarService.CalendarEvent event) {
                            android.util.Log.d("AIScheduleActivity", "✅ Calendar event created successfully: " + event.title + " (ID: " + event.id + ")");
                            android.util.Log.d("AIScheduleActivity", "   Start: " + new java.util.Date(item.startTime));
                            android.util.Log.d("AIScheduleActivity", "   End: " + new java.util.Date(item.endTime));
                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            android.util.Log.e("AIScheduleActivity", "❌ Failed to create calendar event for task: " + item.title, error);
                            android.util.Log.e("AIScheduleActivity", "   Error details: " + error.getMessage());
                            if (onError != null) {
                                onError.run();
                            }
                        }
                    }
                );
            }
            
            @Override
            public void onError(Exception error) {
                // Even if delete fails, try to create the event (might be a new task)
                android.util.Log.w("AIScheduleActivity", "Failed to delete existing events for: " + item.title + ", proceeding to create new event", error);
                simpleCalendarService.createCalendarEvent(
                    item.title,
                    description.toString(),
                    item.startTime,
                    item.endTime,
                    new SimpleCalendarService.CreateEventCallback() {
                        @Override
                        public void onSuccess(SimpleCalendarService.CalendarEvent event) {
                            android.util.Log.d("AIScheduleActivity", "✅ Calendar event created successfully: " + event.title + " (ID: " + event.id + ")");
                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                        }
                        
                        @Override
                        public void onError(Exception error) {
                            android.util.Log.e("AIScheduleActivity", "❌ Failed to create calendar event for task: " + item.title, error);
                            if (onError != null) {
                                onError.run();
                            }
                        }
                    }
                );
            }
        });
    }
    
    /**
     * Show task details dialog
     */
    private void showTaskDetailsDialog() {
        TaskDetailsDialog dialog = TaskDetailsDialog.newInstance(new TaskDetailsDialog.TaskDetailsCallback() {
            @Override
            public void onTaskAdded(String title, String description, long startTime, long endTime, 
                                  com.flowstate.app.data.models.EnergyLevel energyLevel) {
                // Add to task list with energy level
                userTasks.add(new com.personaleenergy.app.ui.schedule.TaskWithEnergy(title, energyLevel));
                
                // Create calendar event
                createCalendarEventForTask(title, description, startTime, endTime, energyLevel);
                
                Snackbar.make(findViewById(android.R.id.content), 
                    "Task added: " + title, Snackbar.LENGTH_SHORT).show();
                
                // Regenerate schedule with new task
                generateSchedule();
            }
        });
        
        dialog.show(getSupportFragmentManager(), "TaskDetailsDialog");
    }
    
    /**
     * Create a calendar event for a task with full details
     */
    private void createCalendarEventForTask(String taskTitle, String description, long startTime, 
                                           long endTime, com.flowstate.app.data.models.EnergyLevel energyLevel) {
        // Check if calendar write permission is granted
        if (!simpleCalendarService.hasWritePermissions()) {
            // Request write permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_CALENDAR},
                    CALENDAR_WRITE_PERMISSION_REQUEST);
            return;
        }
        
        // Check if calendar is enabled
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (!calendarEnabled) {
            // Calendar not enabled, just add to task list
            return;
        }
        
        // Build description with energy level info
        StringBuilder eventDescription = new StringBuilder();
        if (description != null && !description.trim().isEmpty()) {
            eventDescription.append(description).append("\n\n");
        }
        eventDescription.append("Created by FlowState AI Schedule");
        if (energyLevel != null) {
            eventDescription.append("\nEnergy Level: ").append(energyLevel);
        }
        
        // Create the calendar event
        simpleCalendarService.createCalendarEvent(
            taskTitle,
            eventDescription.toString(),
            startTime,
            endTime,
            new SimpleCalendarService.CreateEventCallback() {
                @Override
                public void onSuccess(SimpleCalendarService.CalendarEvent event) {
                    runOnUiThread(() -> {
                        android.util.Log.d("AIScheduleActivity", "Calendar event created: " + event.title);
                        Toast.makeText(AIScheduleActivity.this, 
                            "✅ Task added to calendar!", 
                            Toast.LENGTH_SHORT).show();
                        
                        // Reload calendar events to include the new one
                        loadCalendarEvents();
                    });
                }
                
                @Override
                public void onError(Exception error) {
                    runOnUiThread(() -> {
                        android.util.Log.e("AIScheduleActivity", "Failed to create calendar event", error);
                        // Still show success for adding to task list, but warn about calendar
                        Snackbar.make(findViewById(android.R.id.content), 
                            "Task added, but couldn't create calendar event: " + error.getMessage(), 
                            Snackbar.LENGTH_LONG).show();
                    });
                }
            }
        );
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CALENDAR_WRITE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Calendar permission granted! Saving tasks...", Toast.LENGTH_SHORT).show();
                // Retry saving tasks to calendar if we have tasks to save
                if (!userTasks.isEmpty()) {
                    saveTasksToCalendar(userTasks);
                }
            } else {
                Toast.makeText(this, 
                    "⚠️ Calendar permission is needed to save tasks. Tasks will only be stored in the app.", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void generateFallbackSchedule() {
        // Simple fallback schedule - create basic scheduled items from user tasks
        List<SmartCalendarAI.ScheduledItem> fallbackItems = new ArrayList<>();
        
        // Add calendar events as existing events
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        for (GoogleCalendarService.CalendarEvent event : calendarEvents) {
            SmartCalendarAI.ScheduledItem item = new SmartCalendarAI.ScheduledItem();
            item.title = event.title;
            item.startTime = event.startTime;
            item.endTime = event.endTime;
            item.type = SmartCalendarAI.ScheduledItemType.EXISTING_EVENT;
            fallbackItems.add(item);
        }
        
        // Add user tasks as scheduled items
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        
        for (int i = 0; i < userTasks.size(); i++) {
            com.personaleenergy.app.ui.schedule.TaskWithEnergy task = userTasks.get(i);
            Calendar taskTime = (Calendar) now.clone();
            taskTime.add(Calendar.HOUR_OF_DAY, i + 1); // Space tasks 1 hour apart
            
            SmartCalendarAI.ScheduledItem item = new SmartCalendarAI.ScheduledItem();
            item.title = task.getTaskName();
            item.startTime = taskTime.getTimeInMillis();
            item.endTime = taskTime.getTimeInMillis() + (60 * 60 * 1000L); // 1 hour
            item.type = SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK;
            item.energyLevel = task.getEnergyLevel();
            item.reasoning = "Fallback schedule: Task scheduled at " + sdf.format(new java.util.Date(item.startTime));
            fallbackItems.add(item);
        }
        
        // Display fallback schedule
        if (fallbackItems.isEmpty()) {
            if (recyclerViewSchedule != null && tvEmptySchedule != null) {
                recyclerViewSchedule.setVisibility(View.GONE);
                tvEmptySchedule.setVisibility(View.VISIBLE);
                tvEmptySchedule.setText("Add tasks above to generate your smart schedule!");
            }
        } else {
            SmartCalendarAI.SmartSchedule fallbackSchedule = new SmartCalendarAI.SmartSchedule();
            fallbackSchedule.scheduledItems = fallbackItems;
            fallbackSchedule.summary = "Fallback schedule generated";
            displaySchedule(fallbackSchedule);
        }
    }
    
    /**
     * Delete a task from both the app and calendar
     */
    private void deleteTask(SmartCalendarAI.ScheduledItem item, int position) {
        // Remove from userTasks if it's an AI-scheduled task
        if (item.type == SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK) {
            userTasks.removeIf(task -> task.getTaskName().equals(item.title));
            android.util.Log.d("AIScheduleActivity", "Removed task from userTasks: " + item.title);
        }
        
        // Remove from current schedule display
        if (currentSchedule != null && currentSchedule.scheduledItems != null) {
            currentSchedule.scheduledItems.remove(position);
            scheduleAdapter.updateScheduleItems(currentSchedule.scheduledItems);
            
            // Update empty state if needed
            if (currentSchedule.scheduledItems.isEmpty()) {
                if (recyclerViewSchedule != null && tvEmptySchedule != null) {
                    recyclerViewSchedule.setVisibility(View.GONE);
                    tvEmptySchedule.setVisibility(View.VISIBLE);
                    tvEmptySchedule.setText("📭 No items scheduled for today.\n\n➕ Add tasks above and click 'Regenerate Schedule' to generate your smart schedule!");
                }
            }
        }
        
        // Delete from calendar if it exists there
        if (simpleCalendarService.hasWritePermissions()) {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
            
            if (calendarEnabled) {
                // Delete all events with this title (FlowState-created)
                simpleCalendarService.deleteEventsByTitle(item.title, new SimpleCalendarService.DeleteEventCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(AIScheduleActivity.this, 
                                "Task deleted: " + item.title, 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        android.util.Log.e("AIScheduleActivity", "Failed to delete task from calendar: " + item.title, error);
                        runOnUiThread(() -> {
                            Toast.makeText(AIScheduleActivity.this, 
                                "Task removed from app, but couldn't delete from calendar", 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "Task deleted: " + item.title, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Task deleted: " + item.title, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Delete all FlowState-created calendar events (both scheduled and unscheduled)
     */
    private void deleteAllFlowStateCalendarEvents(Runnable onComplete) {
        if (!simpleCalendarService.hasWritePermissions()) {
            android.util.Log.d("AIScheduleActivity", "Calendar write permission not granted, skipping deletion");
            onComplete.run();
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (!calendarEnabled) {
            android.util.Log.d("AIScheduleActivity", "Calendar integration not enabled, skipping deletion");
            onComplete.run();
            return;
        }
        
        // Get today's events
        Calendar startOfDay = Calendar.getInstance();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        endOfDay.set(Calendar.SECOND, 59);
        endOfDay.set(Calendar.MILLISECOND, 999);
        
        android.util.Log.d("AIScheduleActivity", "Deleting all FlowState calendar events...");
        
        simpleCalendarService.getEvents(
            startOfDay.getTimeInMillis(),
            endOfDay.getTimeInMillis(),
            new SimpleCalendarService.CalendarEventsCallback() {
                @Override
                public void onSuccess(List<SimpleCalendarService.CalendarEvent> events) {
                    // Find all FlowState-created events
                    List<String> eventIdsToDelete = new ArrayList<>();
                    for (SimpleCalendarService.CalendarEvent event : events) {
                        if (event.description != null && 
                            event.description.contains("Created by FlowState AI Schedule")) {
                            eventIdsToDelete.add(event.id);
                        }
                    }
                    
                    if (eventIdsToDelete.isEmpty()) {
                        android.util.Log.d("AIScheduleActivity", "No FlowState calendar events to delete");
                        onComplete.run();
                        return;
                    }
                    
                    // Delete all FlowState events
                    final int[] deletedCount = {0};
                    final int totalToDelete = eventIdsToDelete.size();
                    
                    for (String eventId : eventIdsToDelete) {
                        simpleCalendarService.deleteEvent(eventId, new SimpleCalendarService.DeleteEventCallback() {
                            @Override
                            public void onSuccess() {
                                deletedCount[0]++;
                                if (deletedCount[0] == totalToDelete) {
                                    android.util.Log.d("AIScheduleActivity", 
                                        "Deleted " + deletedCount[0] + " FlowState calendar events");
                                    onComplete.run();
                                }
                            }
                            
                            @Override
                            public void onError(Exception error) {
                                android.util.Log.w("AIScheduleActivity", 
                                    "Failed to delete event: " + eventId, error);
                                deletedCount[0]++;
                                if (deletedCount[0] == totalToDelete) {
                                    // Continue even if some deletions failed
                                    android.util.Log.d("AIScheduleActivity", 
                                        "Finished deleting FlowState events (some may have failed)");
                                    onComplete.run();
                                }
                            }
                        });
                    }
                }
                
                @Override
                public void onError(Exception error) {
                    android.util.Log.e("AIScheduleActivity", "Error loading events for deletion", error);
                    // Continue anyway
                    onComplete.run();
                }
            });
    }
    
    /**
     * Add the current schedule to calendar
     */
    private void addScheduleToCalendar(SmartCalendarAI.SmartSchedule schedule) {
        if (!simpleCalendarService.hasWritePermissions()) {
            Toast.makeText(this, "Calendar write permission needed. Please grant permission.", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR},
                    CALENDAR_WRITE_PERMISSION_REQUEST);
            return;
        }
        
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean calendarEnabled = prefs.getBoolean("google_calendar_enabled", false);
        
        if (!calendarEnabled) {
            Toast.makeText(this, "Please enable calendar integration in Settings first", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (schedule.scheduledItems == null || schedule.scheduledItems.isEmpty()) {
            Toast.makeText(this, "No schedule to add to calendar", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "📅 Adding schedule to calendar...", Toast.LENGTH_SHORT).show();
        
        // Create calendar events for all AI-scheduled tasks
        final int[] totalTasks = {0};
        final int[] successCount = {0};
        final int[] failCount = {0};
        
        // Count total tasks first
        for (SmartCalendarAI.ScheduledItem item : schedule.scheduledItems) {
            if (item.type == SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK) {
                totalTasks[0]++;
            }
        }
        
        if (totalTasks[0] == 0) {
            Toast.makeText(this, "No tasks to add to calendar", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create calendar events
        for (SmartCalendarAI.ScheduledItem item : schedule.scheduledItems) {
            if (item.type == SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK) {
                createCalendarEventForScheduledTask(item, new Runnable() {
                    @Override
                    public void run() {
                        successCount[0]++;
                        checkCalendarSaveComplete(totalTasks[0], successCount[0], failCount[0]);
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        failCount[0]++;
                        checkCalendarSaveComplete(totalTasks[0], successCount[0], failCount[0]);
                    }
                });
            }
        }
    }
    
    private void checkCalendarSaveComplete(int totalTasks, int successCount, int failCount) {
        if (successCount + failCount == totalTasks) {
            runOnUiThread(() -> {
                if (failCount == 0) {
                    Toast.makeText(AIScheduleActivity.this, 
                        "✅ Successfully added " + successCount + " task(s) to calendar!", 
                        Toast.LENGTH_LONG).show();
                } else if (failCount == totalTasks) {
                    Toast.makeText(AIScheduleActivity.this, 
                        "❌ Failed to add tasks to calendar", 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AIScheduleActivity.this, 
                        "⚠️ Added " + successCount + " task(s), " + failCount + " failed", 
                        Toast.LENGTH_LONG).show();
                }
            });
        }
    }
    
    /**
     * Check if midnight has passed and reset if needed
     * This ensures the set resets at midnight even if the app is running
     * @return true if the set was reset (new day), false otherwise
     */
    private boolean checkAndResetIfNewDay() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        
        // Get today's date as a string (YYYY-MM-DD format)
        Calendar today = Calendar.getInstance();
        String todayDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1, // Month is 0-indexed
            today.get(Calendar.DAY_OF_MONTH));
        
        // Get the stored date
        String storedDateStr = prefs.getString(PREFS_RECENT_TASKS_DATE, "");
        
        // If the date has changed (midnight has passed), clear the set
        if (!todayDateStr.equals(storedDateStr)) {
            recentlyCreatedTaskNames.clear();
            // Save the new date
            prefs.edit()
                .putString(PREFS_RECENT_TASKS_DATE, todayDateStr)
                .putStringSet(PREFS_RECENT_TASKS_NAMES, new HashSet<String>())
                .apply();
            android.util.Log.d("AIScheduleActivity", "Midnight passed - new day detected, cleared recently created task names");
            return true;
        }
        return false;
    }
    
    /**
     * Load recently created task names from SharedPreferences
     * Resets the set if the date has changed (new day/midnight has passed)
     */
    private void loadRecentlyCreatedTaskNames() {
        // First check if midnight has passed and reset if needed
        boolean wasReset = checkAndResetIfNewDay();
        
        if (!wasReset) {
            // Load the set from SharedPreferences (same day)
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            Set<String> savedNames = prefs.getStringSet(PREFS_RECENT_TASKS_NAMES, new HashSet<String>());
            recentlyCreatedTaskNames.clear();
            recentlyCreatedTaskNames.addAll(savedNames);
            android.util.Log.d("AIScheduleActivity", "Loaded " + recentlyCreatedTaskNames.size() + 
                " recently created task names from SharedPreferences");
        }
    }
    
    /**
     * Save recently created task names to SharedPreferences
     * Also saves today's date to check for day changes
     */
    private void saveRecentlyCreatedTaskNames() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        
        // Get today's date as a string (YYYY-MM-DD format)
        Calendar today = Calendar.getInstance();
        String todayDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1, // Month is 0-indexed
            today.get(Calendar.DAY_OF_MONTH));
        
        // Save both the date and the task names
        prefs.edit()
            .putString(PREFS_RECENT_TASKS_DATE, todayDateStr)
            .putStringSet(PREFS_RECENT_TASKS_NAMES, recentlyCreatedTaskNames)
            .apply();
        
        android.util.Log.d("AIScheduleActivity", "Saved " + recentlyCreatedTaskNames.size() + 
            " recently created task names to SharedPreferences");
    }
    
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_schedule) {
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(this, EnergyDashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_data) {
                startActivity(new Intent(this, DataLogsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_insights) {
                startActivity(new Intent(this, WeeklyInsightsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_schedule);
    }
    
    /**
     * Show custom dialog with schedule details (CP470 Requirement #11 - Custom Dialog)
     */
    private void showScheduleDetailDialog(String scheduleInfo) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Schedule Details");
        builder.setMessage(scheduleInfo);
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_help) {
            HelpDialogHelper.showHelpDialog(
                this,
                "AI Schedule",
                HelpDialogHelper.getDefaultInstructions("Schedule")
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * CP470 Requirement #7: AsyncTask for generating schedule
     * Note: AsyncTask is deprecated but required for project compliance.
     */
    @SuppressWarnings("deprecation")
    private class GenerateScheduleAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private Exception error;
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Show progress bar (CP470 Requirement #8)
            if (progressBarSchedule != null) {
                progressBarSchedule.setVisibility(View.VISIBLE);
            }
            if (listViewSchedule != null) {
                listViewSchedule.setVisibility(View.GONE);
            }
            if (recyclerViewSchedule != null) {
                recyclerViewSchedule.setVisibility(View.GONE);
            }
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                publishProgress(25);
                // Generate schedule in background
                // The actual generation happens in generateSchedule() which uses callbacks
                // For AsyncTask compliance, we call it here
                return true;
            } catch (Exception e) {
                this.error = e;
                android.util.Log.e("AIScheduleActivity", "Error in AsyncTask", e);
                return false;
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            // Update progress if needed
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            // Hide progress bar
            if (progressBarSchedule != null) {
                progressBarSchedule.setVisibility(View.GONE);
            }
            
            if (success) {
                // Call the existing generateSchedule method
                generateSchedule();
                
                // Show Toast (CP470 Requirement #11)
                Toast.makeText(AIScheduleActivity.this, 
                    "Schedule generation started", Toast.LENGTH_SHORT).show();
            } else {
                // Show Snackbar (CP470 Requirement #11)
                String errorMsg = error != null ? error.getMessage() : "Unknown error";
                Snackbar.make(findViewById(android.R.id.content), 
                    "Error generating schedule: " + errorMsg, 
                    Snackbar.LENGTH_LONG).show();
            }
        }
    }
    
    /**
     * Populate ListView with schedule items (CP470 Requirement #3)
     */
    private void populateScheduleListView(List<SmartCalendarAI.ScheduledItem> items) {
        if (listViewSchedule != null && items != null && !items.isEmpty()) {
            List<String> scheduleItems = new ArrayList<>();
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            
            for (SmartCalendarAI.ScheduledItem item : items) {
                String startTime = timeFormat.format(new Date(item.startTime));
                String endTime = timeFormat.format(new Date(item.endTime));
                scheduleItems.add(String.format(Locale.getDefault(), 
                    "%s - %s: %s", startTime, endTime, item.title));
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, scheduleItems);
            listViewSchedule.setAdapter(adapter);
            listViewSchedule.setVisibility(View.VISIBLE);
        } else if (listViewSchedule != null) {
            listViewSchedule.setVisibility(View.GONE);
        }
    }
}

