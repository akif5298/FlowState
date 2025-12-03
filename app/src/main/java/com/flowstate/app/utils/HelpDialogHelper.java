package com.flowstate.app.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import com.flowstate.app.R;

/**
 * Helper class to show help dialogs with author information and instructions
 * Required for CP470 project compliance
 */
public class HelpDialogHelper {
    
    private static final String APP_VERSION = "1.0.0";
    private static final String[] AUTHORS = {
        "Bibek Chugh",
        "Kush Jain", 
        "Akif Rahman",
        "Yusuf Muzaffar Iqbal",
        "Tharun Indrakumar"
    };
    
    /**
     * Show help dialog with author information and activity-specific instructions
     */
    public static void showHelpDialog(Context context, String activityName, String instructions) {
        StringBuilder authorsText = new StringBuilder();
        for (int i = 0; i < AUTHORS.length; i++) {
            authorsText.append(AUTHORS[i]);
            if (i < AUTHORS.length - 1) {
                authorsText.append("\n");
            }
        }
        
        String helpText = String.format(
            "<b>FlowState - %s</b><br><br>" +
            "<b>Version:</b> %s<br><br>" +
            "<b>Authors:</b><br>%s<br><br>" +
            "<b>Instructions:</b><br>%s",
            activityName,
            APP_VERSION,
            authorsText.toString(),
            instructions
        );
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Help & Information");
        builder.setMessage(Html.fromHtml(helpText, Html.FROM_HTML_MODE_LEGACY));
        builder.setPositiveButton("OK", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Make links clickable
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
    
    /**
     * Get default instructions for an activity
     */
    public static String getDefaultInstructions(String activityName) {
        if (activityName.contains("Dashboard")) {
            return "View your energy predictions and navigate to different sections. " +
                   "Use the bottom navigation to access Data Logs, Schedule, Insights, and Settings.";
        } else if (activityName.contains("Data Logs")) {
            return "View your biometric data, typing speed, and reaction time records. " +
                   "Tap on any item to see detailed information. Data is synced from Google Fit and stored in Supabase.";
        } else if (activityName.contains("Schedule")) {
            return "Add tasks with energy levels, then generate an AI-optimized schedule. " +
                   "Tasks are matched to your predicted energy levels for optimal productivity.";
        } else if (activityName.contains("Settings")) {
            return "Configure app settings including Google Fit integration, calendar sync, and notifications. " +
                   "Toggle switches to enable/disable features.";
        } else if (activityName.contains("Energy Prediction")) {
            return "Generate and view your energy level predictions for today. " +
                   "Tap 'Generate Today's Predictions' to create ML-based energy forecasts.";
        } else if (activityName.contains("Typing")) {
            return "Test your typing speed and accuracy. Type the displayed text as quickly and accurately as possible.";
        } else if (activityName.contains("Reaction")) {
            return "Test your reaction time. Tap the screen when the color changes.";
        } else {
            return "Use this activity to manage your energy predictions and productivity. " +
                   "Navigate using the buttons and menus provided.";
        }
    }
}

