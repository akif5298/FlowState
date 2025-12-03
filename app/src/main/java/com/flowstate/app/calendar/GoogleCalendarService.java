package com.flowstate.app.calendar;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for interacting with Google Calendar API
 */
public class GoogleCalendarService {
    
    private static final String TAG = "GoogleCalendarService";
    private static final String PREF_ACCOUNT_NAME = "google_calendar_account";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "FlowState";
    
    private Context context;
    private Calendar calendarService;
    private ExecutorService executorService;
    private SharedPreferences prefs;
    
    public GoogleCalendarService(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }
    
    /**
     * Initialize Google Calendar service with credentials
     */
    public void initialize(Credential credential) {
        if (credential == null) {
            Log.e(TAG, "Credential is null");
            return;
        }
        
        calendarService = new Calendar.Builder(
            new NetHttpTransport(),
            JSON_FACTORY,
            credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }
    
    /**
     * Initialize using GoogleCalendarAuthManager
     * Note: This creates a placeholder credential. For full functionality,
     * you'll need to exchange the server auth code for access tokens.
     */
    public void initialize(GoogleCalendarAuthManager authManager) {
        if (authManager == null || !authManager.isSignedIn()) {
            Log.e(TAG, "Auth manager is null or not signed in");
            return;
        }
        
        // For now, we'll mark the service as ready
        // In production, you would:
        // 1. Get server auth code from authManager.getServerAuthCode()
        // 2. Exchange it for access/refresh tokens on your backend
        // 3. Create a proper credential with those tokens
        // 4. Initialize the calendar service with that credential
        
        Log.d(TAG, "Calendar service initialized (placeholder - token exchange needed for full functionality)");
        
        // Create a basic credential placeholder
        Credential credential = authManager.getCredential();
        if (credential != null) {
            initialize(credential);
        } else {
            Log.e(TAG, "Failed to get credential from auth manager");
        }
    }
    
    /**
     * Get calendar events for a specific date range
     */
    public void getEvents(long startTime, long endTime, CalendarEventsCallback callback) {
        if (calendarService == null) {
            callback.onError(new Exception("Calendar service not initialized"));
            return;
        }
        
        executorService.execute(() -> {
            try {
                DateTime start = new DateTime(startTime);
                DateTime end = new DateTime(endTime);
                
                Events events = calendarService.events()
                    .list("primary")
                    .setTimeMin(start)
                    .setTimeMax(end)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
                
                List<CalendarEvent> calendarEvents = new ArrayList<>();
                List<Event> items = events.getItems();
                
                if (items != null) {
                    for (Event event : items) {
                        CalendarEvent calEvent = new CalendarEvent();
                        calEvent.id = event.getId();
                        calEvent.title = event.getSummary();
                        calEvent.description = event.getDescription();
                        
                        if (event.getStart() != null) {
                            if (event.getStart().getDateTime() != null) {
                                calEvent.startTime = event.getStart().getDateTime().getValue();
                            } else if (event.getStart().getDate() != null) {
                                calEvent.startTime = event.getStart().getDate().getValue();
                            }
                        }
                        
                        if (event.getEnd() != null) {
                            if (event.getEnd().getDateTime() != null) {
                                calEvent.endTime = event.getEnd().getDateTime().getValue();
                            } else if (event.getEnd().getDate() != null) {
                                calEvent.endTime = event.getEnd().getDate().getValue();
                            }
                        }
                        
                        calendarEvents.add(calEvent);
                    }
                }
                
                callback.onSuccess(calendarEvents);
            } catch (IOException e) {
                Log.e(TAG, "Error fetching calendar events", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Get events for today
     */
    public void getTodayEvents(CalendarEventsCallback callback) {
        long now = System.currentTimeMillis();
        long endOfDay = now + (24 * 60 * 60 * 1000); // 24 hours from now
        getEvents(now, endOfDay, callback);
    }
    
    /**
     * Check if Google Calendar is enabled
     */
    public boolean isEnabled() {
        return prefs.getBoolean("google_calendar_enabled", false);
    }
    
    /**
     * Calendar event model
     */
    public static class CalendarEvent {
        public String id;
        public String title;
        public String description;
        public long startTime;
        public long endTime;
    }
    
    /**
     * Callback interface for calendar events
     */
    public interface CalendarEventsCallback {
        void onSuccess(List<CalendarEvent> events);
        void onError(Exception error);
    }
}

