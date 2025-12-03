package com.flowstate.app.ai;

import android.content.Context;
import android.util.Log;
import com.flowstate.app.calendar.SimpleCalendarService;
import com.flowstate.app.data.models.*;
import com.flowstate.app.supabase.repository.BiometricDataRepository;
import com.flowstate.app.supabase.repository.EnergyPredictionRepository;
import com.flowstate.app.supabase.repository.TypingSpeedRepository;
import com.flowstate.app.supabase.repository.ReactionTimeRepository;

import com.personaleenergy.app.llm.GeminiService;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI-powered Smart Calendar Generator
 * 
 * Analyzes all available user data to create an optimized schedule:
 * - Biometric data (heart rate, sleep, temperature)
 * - Cognitive performance (typing speed, reaction time)
 * - Energy predictions
 * - Existing calendar events
 * - User tasks and preferences
 */
public class SmartCalendarAI {
    
    private static final String TAG = "SmartCalendarAI";
    private Context context;
    private ExecutorService executorService;
    
    // Repositories for data access
    private BiometricDataRepository biometricRepo;
    private EnergyPredictionRepository energyRepo;
    private TypingSpeedRepository typingRepo;
    private ReactionTimeRepository reactionRepo;
    private SimpleCalendarService calendarService;
    private GeminiService geminiService;
    
    public SmartCalendarAI(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.biometricRepo = new BiometricDataRepository(context);
        this.energyRepo = new EnergyPredictionRepository(context);
        this.typingRepo = new TypingSpeedRepository(context);
        this.reactionRepo = new ReactionTimeRepository(context);
        this.calendarService = new SimpleCalendarService(context);
        this.geminiService = new GeminiService(context);
    }
    
    /**
     * Generate a smart calendar schedule for the user
     */
    public void generateSmartSchedule(String userId, List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> userTasksWithEnergy, 
                                     SmartScheduleCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting smart schedule generation for user: " + userId);
                
                // Step 1: Collect all user data
                UserDataProfile profile = collectUserData(userId);
                
                // Step 2: Get existing calendar events
                List<SimpleCalendarService.CalendarEvent> existingEvents = getExistingEvents();
                
                // Step 3: Convert existing events to string list
                List<String> existingEventStrings = new ArrayList<>();
                for (SimpleCalendarService.CalendarEvent event : existingEvents) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(event.startTime);
                    String timeStr = String.format("%d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
                    existingEventStrings.add(timeStr + " - " + event.title);
                }
                
                // Convert tasks with energy to simple list for OpenAI
                List<String> userTaskNames = new ArrayList<>();
                for (com.personaleenergy.app.ui.schedule.TaskWithEnergy taskWithEnergy : userTasksWithEnergy) {
                    userTaskNames.add(taskWithEnergy.getTaskName());
                }
                
                // Step 4: Use OpenAI to generate schedule based on energy predictions
                // First, get today's energy predictions
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);
                Date todayStart = today.getTime();
                
