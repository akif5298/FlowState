# Supabase Backend Setup Guide

This guide will help you set up Supabase as the backend for your FlowState app.

## Prerequisites

1. A Supabase account (sign up at [supabase.com](https://supabase.com))
2. Android Studio with the project open
3. Your Supabase project URL and anon key

## Step 1: Create a Supabase Project

1. Go to [supabase.com](https://supabase.com) and sign in
2. Click "New Project"
3. Fill in:
   - Project name: `FlowState` (or your choice)
   - Database password: (choose a strong password)
   - Region: (choose closest to your users)
4. Click "Create new project"
5. Wait for the project to be created (takes a few minutes)

## Step 2: Set Up Database Schema

1. In your Supabase dashboard, go to **SQL Editor**
2. Open the file `supabase/schema.sql` from this project
3. Copy and paste the entire SQL content into the SQL Editor
4. Click "Run" to execute the SQL
5. Verify that the following tables were created:
   - `profiles`
   - `biometric_data`
   - `typing_speed_data`
   - `reaction_time_data`
   - `energy_predictions`

## Step 3: Configure Supabase Client

1. In Supabase dashboard, go to **Settings** → **API**
2. Copy your **Project URL** (looks like: `https://xxxxx.supabase.co`)
3. Copy your **anon/public** key (starts with `eyJ...`)
4. Open `app/src/main/java/com/flowstate/app/supabase/SupabaseClient.kt`
5. Replace the placeholder values:
   ```kotlin
   private const val SUPABASE_URL = "YOUR_SUPABASE_URL"  // Replace with your Project URL
   private const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"  // Replace with your anon key
   ```

## Step 4: Enable Email Authentication

1. In Supabase dashboard, go to **Authentication** → **Providers**
2. Make sure **Email** provider is enabled
3. Configure email templates (optional):
   - Go to **Authentication** → **Email Templates**
   - Customize the templates if desired

## Step 5: Configure Row Level Security (RLS)

The SQL schema already includes RLS policies, but verify:

1. Go to **Authentication** → **Policies**
2. Verify that policies exist for:
   - `profiles`
   - `biometric_data`
   - `typing_speed_data`
   - `reaction_time_data`
   - `energy_predictions`

All policies should allow users to only access their own data.

## Step 6: Test the Integration

1. Sync your Gradle files in Android Studio
2. Build and run the app
3. Try to sign up with a new account
4. Check Supabase dashboard → **Authentication** → **Users** to see if the user was created
5. Try logging in with the new account

## Step 7: Using the Repositories in Your Code

### Example: Saving Biometric Data

```java
// In your activity or fragment
BiometricDataRepository repository = new BiometricDataRepository();
AuthServiceJava authService = new AuthServiceJava(this);

authService.getCurrentUser(new AuthServiceJava.AuthCallback() {
    @Override
    public void onSuccess(UserInfo user) {
        if (user != null) {
            BiometricData data = new BiometricData(
                new Date(),
                75,  // heart rate
                480, // sleep minutes
                0.85, // sleep quality
                36.5  // skin temperature
            );
            
            // Use Kotlin coroutines from Java
            CoroutineScope(Dispatchers.Main).launch {
                val result = repository.upsertBiometricData(user.id, data);
                result.fold(
                    onSuccess = { 
                        // Success
                    },
                    onFailure = { error -> 
                        // Handle error
                    }
                );
            }
        }
    }
    
    @Override
    public void onError(Throwable error) {
        // Handle error
    }
});
```

### Example: Retrieving Data

```kotlin
// In Kotlin code
val repository = BiometricDataRepository()
val userId = SupabaseClient.getCurrentUserId()

if (userId != null) {
    val startDate = Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000) // 7 days ago
    val endDate = Date()
    
    val result = repository.getBiometricData(userId, startDate, endDate)
    result.fold(
        onSuccess = { dataList ->
            // Use the data
        },
        onFailure = { error ->
            // Handle error
        }
    )
}
```

## Troubleshooting

### Build Errors

1. **Kotlin plugin not found**: Make sure you've synced Gradle files
2. **Supabase dependencies not found**: Check that you've added the Supabase repository in `settings.gradle.kts`

### Authentication Errors

1. **"Invalid credentials"**: Verify your Supabase URL and anon key are correct
2. **"Email not confirmed"**: Check Supabase dashboard → **Authentication** → **Settings** → **Email Auth** to see if email confirmation is required

### Database Errors

1. **"Row Level Security policy violation"**: Make sure you're authenticated and the RLS policies are set up correctly
2. **"Table does not exist"**: Make sure you ran the schema.sql file in the SQL Editor

### Network Errors

1. Check your internet connection
2. Verify your Supabase project is active (not paused)
3. Check if your Supabase URL is correct

## Security Best Practices

1. **Never commit your Supabase keys to version control**
   - Use environment variables or a config file that's in `.gitignore`
   - Consider using Android's `BuildConfig` or `local.properties`

2. **Use Row Level Security (RLS)**
   - Already enabled in the schema
   - Never disable RLS in production

3. **Use the anon key, not the service_role key**
   - The anon key is safe for client-side use
   - The service_role key should NEVER be in your app

## Next Steps

1. **Add data synchronization**: Use the repositories to sync data when users perform actions
2. **Implement offline support**: Consider using Room database for local caching
3. **Add real-time updates**: Use Supabase Realtime for live data updates
4. **Set up push notifications**: Use Supabase Edge Functions or Firebase Cloud Messaging

## Additional Resources

- [Supabase Android Documentation](https://supabase.com/docs/reference/android)
- [Supabase Kotlin SDK](https://github.com/supabase/supabase-kt)
- [Supabase Authentication Guide](https://supabase.com/docs/guides/auth)

