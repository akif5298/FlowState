package com.flowstate.app.calendar;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Simple Calendar Service using Android's built-in Calendar API
 * No OAuth required - uses device calendar directly
 */
public class SimpleCalendarService {
    
    private static final String TAG = "SimpleCalendarService";
    private Context context;
    
    public SimpleCalendarService(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Check if calendar read permissions are granted
     */
    public boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if calendar write permissions are granted
     */
    public boolean hasWritePermissions() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if both read and write permissions are granted
     */
    public boolean hasFullPermissions() {
        return hasPermissions() && hasWritePermissions();
    }
    
    /**
     * Get calendar events for today
     */
    public void getTodayEvents(CalendarEventsCallback callback) {
        if (!hasPermissions()) {
            callback.onError(new Exception("Calendar permission not granted"));
            return;
        }
        
        try {
            List<CalendarEvent> events = new ArrayList<>();
            
            // Get start and end of today
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
            
            long startMillis = startOfDay.getTimeInMillis();
            long endMillis = endOfDay.getTimeInMillis();
            
            // Query calendar events
            ContentResolver cr = context.getContentResolver();
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startMillis);
            ContentUris.appendId(builder, endMillis);
            
            String[] projection = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_ID
            };
            
            String selection = CalendarContract.Instances.BEGIN + " >= ? AND " + 
                             CalendarContract.Instances.END + " <= ?";
            String[] selectionArgs = {String.valueOf(startMillis), String.valueOf(endMillis)};
            
            Cursor cursor = cr.query(builder.build(), projection, selection, selectionArgs,
                    CalendarContract.Instances.BEGIN + " ASC");
            
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        CalendarEvent event = new CalendarEvent();
                        event.title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE));
                        event.startTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN));
                        event.endTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END));
                        event.description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION));
                        event.id = String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)));
                        
                        if (event.title != null && !event.title.isEmpty()) {
                            events.add(event);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            
            Log.d(TAG, "Found " + events.size() + " calendar events for today");
            callback.onSuccess(events);
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching calendar events", e);
            callback.onError(e);
        }
    }
    
    /**
     * Get calendar events for a date range
     */
    public void getEvents(long startTimeMillis, long endTimeMillis, CalendarEventsCallback callback) {
        if (!hasPermissions()) {
            callback.onError(new Exception("Calendar permission not granted"));
            return;
        }
        
        try {
            List<CalendarEvent> events = new ArrayList<>();
            
            ContentResolver cr = context.getContentResolver();
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startTimeMillis);
            ContentUris.appendId(builder, endTimeMillis);
            
            String[] projection = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_ID
            };
            
            Cursor cursor = cr.query(builder.build(), projection, null, null,
                    CalendarContract.Instances.BEGIN + " ASC");
            
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        CalendarEvent event = new CalendarEvent();
                        event.title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE));
                        event.startTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN));
                        event.endTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END));
                        event.description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION));
                        event.id = String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)));
                        
                        if (event.title != null && !event.title.isEmpty()) {
                            events.add(event);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            
            Log.d(TAG, "Found " + events.size() + " calendar events");
            callback.onSuccess(events);
            
        } catch (Exception e) {
            Log.e(TAG, "Error fetching calendar events", e);
            callback.onError(e);
        }
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
     * Create a calendar event
     */
    public void createCalendarEvent(String title, String description, long startTime, long endTime, 
                                   CreateEventCallback callback) {
        if (!hasWritePermissions()) {
            callback.onError(new Exception("Calendar write permission not granted"));
            return;
        }
        
        try {
            ContentResolver cr = context.getContentResolver();
            
            // Get the default calendar ID
            String[] projection = {CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME};
            String selection = CalendarContract.Calendars.VISIBLE + " = 1";
            Cursor cursor = cr.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null);
            
            long calendarId = -1;
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        int idIndex = cursor.getColumnIndex(CalendarContract.Calendars._ID);
                        calendarId = cursor.getLong(idIndex);
                    }
                } finally {
                    cursor.close();
                }
            }
            
            if (calendarId == -1) {
                callback.onError(new Exception("No calendar found"));
                return;
            }
            
            // Create the event
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            values.put(CalendarContract.Events.TITLE, title);
            values.put(CalendarContract.Events.DESCRIPTION, description != null ? description : "");
            values.put(CalendarContract.Events.DTSTART, startTime);
            values.put(CalendarContract.Events.DTEND, endTime);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().getID());
            values.put(CalendarContract.Events.HAS_ALARM, 0); // No alarm by default
            
            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
            
            if (uri != null) {
                long eventId = Long.parseLong(uri.getLastPathSegment());
                Log.d(TAG, "Created calendar event: " + title + " (ID: " + eventId + ")");
                
                CalendarEvent event = new CalendarEvent();
                event.id = String.valueOf(eventId);
                event.title = title;
                event.description = description;
                event.startTime = startTime;
                event.endTime = endTime;
                
                callback.onSuccess(event);
            } else {
                callback.onError(new Exception("Failed to create calendar event"));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating calendar event", e);
            callback.onError(e);
        }
    }
    
    /**
     * Find calendar events by title (for today)
     * Used to check if an event already exists before creating a new one
     */
    public void findEventsByTitle(String title, CalendarEventsCallback callback) {
        if (!hasPermissions()) {
            callback.onError(new Exception("Calendar permission not granted"));
            return;
        }
        
        try {
            List<CalendarEvent> events = new ArrayList<>();
            
            // Get start and end of today
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
            
            long startMillis = startOfDay.getTimeInMillis();
            long endMillis = endOfDay.getTimeInMillis();
            
            ContentResolver cr = context.getContentResolver();
            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, startMillis);
            ContentUris.appendId(builder, endMillis);
            
            String[] projection = {
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_ID
            };
            
            String selection = CalendarContract.Instances.TITLE + " = ?";
            String[] selectionArgs = {title};
            
            Cursor cursor = cr.query(builder.build(), projection, selection, selectionArgs,
                    CalendarContract.Instances.BEGIN + " ASC");
            
            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        CalendarEvent event = new CalendarEvent();
                        event.title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE));
                        event.startTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN));
                        event.endTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.END));
                        event.description = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION));
                        event.id = String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)));
                        
                        if (event.title != null && event.title.equals(title)) {
                            events.add(event);
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            
            Log.d(TAG, "Found " + events.size() + " events with title: " + title);
            callback.onSuccess(events);
            
        } catch (Exception e) {
            Log.e(TAG, "Error finding events by title", e);
            callback.onError(e);
        }
    }
    
    /**
     * Delete a calendar event by ID
     */
    public void deleteEvent(String eventId, DeleteEventCallback callback) {
        if (!hasWritePermissions()) {
            callback.onError(new Exception("Calendar write permission not granted"));
            return;
        }
        
        try {
            ContentResolver cr = context.getContentResolver();
            Uri deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, Long.parseLong(eventId));
            int rowsDeleted = cr.delete(deleteUri, null, null);
            
            if (rowsDeleted > 0) {
                Log.d(TAG, "Deleted calendar event: " + eventId);
                callback.onSuccess();
            } else {
                Log.w(TAG, "No event found to delete with ID: " + eventId);
                callback.onError(new Exception("Event not found"));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting calendar event", e);
            callback.onError(e);
        }
    }
    
    /**
     * Delete all events with a given title (for today)
     * Used to remove duplicate events before creating a new one with AI-assigned time
     */
    public void deleteEventsByTitle(String title, DeleteEventCallback callback) {
        if (!hasWritePermissions()) {
            callback.onError(new Exception("Calendar write permission not granted"));
            return;
        }
        
        findEventsByTitle(title, new CalendarEventsCallback() {
            @Override
            public void onSuccess(List<CalendarEvent> events) {
                if (events.isEmpty()) {
                    callback.onSuccess();
                    return;
                }
                
                int deletedCount = 0;
                for (CalendarEvent event : events) {
                    // Only delete FlowState-created events (to avoid deleting user's existing events)
                    if (event.description != null && 
                        event.description.contains("Created by FlowState AI Schedule")) {
                        deleteEvent(event.id, new DeleteEventCallback() {
                            @Override
                            public void onSuccess() {
                                // Individual delete succeeded
                            }
                            
                            @Override
                            public void onError(Exception error) {
                                Log.w(TAG, "Failed to delete event: " + event.id, error);
                            }
                        });
                        deletedCount++;
                    }
                }
                
                Log.d(TAG, "Deleted " + deletedCount + " events with title: " + title);
                callback.onSuccess();
            }
            
            @Override
            public void onError(Exception error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Callback interface for calendar events
     */
    public interface CalendarEventsCallback {
        void onSuccess(List<CalendarEvent> events);
        void onError(Exception error);
    }
    
    /**
     * Callback interface for creating calendar events
     */
    public interface CreateEventCallback {
        void onSuccess(CalendarEvent event);
        void onError(Exception error);
    }
    
    /**
     * Callback interface for deleting calendar events
     */
    public interface DeleteEventCallback {
        void onSuccess();
        void onError(Exception error);
    }
}