                Calendar tomorrow = Calendar.getInstance();
                tomorrow.setTime(todayStart);
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);
                Date tomorrowStart = tomorrow.getTime();
                
                // Get energy predictions for today
                final List<EnergyPrediction>[] todayPredictions = new List[]{new ArrayList<>()};
                final Object lock = new Object();
                final java.util.concurrent.CountDownLatch predictionsLatch = new java.util.concurrent.CountDownLatch(1);
                
                // Use profile predictions if available, otherwise fetch from database
                if (!profile.energyPredictions.isEmpty()) {
                    // Filter to today's predictions
                    List<EnergyPrediction> todayPreds = new ArrayList<>();
                    for (EnergyPrediction pred : profile.energyPredictions) {
                        if (pred.getTimestamp().after(todayStart) && pred.getTimestamp().before(tomorrowStart)) {
                            todayPreds.add(pred);
                        }
                    }
                    todayPredictions[0] = todayPreds;
                    predictionsLatch.countDown();
                } else {
                    energyRepo.getEnergyPredictions(userId, todayStart, tomorrowStart, 
                    new EnergyPredictionRepository.DataCallback() {
                        @Override
                        public void onSuccess(Object data) {
                            synchronized (lock) {
                                @SuppressWarnings("unchecked")
                                List<EnergyPrediction> preds = (List<EnergyPrediction>) data;
                                todayPredictions[0] = preds != null ? preds : profile.energyPredictions;
                            }
                            predictionsLatch.countDown();
                        }
                        
                        @Override
                        public void onError(Throwable error) {
                            Log.e(TAG, "Error getting today's predictions, using empty list", error);
                            synchronized (lock) {
                                todayPredictions[0] = new ArrayList<>();
                            }
                            predictionsLatch.countDown();
                        }
                    });
                }
                
                try {
                    predictionsLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Extract sleep pattern information
                String sleepPatternInfo = extractSleepPatternInfo(profile);
                
                // Use OpenAI to generate schedule with sleep patterns
                if (sleepPatternInfo != null && !sleepPatternInfo.isEmpty()) {
                    Log.d(TAG, "🤖 Attempting to generate schedule with Gemini (with sleep patterns)...");
                    geminiService.generateScheduleWithSleep(
                        todayPredictions[0],
                        userTaskNames,
                        userTasksWithEnergy,
                        existingEventStrings,
                        sleepPatternInfo,
                        new GeminiService.ScheduleCallback() {
                            @Override
                            public void onSuccess(String aiSchedule) {
                                Log.d(TAG, "✅ Gemini schedule generation successful (with sleep patterns)!");
                                // Parse AI schedule and combine with logic-based optimization
                                SmartSchedule schedule = parseAISchedule(aiSchedule, todayPredictions[0], 
                                    existingEvents, userTasksWithEnergy, profile);
                                callback.onSuccess(schedule);
                            }
                            
                            @Override
                            public void onError(Exception error) {
                                Log.e(TAG, "❌ Gemini schedule generation failed (with sleep patterns), using fallback", error);
                                Log.e(TAG, "Error message: " + error.getMessage());
                                // Fallback to rule-based schedule
                                EnergyPatternAnalysis energyPattern = analyzeEnergyPatterns(profile);
                                CognitivePatternAnalysis cognitivePattern = analyzeCognitivePatterns(profile);
                                // Convert TaskWithEnergy to TaskWithEnergyRequirement
                                List<TaskWithEnergyRequirement> categorizedTasks = new ArrayList<>();
                                for (com.personaleenergy.app.ui.schedule.TaskWithEnergy task : userTasksWithEnergy) {
                                    EnergyRequirement req = EnergyRequirement.MEDIUM;
                                    switch (task.getEnergyLevel()) {
                                        case HIGH:
                                            req = EnergyRequirement.HIGH;
                                            break;
                                        case LOW:
                                            req = EnergyRequirement.LOW;
                                            break;
                                    }
                                    categorizedTasks.add(new TaskWithEnergyRequirement(task.getTaskName(), req));
                                }
                                SmartSchedule fallbackSchedule = createOptimizedSchedule(
                                    energyPattern, cognitivePattern, existingEvents, categorizedTasks);
                                callback.onSuccess(fallbackSchedule);
                            }
                        });
                } else {
                    // No sleep data, use regular schedule generation
                    Log.d(TAG, "🤖 Attempting to generate schedule with Gemini (no sleep patterns)...");
                    geminiService.generateSchedule(
                        todayPredictions[0],
                        userTaskNames,
                        userTasksWithEnergy,
                        existingEventStrings,
                        new GeminiService.ScheduleCallback() {
                            @Override
                            public void onSuccess(String aiSchedule) {
                                Log.d(TAG, "✅ Gemini schedule generation successful (no sleep patterns)!");
                                // Parse AI schedule and combine with logic-based optimization
                                SmartSchedule schedule = parseAISchedule(aiSchedule, todayPredictions[0], 
                                    existingEvents, userTasksWithEnergy, profile);
                                callback.onSuccess(schedule);
                            }
                            
                            @Override
                            public void onError(Exception error) {
                                Log.e(TAG, "❌ Gemini schedule generation failed (no sleep patterns), using fallback", error);
                                Log.e(TAG, "Error message: " + error.getMessage());
                                // Fallback to rule-based schedule
                                EnergyPatternAnalysis energyPattern = analyzeEnergyPatterns(profile);
                                CognitivePatternAnalysis cognitivePattern = analyzeCognitivePatterns(profile);
                                // Convert TaskWithEnergy to TaskWithEnergyRequirement
                                List<TaskWithEnergyRequirement> categorizedTasks = new ArrayList<>();
                                for (com.personaleenergy.app.ui.schedule.TaskWithEnergy task : userTasksWithEnergy) {
                                    EnergyRequirement req = EnergyRequirement.MEDIUM;
                                    switch (task.getEnergyLevel()) {
                                        case HIGH:
                                            req = EnergyRequirement.HIGH;
                                            break;
                                        case MEDIUM:
                                            req = EnergyRequirement.MEDIUM;
                                            break;
                                        case LOW:
                                            req = EnergyRequirement.LOW;
                                            break;
                                    }
                                    categorizedTasks.add(new TaskWithEnergyRequirement(task.getTaskName(), req));
                                }
                                SmartSchedule schedule = createOptimizedSchedule(
                                    energyPattern, 
                                    cognitivePattern, 
                                    existingEvents, 
                                    categorizedTasks
                                );
                                schedule.summary = "Schedule generated using rule-based optimization (OpenAI unavailable).\n\n" + schedule.summary;
                                callback.onSuccess(schedule);
                            }
                        });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating smart schedule", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Collect all available user data (using callbacks since repositories are async)
     */
    private UserDataProfile collectUserData(String userId) {
        UserDataProfile profile = new UserDataProfile();
        
        try {
            // Get biometric data (last 30 days)
            long endTime = System.currentTimeMillis();
            long startTime = endTime - (30 * 24 * 60 * 60 * 1000L);
            java.util.Date startDate = new java.util.Date(startTime);
            java.util.Date endDate = new java.util.Date(endTime);
            
            // Use CountDownLatch to wait for all async calls
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(4);
            final Object lock = new Object();
            
            // Get biometric data
            biometricRepo.getBiometricData(userId, startDate, endDate, new BiometricDataRepository.DataCallback() {
                @Override
                public void onSuccess(Object data) {
                    synchronized (lock) {
                        @SuppressWarnings("unchecked")
                        List<BiometricData> biometricList = (List<BiometricData>) data;
                        profile.biometricData = biometricList != null ? biometricList : new ArrayList<>();
                        Log.d(TAG, "Collected " + profile.biometricData.size() + " biometric data points");
                    }
                    latch.countDown();
                }
                
                @Override
                public void onError(Throwable error) {
                    Log.e(TAG, "Error getting biometric data", error);
                    latch.countDown();
                }
            });
            
            // Get energy predictions
            java.util.Date futureEndDate = new java.util.Date(endTime + (24 * 60 * 60 * 1000L));
            energyRepo.getEnergyPredictions(userId, startDate, futureEndDate, new EnergyPredictionRepository.DataCallback() {
                @Override
                public void onSuccess(Object data) {
                    synchronized (lock) {
                        @SuppressWarnings("unchecked")
                        List<EnergyPrediction> energyList = (List<EnergyPrediction>) data;
                        profile.energyPredictions = energyList != null ? energyList : new ArrayList<>();
                        Log.d(TAG, "Collected " + profile.energyPredictions.size() + " energy predictions");
                    }
                    latch.countDown();
                }
                
                @Override
                public void onError(Throwable error) {
                    Log.e(TAG, "Error getting energy predictions", error);
                    latch.countDown();
                }
            });
            
            // Get typing speed data
            typingRepo.getTypingSpeedData(userId, startDate, endDate, new TypingSpeedRepository.DataCallback() {
                @Override
                public void onSuccess(Object data) {
                    synchronized (lock) {
                        @SuppressWarnings("unchecked")
                        List<TypingSpeedData> typingList = (List<TypingSpeedData>) data;
                        profile.typingData = typingList != null ? typingList : new ArrayList<>();
                        Log.d(TAG, "Collected " + profile.typingData.size() + " typing speed tests");
                    }
                    latch.countDown();
                }
                
                @Override
                public void onError(Throwable error) {
                    Log.e(TAG, "Error getting typing speed data", error);
                    latch.countDown();
                }
            });
            
            // Get reaction time data
            reactionRepo.getReactionTimeData(userId, startDate, endDate, new ReactionTimeRepository.DataCallback() {
                @Override
                public void onSuccess(Object data) {
                    synchronized (lock) {
                        @SuppressWarnings("unchecked")
                        List<ReactionTimeData> reactionList = (List<ReactionTimeData>) data;
                        profile.reactionData = reactionList != null ? reactionList : new ArrayList<>();
                        Log.d(TAG, "Collected " + profile.reactionData.size() + " reaction time tests");
                    }
                    latch.countDown();
                }
                
                @Override
                public void onError(Throwable error) {
                    Log.e(TAG, "Error getting reaction time data", error);
                    latch.countDown();
                }
            });
            
            // Wait for all data to be collected (max 10 seconds)
            try {
                latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Interrupted while waiting for data", e);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting user data", e);
        }
        
        return profile;
    }
    
    /**
     * Analyze energy patterns throughout the day
     */
    private EnergyPatternAnalysis analyzeEnergyPatterns(UserDataProfile profile) {
        EnergyPatternAnalysis analysis = new EnergyPatternAnalysis();
        
        // Analyze energy predictions by hour
        Map<Integer, List<EnergyPrediction>> predictionsByHour = new HashMap<>();
        for (EnergyPrediction pred : profile.energyPredictions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pred.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            predictionsByHour.computeIfAbsent(hour, k -> new ArrayList<>()).add(pred);
        }
        
        // Find peak energy hours
        Map<Integer, Double> avgEnergyByHour = new HashMap<>();
        for (Map.Entry<Integer, List<EnergyPrediction>> entry : predictionsByHour.entrySet()) {
            double avgScore = entry.getValue().stream()
                .mapToDouble(p -> {
                    switch (p.getPredictedLevel()) {
                        case HIGH: return 3.0;
                        case MEDIUM: return 2.0;
                        case LOW: return 1.0;
                        default: return 2.0;
                    }
                })
                .average()
                .orElse(2.0);
            avgEnergyByHour.put(entry.getKey(), avgScore);
        }
        
        // Find peak hours (top 3 hours with highest energy)
        List<Map.Entry<Integer, Double>> sortedHours = new ArrayList<>(avgEnergyByHour.entrySet());
        sortedHours.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        analysis.peakEnergyHours = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sortedHours.size()); i++) {
            analysis.peakEnergyHours.add(sortedHours.get(i).getKey());
        }
        
        // Find low energy hours
        analysis.lowEnergyHours = new ArrayList<>();
        for (int i = sortedHours.size() - 1; i >= Math.max(0, sortedHours.size() - 3); i--) {
            analysis.lowEnergyHours.add(sortedHours.get(i).getKey());
        }
        
        // Analyze sleep patterns
        if (!profile.biometricData.isEmpty()) {
            double avgSleepQuality = profile.biometricData.stream()
                .filter(d -> d.getSleepQuality() != null)
                .mapToDouble(BiometricData::getSleepQuality)
                .average()
                .orElse(0.7);
            analysis.avgSleepQuality = avgSleepQuality;
            
            double avgSleepMinutes = profile.biometricData.stream()
                .filter(d -> d.getSleepMinutes() != null)
                .mapToInt(BiometricData::getSleepMinutes)
                .average()
                .orElse(480.0); // 8 hours default
            analysis.avgSleepMinutes = avgSleepMinutes;
        }
        
        Log.d(TAG, "Energy analysis: Peak hours=" + analysis.peakEnergyHours + 
              ", Low hours=" + analysis.lowEnergyHours);
        
        return analysis;
    }
    
    /**
     * Analyze cognitive performance patterns
     */
    private CognitivePatternAnalysis analyzeCognitivePatterns(UserDataProfile profile) {
        CognitivePatternAnalysis analysis = new CognitivePatternAnalysis();
        
        // Analyze typing speed by hour
        Map<Integer, List<TypingSpeedData>> typingByHour = new HashMap<>();
        for (TypingSpeedData data : profile.typingData) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(data.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            typingByHour.computeIfAbsent(hour, k -> new ArrayList<>()).add(data);
        }
        
        Map<Integer, Double> avgWPMByHour = new HashMap<>();
        for (Map.Entry<Integer, List<TypingSpeedData>> entry : typingByHour.entrySet()) {
            double avgWPM = entry.getValue().stream()
                .mapToInt(TypingSpeedData::getWordsPerMinute)
                .average()
                .orElse(0.0);
            avgWPMByHour.put(entry.getKey(), avgWPM);
        }
        
        // Find peak cognitive performance hours
        if (!avgWPMByHour.isEmpty()) {
            List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(avgWPMByHour.entrySet());
            sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            analysis.peakCognitiveHours = new ArrayList<>();
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                analysis.peakCognitiveHours.add(sorted.get(i).getKey());
            }
        }
        
        // Analyze reaction time
        if (!profile.reactionData.isEmpty()) {
            double avgReactionTime = profile.reactionData.stream()
                .mapToDouble(d -> d.getAverageReactionTimeMs() != null ? 
                    d.getAverageReactionTimeMs() : d.getReactionTimeMs())
                .average()
                .orElse(250.0);
            analysis.avgReactionTime = avgReactionTime;
        }
        
        return analysis;
    }
    
    /**
     * Get existing calendar events
     */
    private List<SimpleCalendarService.CalendarEvent> getExistingEvents() {
        List<SimpleCalendarService.CalendarEvent> events = new ArrayList<>();
        
        if (calendarService.hasPermissions()) {
            try {
                long now = System.currentTimeMillis();
                long endOfDay = now + (24 * 60 * 60 * 1000L);
                
                final List<SimpleCalendarService.CalendarEvent>[] result = new List[]{new ArrayList<>()};
                
                calendarService.getEvents(now, endOfDay, new SimpleCalendarService.CalendarEventsCallback() {
                    @Override
                    public void onSuccess(List<SimpleCalendarService.CalendarEvent> eventList) {
                        result[0] = eventList;
                    }
                    
                    @Override
                    public void onError(Exception error) {
                        Log.e(TAG, "Error fetching calendar events", error);
                    }
                });
                
                // Wait a bit for async call (in production, use proper async handling)
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                events = result[0];
            } catch (Exception e) {
                Log.e(TAG, "Error getting calendar events", e);
            }
        }
        
        return events;
    }
    
    /**
     * Categorize tasks by energy requirement
     */
    private List<TaskWithEnergyRequirement> categorizeTasks(List<String> tasks, UserDataProfile profile) {
        List<TaskWithEnergyRequirement> categorized = new ArrayList<>();
        
        for (String task : tasks) {
            String lowerTask = task.toLowerCase();
            EnergyRequirement requirement = EnergyRequirement.MEDIUM;
            
            // High energy tasks
            if (lowerTask.contains("code") || lowerTask.contains("program") || 
                lowerTask.contains("write") || lowerTask.contains("design") ||
                lowerTask.contains("analyze") || lowerTask.contains("create") ||
                lowerTask.contains("build") || lowerTask.contains("develop")) {
                requirement = EnergyRequirement.HIGH;
            }
            // Low energy tasks
            else if (lowerTask.contains("read") || lowerTask.contains("review") ||
                     lowerTask.contains("email") || lowerTask.contains("organize") ||
                     lowerTask.contains("plan") || lowerTask.contains("meeting") ||
                     lowerTask.contains("call")) {
                requirement = EnergyRequirement.LOW;
            }
            
            categorized.add(new TaskWithEnergyRequirement(task, requirement));
        }
        
        return categorized;
    }
    
    /**
     * Create optimized schedule
     */
    private SmartSchedule createOptimizedSchedule(
            EnergyPatternAnalysis energyPattern,
            CognitivePatternAnalysis cognitivePattern,
            List<SimpleCalendarService.CalendarEvent> existingEvents,
            List<TaskWithEnergyRequirement> tasks) {
        
        SmartSchedule schedule = new SmartSchedule();
        schedule.scheduledItems = new ArrayList<>();
        
        Calendar now = Calendar.getInstance();
        Calendar endOfDay = Calendar.getInstance();
        endOfDay.set(Calendar.HOUR_OF_DAY, 23);
        endOfDay.set(Calendar.MINUTE, 59);
        
        // Create time slots (every hour)
        List<TimeSlot> timeSlots = new ArrayList<>();
        Calendar current = (Calendar) now.clone();
        while (current.before(endOfDay)) {
            TimeSlot slot = new TimeSlot();
            slot.startTime = current.getTimeInMillis();
            slot.hour = current.get(Calendar.HOUR_OF_DAY);
            slot.isAvailable = true;
            
            // Check if there's an existing event
            for (SimpleCalendarService.CalendarEvent event : existingEvents) {
                if (event.startTime <= slot.startTime && event.endTime > slot.startTime) {
                    slot.isAvailable = false;
                    ScheduledItem item = new ScheduledItem();
                    item.title = event.title;
                    item.startTime = slot.startTime;
                    item.endTime = event.endTime;
                    item.type = ScheduledItemType.EXISTING_EVENT;
                    schedule.scheduledItems.add(item);
                    break;
                }
            }
            
            if (slot.isAvailable) {
                timeSlots.add(slot);
            }
            
            current.add(Calendar.HOUR_OF_DAY, 1);
        }
        
        // Schedule high-energy tasks during peak hours
        List<TaskWithEnergyRequirement> highEnergyTasks = new ArrayList<>();
        List<TaskWithEnergyRequirement> mediumEnergyTasks = new ArrayList<>();
        List<TaskWithEnergyRequirement> lowEnergyTasks = new ArrayList<>();
        
        for (TaskWithEnergyRequirement task : tasks) {
            switch (task.requirement) {
                case HIGH:
                    highEnergyTasks.add(task);
                    break;
                case MEDIUM:
                    mediumEnergyTasks.add(task);
                    break;
                case LOW:
                    lowEnergyTasks.add(task);
                    break;
            }
        }
        
        // Schedule high-energy tasks (ensuring no conflicts with existing events)
        for (TaskWithEnergyRequirement task : highEnergyTasks) {
            TimeSlot bestSlot = findBestSlot(timeSlots, energyPattern.peakEnergyHours, 
                                            cognitivePattern.peakCognitiveHours, existingEvents);
            if (bestSlot != null) {
                ScheduledItem item = new ScheduledItem();
                item.title = task.task;
                item.startTime = bestSlot.startTime;
                item.endTime = bestSlot.startTime + (60 * 60 * 1000L); // 1 hour
                item.type = ScheduledItemType.AI_SCHEDULED_TASK;
                item.energyLevel = EnergyLevel.HIGH;
                item.reasoning = "Scheduled during peak energy hours (" + bestSlot.hour + ":00) for optimal performance";
                schedule.scheduledItems.add(item);
                timeSlots.remove(bestSlot);
            }
        }
        
        // Schedule medium-energy tasks (ensuring no conflicts with existing events)
        for (TaskWithEnergyRequirement task : mediumEnergyTasks) {
            TimeSlot bestSlot = findBestSlot(timeSlots, null, null, existingEvents);
            if (bestSlot != null) {
                ScheduledItem item = new ScheduledItem();
                item.title = task.task;
                item.startTime = bestSlot.startTime;
                item.endTime = bestSlot.startTime + (60 * 60 * 1000L);
                item.type = ScheduledItemType.AI_SCHEDULED_TASK;
                item.energyLevel = EnergyLevel.MEDIUM;
                item.reasoning = "Scheduled at " + bestSlot.hour + ":00";
                schedule.scheduledItems.add(item);
                timeSlots.remove(bestSlot);
            }
        }
        
        // Schedule low-energy tasks during low energy hours (ensuring no conflicts with existing events)
        for (TaskWithEnergyRequirement task : lowEnergyTasks) {
            TimeSlot bestSlot = findBestSlot(timeSlots, energyPattern.lowEnergyHours, null, existingEvents);
            if (bestSlot != null) {
                ScheduledItem item = new ScheduledItem();
                item.title = task.task;
                item.startTime = bestSlot.startTime;
                item.endTime = bestSlot.startTime + (60 * 60 * 1000L);
                item.type = ScheduledItemType.AI_SCHEDULED_TASK;
                item.energyLevel = EnergyLevel.LOW;
                item.reasoning = "Scheduled during low energy period (" + bestSlot.hour + ":00) for less demanding tasks";
                schedule.scheduledItems.add(item);
                timeSlots.remove(bestSlot);
            }
        }
        
        // Sort by start time
        schedule.scheduledItems.sort((a, b) -> Long.compare(a.startTime, b.startTime));
        
        // Generate summary
        schedule.summary = generateSummary(schedule, energyPattern, tasks.size());
        
        return schedule;
    }
    
    /**
     * Find best time slot for a task
     * Ensures no conflicts with existing immutable calendar events
     */
    private TimeSlot findBestSlot(List<TimeSlot> slots, List<Integer> preferredHours, 
                                 List<Integer> cognitiveHours,
                                 List<SimpleCalendarService.CalendarEvent> existingEvents) {
        if (slots.isEmpty()) return null;
        
        // Helper to check if a slot conflicts with existing events
        java.util.function.Predicate<TimeSlot> isSlotAvailable = slot -> {
            if (!slot.isAvailable) return false;
            // Check if this slot conflicts with any existing immutable event
            long slotStart = slot.startTime;
            long slotEnd = slotStart + (60 * 60 * 1000L); // 1 hour duration
            
            for (SimpleCalendarService.CalendarEvent event : existingEvents) {
                // Check for overlap
                if ((slotStart >= event.startTime && slotStart < event.endTime) ||
                    (slotEnd > event.startTime && slotEnd <= event.endTime) ||
                    (slotStart <= event.startTime && slotEnd >= event.endTime)) {
                    return false; // Conflicts with existing event
                }
            }
            return true;
        };
        
        // If preferred hours specified, find matching slot that doesn't conflict
        if (preferredHours != null && !preferredHours.isEmpty()) {
            for (Integer hour : preferredHours) {
                for (TimeSlot slot : slots) {
                    if (slot.hour == hour && isSlotAvailable.test(slot)) {
                        return slot;
                    }
                }
            }
        }
        
        // If cognitive hours specified, try those (without conflicts)
        if (cognitiveHours != null && !cognitiveHours.isEmpty()) {
            for (Integer hour : cognitiveHours) {
                for (TimeSlot slot : slots) {
                    if (slot.hour == hour && isSlotAvailable.test(slot)) {
                        return slot;
                    }
                }
            }
        }
        
        // Return first available slot that doesn't conflict
        for (TimeSlot slot : slots) {
            if (isSlotAvailable.test(slot)) {
                return slot;
            }
        }
        
        return null;
    }
    
    /**
     * Generate schedule summary
     */
    private String generateSummary(SmartSchedule schedule, EnergyPatternAnalysis energyPattern, 
                                   int totalTasks) {
        StringBuilder summary = new StringBuilder();
        summary.append("📅 Smart Schedule Generated\n\n");
        summary.append("✅ Scheduled ").append(schedule.scheduledItems.size()).append(" items\n");
        summary.append("⏰ Peak energy hours: ").append(energyPattern.peakEnergyHours).append("\n");
        summary.append("💤 Average sleep quality: ").append(String.format("%.1f", energyPattern.avgSleepQuality * 100)).append("%\n");
        summary.append("\nYour schedule is optimized based on:\n");
        summary.append("• Energy level predictions\n");
        summary.append("• Cognitive performance patterns\n");
        summary.append("• Existing calendar events\n");
        summary.append("• Task energy requirements\n");
        
        return summary.toString();
    }
    
    // Data classes
    private static class UserDataProfile {
        List<BiometricData> biometricData = new ArrayList<>();
        List<EnergyPrediction> energyPredictions = new ArrayList<>();
        List<TypingSpeedData> typingData = new ArrayList<>();
        List<ReactionTimeData> reactionData = new ArrayList<>();
    }
    
    private static class EnergyPatternAnalysis {
        List<Integer> peakEnergyHours = new ArrayList<>();
        List<Integer> lowEnergyHours = new ArrayList<>();
        double avgSleepQuality = 0.7;
        double avgSleepMinutes = 480.0;
    }
    
    private static class CognitivePatternAnalysis {
        List<Integer> peakCognitiveHours = new ArrayList<>();
        double avgReactionTime = 250.0;
    }
    
    private static class TaskWithEnergyRequirement {
        String task;
        EnergyRequirement requirement;
        
        TaskWithEnergyRequirement(String task, EnergyRequirement requirement) {
            this.task = task;
            this.requirement = requirement;
        }
    }
    
    private enum EnergyRequirement {
        HIGH, MEDIUM, LOW
    }
    
    private static class TimeSlot {
        long startTime;
        int hour;
        boolean isAvailable;
    }
    
    // Public data classes
    public static class SmartSchedule {
        public List<ScheduledItem> scheduledItems;
        public String summary;
    }
    
    public static class ScheduledItem {
        public String title;
        public long startTime;
        public long endTime;
        public ScheduledItemType type;
        public EnergyLevel energyLevel;
        public String reasoning;
    }
    
    /**
     * Parse AI-generated schedule and combine with logic-based optimization
     */
    private SmartSchedule parseAISchedule(String aiSchedule, List<EnergyPrediction> energyPredictions,
                                         List<SimpleCalendarService.CalendarEvent> existingEvents,
                                         List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> userTasksWithEnergy,
                                         UserDataProfile profile) {
        SmartSchedule schedule = new SmartSchedule();
        schedule.scheduledItems = new ArrayList<>();
        
        // Add existing events first
        for (SimpleCalendarService.CalendarEvent event : existingEvents) {
            ScheduledItem item = new ScheduledItem();
            item.title = event.title;
            item.startTime = event.startTime;
            item.endTime = event.endTime;
            item.type = ScheduledItemType.EXISTING_EVENT;
            schedule.scheduledItems.add(item);
        }
        
        // Parse AI schedule text and extract scheduled items
        String[] lines = aiSchedule.split("\n");
        Calendar today = Calendar.getInstance();
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("*")) continue;
            
            // Try to parse time patterns like "9:00 AM", "14:00", "9:00-10:00", etc.
            java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile(
                "(\\d{1,2}):(\\d{2})\\s*(AM|PM|am|pm)?"
            );
            java.util.regex.Matcher matcher = timePattern.matcher(line);
            
            if (matcher.find()) {
                try {
                    int hour = Integer.parseInt(matcher.group(1));
                    int minute = Integer.parseInt(matcher.group(2));
                    String ampm = matcher.group(3);
                    
                    // Convert to 24-hour format
                    if (ampm != null && (ampm.equalsIgnoreCase("PM") || ampm.equalsIgnoreCase("pm"))) {
                        if (hour != 12) hour += 12;
                    } else if (ampm != null && (ampm.equalsIgnoreCase("AM") || ampm.equalsIgnoreCase("am"))) {
                        if (hour == 12) hour = 0;
                    }
                    
                    // Extract task description (everything after the time)
                    String taskDesc = line.substring(matcher.end()).trim();
                    // Remove common prefixes and separators
                    if (taskDesc.startsWith("-") || taskDesc.startsWith("•") || taskDesc.startsWith(":")) {
                        taskDesc = taskDesc.substring(1).trim();
                    }
                    // Also handle cases like "3:00 AM: Gym" where there's a colon after AM/PM
                    if (taskDesc.startsWith(":")) {
                        taskDesc = taskDesc.substring(1).trim();
                    }
                    
                    // Find matching task from user tasks
                    List<String> userTaskNames = new ArrayList<>();
                    for (com.personaleenergy.app.ui.schedule.TaskWithEnergy task : userTasksWithEnergy) {
                        userTaskNames.add(task.getTaskName());
                    }
                    String matchedTask = findMatchingTask(taskDesc, userTaskNames);
                    
                    // CRITICAL: Only add if we found a valid match - ignore explanatory text
                    if (matchedTask != null && userTaskNames.contains(matchedTask)) {
                        // CRITICAL: Check if time is during sleep hours using actual sleep pattern data
                        boolean isDuringSleepHours = isHourDuringSleep(hour, profile);
                        if (isDuringSleepHours) {
                            Log.d(TAG, "Skipping task '" + matchedTask + "' at " + hour + ":00 - scheduled during sleep hours");
                            continue; // Skip this task
                        }
                        
                        Calendar taskTime = Calendar.getInstance();
                        taskTime.set(Calendar.HOUR_OF_DAY, hour);
                        taskTime.set(Calendar.MINUTE, minute);
                        taskTime.set(Calendar.SECOND, 0);
                        taskTime.set(Calendar.MILLISECOND, 0);
                        long taskStartTime = taskTime.getTimeInMillis();
                        long taskEndTime = taskStartTime + (60 * 60 * 1000L); // 1 hour default
                        
                        // CRITICAL: Check if this time conflicts with existing immutable calendar events
                        boolean conflictsWithExisting = false;
                        for (SimpleCalendarService.CalendarEvent event : existingEvents) {
                            // Check if the task time overlaps with any existing event
                            if ((taskStartTime >= event.startTime && taskStartTime < event.endTime) ||
                                (taskEndTime > event.startTime && taskEndTime <= event.endTime) ||
                                (taskStartTime <= event.startTime && taskEndTime >= event.endTime)) {
                                conflictsWithExisting = true;
                                Log.d(TAG, "Skipping task '" + matchedTask + "' at " + hour + ":00 - conflicts with existing event: " + event.title);
                                break;
                            }
                        }
                        
                        // Only add if it doesn't conflict with existing events
                        if (!conflictsWithExisting) {
                            // Find energy level from user's task specification (not from predictions)
                            EnergyLevel energyLevel = EnergyLevel.MEDIUM; // Default
                            for (com.personaleenergy.app.ui.schedule.TaskWithEnergy taskWithEnergy : userTasksWithEnergy) {
                                if (taskWithEnergy.getTaskName().equals(matchedTask)) {
                                    energyLevel = taskWithEnergy.getEnergyLevel();
                                    break;
                                }
                            }
                            
                            ScheduledItem item = new ScheduledItem();
                            item.title = matchedTask;
                            item.startTime = taskStartTime;
                            item.endTime = taskEndTime;
                            item.type = ScheduledItemType.AI_SCHEDULED_TASK;
                            item.energyLevel = energyLevel; // Use user-specified energy level
                            item.reasoning = "AI-scheduled: " + taskDesc;
                            schedule.scheduledItems.add(item);
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Could not parse line: " + line, e);
                }
            }
        }
        
        // Sort by start time
        schedule.scheduledItems.sort((a, b) -> Long.compare(a.startTime, b.startTime));
        
        // Generate summary with AI schedule text
        schedule.summary = "📅 AI-Generated Schedule (Powered by ChatGPT)\n\n" + 
                         "✅ Scheduled " + schedule.scheduledItems.size() + " items\n\n" +
                         "AI Schedule:\n" + aiSchedule;
        
        return schedule;
    }
    
    /**
     * Find matching task from user tasks based on description
     * CRITICAL: Only returns a match if the description clearly matches a user task
     * Returns null if no match found (to prevent creating tasks from explanatory text)
     */
    private String findMatchingTask(String description, List<String> userTasks) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        
        String lowerDesc = description.toLowerCase().trim();
        
        // Remove common prefixes/suffixes that might be in explanatory text
        lowerDesc = lowerDesc.replaceAll("^(designed to|to|for|with|and|or|the|a|an)\\s+", "");
        lowerDesc = lowerDesc.replaceAll("\\s+(designed to|to|for|with|and|or|the|a|an)$", "");
        
        // Try exact match first (case-insensitive)
        for (String task : userTasks) {
            if (task == null || task.trim().isEmpty()) continue;
            String lowerTask = task.toLowerCase().trim();
            if (lowerDesc.equals(lowerTask)) {
                return task; // Exact match
            }
        }
        
        // Try substring match - description must contain the full task name
        for (String task : userTasks) {
            if (task == null || task.trim().isEmpty()) continue;
            String lowerTask = task.toLowerCase().trim();
            
            // Check if description contains the full task name (not just a word)
            if (lowerDesc.contains(lowerTask)) {
                // Verify it's not just a partial word match
                int index = lowerDesc.indexOf(lowerTask);
                // Check if it's a word boundary (start of string, space/colon before, or end of string, space/colon after)
                boolean isWordBoundary = (index == 0 || 
                                         lowerDesc.charAt(index - 1) == ' ' || 
                                         lowerDesc.charAt(index - 1) == ':') &&
                                        (index + lowerTask.length() == lowerDesc.length() || 
                                         lowerDesc.charAt(index + lowerTask.length()) == ' ' ||
                                         lowerDesc.charAt(index + lowerTask.length()) == ':');
                if (isWordBoundary) {
                    return task;
                }
            }
        }
        
        // Try reverse - task contains description (for short task names like "Gym", "Run")
        if (lowerDesc.length() <= 20) {
            for (String task : userTasks) {
                if (task == null || task.trim().isEmpty()) continue;
                String lowerTask = task.toLowerCase().trim();
                // For short descriptions (like "Gym", "Run"), be more lenient
                if (lowerTask.contains(lowerDesc) && lowerDesc.length() >= 2) {
                    return task;
                }
            }
        }
        
        // Try fuzzy match - check if any word in description matches task
        String[] descWords = lowerDesc.split("\\s+");
        for (String word : descWords) {
            if (word.length() < 2) continue; // Skip very short words
            for (String task : userTasks) {
                if (task == null || task.trim().isEmpty()) continue;
                String lowerTask = task.toLowerCase().trim();
                // If task name contains this word or vice versa
                if (lowerTask.contains(word) || word.contains(lowerTask)) {
                    // Make sure it's not a common word
                    if (!word.equals("the") && !word.equals("a") && !word.equals("an") && 
                        !word.equals("to") && !word.equals("for") && !word.equals("with")) {
                        return task;
                    }
                }
            }
        }
        
        // NO MATCH FOUND - return null to prevent creating tasks from explanatory text
        Log.d(TAG, "No matching task found for description: '" + description + "'. This is likely explanatory text and will be ignored.");
        return null;
    }
    
    /**
     * Get energy level for a specific hour
     */
    private EnergyLevel getEnergyLevelForHour(List<EnergyPrediction> predictions, int hour) {
        for (EnergyPrediction pred : predictions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pred.getTimestamp());
            if (cal.get(Calendar.HOUR_OF_DAY) == hour) {
                return pred.getPredictedLevel();
            }
        }
        return EnergyLevel.MEDIUM; // Default
    }
    
    /**
     * Extract sleep pattern information from biometric data
     * Returns wake-up times and sleep patterns to help AI align schedule
     */
    private String extractSleepPatternInfo(UserDataProfile profile) {
        if (profile.biometricData == null || profile.biometricData.isEmpty()) {
            return null;
        }
        
        List<Integer> wakeUpHours = new ArrayList<>();
        List<Integer> sleepStartHours = new ArrayList<>();
        
        for (BiometricData data : profile.biometricData) {
            if (data.getSleepMinutes() != null && data.getSleepMinutes() > 0) {
                // Calculate wake-up time from sleep start + duration
                Calendar sleepCal = Calendar.getInstance();
                sleepCal.setTime(data.getTimestamp());
                int sleepStartHour = sleepCal.get(Calendar.HOUR_OF_DAY);
                sleepStartHours.add(sleepStartHour);
                
                // Estimate wake-up time (sleep start + duration in minutes)
                int sleepHours = data.getSleepMinutes() / 60;
                int wakeUpHour = (sleepStartHour + sleepHours) % 24;
                wakeUpHours.add(wakeUpHour);
            }
        }
        
        if (wakeUpHours.isEmpty()) {
            return null;
        }
        
        // Calculate average wake-up time
        double avgWakeUp = wakeUpHours.stream().mapToInt(Integer::intValue).average().orElse(7.0);
        int mostCommonWakeUp = wakeUpHours.stream()
            .collect(java.util.stream.Collectors.groupingBy(i -> i, java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse((int) Math.round(avgWakeUp));
        
        // Calculate average sleep start time
        double avgSleepStart = sleepStartHours.stream().mapToInt(Integer::intValue).average().orElse(23.0);
        
        StringBuilder sleepInfo = new StringBuilder();
        // Format wake-up time in 12-hour format
        String wakeUpPeriod = mostCommonWakeUp < 12 ? "AM" : "PM";
        int wakeUpHour = mostCommonWakeUp > 12 ? mostCommonWakeUp - 12 : (mostCommonWakeUp == 0 ? 12 : mostCommonWakeUp);
        String avgWakeUpPeriod = avgWakeUp < 12 ? "AM" : "PM";
        int avgWakeUpHour = (int) avgWakeUp;
        int avgWakeUpDisplay = avgWakeUpHour > 12 ? avgWakeUpHour - 12 : (avgWakeUpHour == 0 ? 12 : avgWakeUpHour);
        
        // Format sleep start time in 12-hour format
        int sleepStartHour = (int) avgSleepStart;
        String sleepStartPeriod = sleepStartHour < 12 ? "AM" : "PM";
        int sleepStartDisplay = sleepStartHour > 12 ? sleepStartHour - 12 : (sleepStartHour == 0 ? 12 : sleepStartHour);
        
        sleepInfo.append(String.format("Typical wake-up time: %d:00 %s (average: %d:00 %s)\n", 
            wakeUpHour, wakeUpPeriod, avgWakeUpDisplay, avgWakeUpPeriod));
        sleepInfo.append(String.format("Typical sleep start time: %d:00 %s\n", 
            sleepStartDisplay, sleepStartPeriod));
        sleepInfo.append(String.format("Average sleep duration: %.1f hours\n", 
            profile.biometricData.stream()
                .filter(d -> d.getSleepMinutes() != null)
                .mapToInt(d -> d.getSleepMinutes() / 60)
                .average()
                .orElse(7.0)));
        
        return sleepInfo.toString();
    }
    
    /**
     * Determine if an hour is during sleep hours
     * Returns true if the hour falls within the sleep window (11 PM - 6 AM)
     */
    private boolean isHourDuringSleep(int hour, UserDataProfile profile) {
        // Fixed sleep window: 11 PM (23:00) to 6 AM (6:00)
        // This means hours 23, 0, 1, 2, 3, 4, 5 are sleep hours
        boolean isSleep = (hour >= 23 || hour < 6);
        
        if (isSleep) {
            Log.d(TAG, "Hour " + hour + " is during sleep hours (11 PM - 6 AM)");
        }
        
        return isSleep;
    }
    
    public enum ScheduledItemType {
        AI_SCHEDULED_TASK,
        EXISTING_EVENT,
        BREAK,
        OPTIMIZATION_SUGGESTION
    }
    
    public interface SmartScheduleCallback {
        void onSuccess(SmartSchedule schedule);
        void onError(Exception error);
    }
}

