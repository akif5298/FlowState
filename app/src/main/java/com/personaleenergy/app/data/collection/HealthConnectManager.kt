package com.personaleenergy.app.data.collection

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.permission.HealthPermission
import com.flowstate.app.data.models.BiometricData as BiometricDataModel
import kotlinx.coroutines.*
import java.time.Instant
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Health Connect Manager for syncing health data
 * Health Connect is Android's unified health data platform that aggregates
 * data from various sources including Google Fit, Samsung Health, etc.
 */
class HealthConnectManager(private val context: Context) {
    companion object {
        private const val TAG = "HealthConnectManager"
        private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
        private const val PREFS_NAME = "health_connect_prefs"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
    }
    
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Lazy initialization - will try to get client when first accessed
    private val healthConnectClient: HealthConnectClient? by lazy {
        try {
            HealthConnectClient.getOrCreate(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Health Connect client", e)
            null
        }
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Get or create Health Connect client
     * This will try to create a fresh client if the lazy one failed
     */
    private fun getOrCreateClient(): HealthConnectClient? {
        // Try the lazy property first
        val client = healthConnectClient
        if (client != null) {
            return client
        }
        
        // If lazy initialization failed (e.g., Health Connect wasn't installed at startup),
        // try creating it fresh now
        return try {
            HealthConnectClient.getOrCreate(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Health Connect client", e)
            null
        }
    }
    
    /**
     * Check if Health Connect is available on the device
     * Health Connect can be available as a system service (integrated into Settings)
     * or as an installed app, so we check by trying to create the client directly
     * 
     * Note: getSdkStatus() may not be available in all SDK versions, so we use
     * client creation as the primary check method
     */
    fun isAvailable(): Boolean {
        // Try to create the client directly - this is the most reliable way to check availability
        // HealthConnectClient.getOrCreate() will throw an exception if Health Connect is not available
        return try {
            val client = HealthConnectClient.getOrCreate(appContext)
            Log.d(TAG, "Health Connect is available (client created successfully)")
            
            // Verify the client is actually functional by checking if we can get the permission controller
            try {
                val controller = client.permissionController
                Log.d(TAG, "Health Connect permission controller accessible")
            } catch (e: Exception) {
                Log.w(TAG, "Health Connect client exists but permission controller not accessible", e)
            }
            
            true
        } catch (e: Exception) {
            // Health Connect is not available
            Log.d(TAG, "Health Connect not available: ${e.message}")
            
            // Also check if package exists for better error reporting
            try {
                appContext.packageManager.getPackageInfo(HEALTH_CONNECT_PACKAGE, 0)
                Log.d(TAG, "Health Connect package exists but client creation failed - may need to enable in Settings")
                // Package exists but client creation failed - might still be usable
                return true
            } catch (e2: PackageManager.NameNotFoundException) {
                Log.d(TAG, "Health Connect package not found")
                return false
            }
        }
    }
    
    /**
     * Get the Health Connect client instance
     * This will try to create a fresh client if needed
     */
    fun getClient(): HealthConnectClient? {
        return getOrCreateClient()
    }
    
    /**
     * Request Health Connect permissions
     * Note: Permission requests must be handled via Activity result launcher in the Activity
     */
    suspend fun requestPermissions(permissionController: PermissionController): Set<String> {
        val client = getOrCreateClient()
        if (client == null) {
            Log.e(TAG, "Health Connect client is null, cannot request permissions")
            return emptySet()
        }
        
        val permissions = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
        
        return try {
            // Note: requestPermissions is handled via Activity result launcher
            // This is a placeholder - actual permission request must be done in Activity
            Log.w(TAG, "Permission requests must be handled via Activity result launcher")
            emptySet()
        } catch (e: Exception) {
            Log.e(TAG, "Exception requesting Health Connect permissions", e)
            emptySet()
        }
    }
    
    /**
     * Check if required permissions are granted
     */
    suspend fun hasPermissions(): Boolean {
        val client = getOrCreateClient()
        if (client == null) {
            return false
        }
        
        val permissions = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
        
        return try {
            val grantedPermissions = client
                .permissionController
                .getGrantedPermissions()
            // Check if all required permissions are in the granted set
            permissions.all { it in grantedPermissions }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check Health Connect permissions", e)
            false
        }
    }
    
    /**
     * Read combined biometric data (heart rate and sleep) from Health Connect
     * This syncs data from Google Fit and other health apps via Health Connect
     * 
     * @param hours Number of hours of data to read, or 0 to read since last sync
     */
    suspend fun readCombinedBiometricData(hours: Int): List<BiometricDataModel> {
        val client = getOrCreateClient()
        if (client == null) {
            throw Exception("Health Connect is not available on this device")
        }
        
        val endTime = Instant.now()
        val startTime = if (hours == 0) {
            // Read since last sync
            val lastSync = getLastSync()
            if (lastSync > 0) {
                Instant.ofEpochMilli(lastSync)
            } else {
                // First sync - get last 7 days
                endTime.minusSeconds(7 * 24 * 3600L)
            }
        } else {
            endTime.minusSeconds(hours * 3600L)
        }
        
        Log.d(TAG, "Reading Health Connect data from $startTime to $endTime")
        
        // Read heart rate and sleep data in parallel
        val heartRateData = try {
            readHeartRateData(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read heart rate data", e)
            emptyList()
        }
        
        val sleepData = try {
            readSleepData(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read sleep data", e)
            emptyList()
        }
        
        // Combine the data
        return combineBiometricData(heartRateData, sleepData)
    }
    
    /**
     * Read new biometric data since last sync
     * This is used for incremental syncing on app open
     */
    suspend fun readNewBiometricDataSinceLastSync(): List<BiometricDataModel> {
        val lastSyncTimestamp = getLastSyncTimestamp()
        val endTime = Instant.now()
        
        // If no previous sync, read last 7 days
        val startTime = if (lastSyncTimestamp > 0) {
            Instant.ofEpochMilli(lastSyncTimestamp)
        } else {
            endTime.minusSeconds(7 * 24 * 3600L) // 7 days
        }
        
        Log.d(TAG, "Reading new Health Connect data from $startTime to $endTime (last sync: $lastSyncTimestamp)")
        
        val client = getOrCreateClient()
        if (client == null) {
            throw Exception("Health Connect is not available on this device")
        }
        
        // Read heart rate and sleep data in parallel
        val heartRateData = try {
            readHeartRateData(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read heart rate data", e)
            emptyList()
        }
        
        val sleepData = try {
            readSleepData(startTime, endTime)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read sleep data", e)
            emptyList()
        }
        
        // Combine the data
        val combinedData = combineBiometricData(heartRateData, sleepData)
        
        // Update last sync timestamp if we got data
        if (combinedData.isNotEmpty()) {
            updateLastSyncTimestamp(System.currentTimeMillis())
        }
        
        return combinedData
    }
    
    /**
     * Get the last sync timestamp
     */
    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0)
    }
    
    /**
     * Update the last sync timestamp
     */
    fun updateLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
        Log.d(TAG, "Updated last sync timestamp to: $timestamp")
    }
    
    /**
     * Java-compatible wrapper for readNewBiometricDataSinceLastSync
     */
    fun readNewBiometricDataSinceLastSync(callback: BiometricCallback) {
        coroutineScope.launch {
            try {
                val data = readNewBiometricDataSinceLastSync()
                mainHandler.post {
                    callback.onSuccess(data)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e)
                }
            }
        }
    }
    
    /**
     * Read heart rate data from Health Connect
     * Processes all samples in each record, as records may contain multiple samples
     * with different timestamps and heart rate values
     */
    private suspend fun readHeartRateData(
        startTime: Instant,
        endTime: Instant
    ): List<BiometricDataModel> {
        val client = getOrCreateClient() ?: throw Exception("Health Connect is not available")
        val timeRange = TimeRangeFilter.between(startTime, endTime)
        
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = timeRange,
            pageSize = 1000
        )
        
        val response = client.readRecords(request)
        
        val dataList = mutableListOf<BiometricDataModel>()
        for (record in response.records) {
            // Process ALL samples in the record, not just the first one
            // Each sample represents a heart rate measurement at a specific time
            for (sample in record.samples) {
                val timestamp = Date.from(sample.time)
                val bpm = sample.beatsPerMinute.toInt()
                
                val biometricData = BiometricDataModel(timestamp)
                biometricData.setHeartRate(bpm)
                dataList.add(biometricData)
            }
        }
        
        Log.d(TAG, "Read ${dataList.size} heart rate samples from ${response.records.size} records in Health Connect")
        return dataList
    }
    
    /**
     * Read sleep data from Health Connect
     * Processes sleep sessions and their stages if available
     * Sleep quality is calculated based on duration and stage distribution
     */
    private suspend fun readSleepData(
        startTime: Instant,
        endTime: Instant
    ): List<BiometricDataModel> {
        val client = getOrCreateClient() ?: throw Exception("Health Connect is not available")
        val timeRange = TimeRangeFilter.between(startTime, endTime)
        
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = timeRange,
            pageSize = 100
        )
        
        val response = client.readRecords(request)
        
        val dataList = mutableListOf<BiometricDataModel>()
        for (record in response.records) {
            val timestamp = Date.from(record.startTime)
            val durationSeconds = java.time.Duration.between(record.startTime, record.endTime).seconds
            val durationMinutes = (durationSeconds / 60).toInt()
            
            // Process sleep stages if available
            var deepSleepMinutes = 0
            var lightSleepMinutes = 0
            var remSleepMinutes = 0
            var awakeMinutes = 0
            
            if (record.stages.isNotEmpty()) {
                // Process all stages in the sleep session
                for (stage in record.stages) {
                    val stageDurationSeconds = java.time.Duration.between(stage.startTime, stage.endTime).seconds
                    val stageDurationMinutes = (stageDurationSeconds / 60).toInt()
                    
                    // Categorize by stage type
                    // Access the stage type and convert to string for comparison
                    try {
                        val stageType = stage.stage
                        val stageTypeName = stageType.toString().uppercase()
                        
                        when {
                            stageTypeName.contains("DEEP") -> {
                                deepSleepMinutes += stageDurationMinutes
                            }
                            stageTypeName.contains("REM") -> {
                                remSleepMinutes += stageDurationMinutes
                            }
                            stageTypeName.contains("AWAKE") -> {
                                awakeMinutes += stageDurationMinutes
                            }
                            stageTypeName.contains("LIGHT") -> {
                                lightSleepMinutes += stageDurationMinutes
                            }
                            else -> {
                                // Unknown stage type, count as light sleep
                                Log.d(TAG, "Unknown sleep stage type: $stageTypeName")
                                lightSleepMinutes += stageDurationMinutes
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback: if we can't access stage type, log and count as light sleep
                        Log.w(TAG, "Could not determine sleep stage type: ${e.message}")
                        lightSleepMinutes += stageDurationMinutes
                    }
                }
                
                Log.d(TAG, "Sleep session stages - Deep: ${deepSleepMinutes}min, Light: ${lightSleepMinutes}min, REM: ${remSleepMinutes}min, Awake: ${awakeMinutes}min")
            }
            
            // Calculate sleep quality based on duration and stage distribution
            val quality = calculateSleepQuality(
                durationMinutes = durationMinutes,
                deepSleepMinutes = deepSleepMinutes,
                lightSleepMinutes = lightSleepMinutes,
                remSleepMinutes = remSleepMinutes,
                awakeMinutes = awakeMinutes,
                hasStages = record.stages.isNotEmpty()
            )
            
            val biometricData = BiometricDataModel(timestamp)
            biometricData.setSleepMinutes(durationMinutes)
            biometricData.setSleepQuality(quality)
            dataList.add(biometricData)
        }
        
        Log.d(TAG, "Read ${dataList.size} sleep records from Health Connect")
        return dataList
    }
    
    /**
     * Calculate sleep quality score (0.0 to 1.0) based on duration and stage distribution
     * 
     * @param durationMinutes Total sleep duration in minutes
     * @param deepSleepMinutes Deep sleep duration in minutes
     * @param lightSleepMinutes Light sleep duration in minutes
     * @param remSleepMinutes REM sleep duration in minutes
     * @param awakeMinutes Awake time during sleep session in minutes
     * @param hasStages Whether stage data is available
     * @return Sleep quality score from 0.0 to 1.0
     */
    private fun calculateSleepQuality(
        durationMinutes: Int,
        deepSleepMinutes: Int,
        lightSleepMinutes: Int,
        remSleepMinutes: Int,
        awakeMinutes: Int,
        hasStages: Boolean
    ): Double {
        if (!hasStages) {
            // Fallback to duration-based quality if no stage data
            return when {
                durationMinutes < 360 -> 0.3  // Less than 6 hours
                durationMinutes < 480 -> 0.6  // 6-8 hours
                durationMinutes < 600 -> 0.9  // 8-10 hours
                else -> 0.7  // More than 10 hours
            }
        }
        
        // Calculate quality based on stage distribution
        val totalSleepMinutes = deepSleepMinutes + lightSleepMinutes + remSleepMinutes
        if (totalSleepMinutes == 0) {
            return 0.1  // No actual sleep
        }
        
        // Ideal sleep distribution (for 8 hours):
        // Deep: 15-20% (72-96 min)
        // REM: 20-25% (96-120 min)
        // Light: 50-60% (240-288 min)
        // Awake: <5% (<24 min)
        
        val deepSleepPercent = (deepSleepMinutes.toDouble() / totalSleepMinutes) * 100
        val remSleepPercent = (remSleepMinutes.toDouble() / totalSleepMinutes) * 100
        val awakePercent = (awakeMinutes.toDouble() / durationMinutes) * 100
        
        var quality = 0.5  // Base score
        
        // Duration factor (optimal: 7-9 hours)
        when {
            durationMinutes in 420..540 -> quality += 0.2  // 7-9 hours: excellent
            durationMinutes in 360..600 -> quality += 0.1  // 6-10 hours: good
            durationMinutes < 360 -> quality -= 0.2  // Less than 6 hours: poor
            else -> quality -= 0.1  // More than 10 hours: may indicate issues
        }
        
        // Deep sleep factor (optimal: 15-20%)
        when {
            deepSleepPercent in 15.0..20.0 -> quality += 0.15
            deepSleepPercent in 10.0..25.0 -> quality += 0.1
            deepSleepPercent < 10.0 -> quality -= 0.1
            else -> quality -= 0.05
        }
        
        // REM sleep factor (optimal: 20-25%)
        when {
            remSleepPercent in 20.0..25.0 -> quality += 0.1
            remSleepPercent in 15.0..30.0 -> quality += 0.05
            remSleepPercent < 15.0 -> quality -= 0.1
            else -> quality -= 0.05
        }
        
        // Awake time factor (should be minimal)
        when {
            awakePercent < 5.0 -> quality += 0.05
            awakePercent < 10.0 -> quality += 0.0
            awakePercent < 20.0 -> quality -= 0.1
            else -> quality -= 0.2
        }
        
        // Clamp to 0.0-1.0 range
        return quality.coerceIn(0.0, 1.0)
    }
    
    /**
     * Combine heart rate and sleep data by timestamp
     */
    private fun combineBiometricData(
        heartRateData: List<BiometricDataModel>,
        sleepData: List<BiometricDataModel>
    ): List<BiometricDataModel> {
        val combined = mutableMapOf<Long, BiometricDataModel>()
        
        // Add heart rate data
        for (biometricData in heartRateData) {
            val timeKey = biometricData.getTimestamp().time
            val existing = combined.getOrDefault(timeKey, BiometricDataModel(biometricData.getTimestamp()))
            if (biometricData.getHeartRate() != null) {
                existing.setHeartRate(biometricData.getHeartRate())
            }
            combined[timeKey] = existing
        }
        
        // Add sleep data
        for (biometricData in sleepData) {
            val timeKey = biometricData.getTimestamp().time
            val existing = combined.getOrDefault(timeKey, BiometricDataModel(biometricData.getTimestamp()))
            if (biometricData.getSleepMinutes() != null) {
                existing.setSleepMinutes(biometricData.getSleepMinutes())
            }
            if (biometricData.getSleepQuality() != null) {
                existing.setSleepQuality(biometricData.getSleepQuality())
            }
            combined[timeKey] = existing
        }
        
        return combined.values.sortedBy { it.getTimestamp() }
    }
    
    /**
     * Open Health Connect app in Play Store if not installed
     */
    fun openHealthConnectInPlayStore() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
                setPackage("com.android.vending")
            }
            appContext.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to web browser
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")
            }
            appContext.startActivity(intent)
        }
    }
    
    /**
     * Open Health Connect app settings directly to permissions
     * This bypasses the permission dialog and opens Health Connect directly
     */
    fun openHealthConnectSettings() {
        try {
            // Method 1: Try to open Health Connect's app permissions screen directly
            // Using the app's package name to open our app's permission page
            val appPackageName = appContext.packageName
            val permissionsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$appPackageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Try opening our app's settings first, then user can navigate to Health Connect
            try {
                appContext.startActivity(permissionsIntent)
                Log.d(TAG, "Opened app settings - user can navigate to Health Connect from there")
                return
            } catch (e: Exception) {
                Log.d(TAG, "Could not open app settings", e)
            }
            
            // Method 2: Try Health Connect's permission rationale intent
            val rationaleIntent = Intent("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE").apply {
                `package` = HEALTH_CONNECT_PACKAGE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            try {
                appContext.startActivity(rationaleIntent)
                Log.d(TAG, "Opened Health Connect permissions rationale screen")
                return
            } catch (e: android.content.ActivityNotFoundException) {
                Log.d(TAG, "Could not open permissions rationale, trying main app")
            }
            
            // Method 3: Try opening Health Connect app directly
            val intent = appContext.packageManager.getLaunchIntentForPackage(HEALTH_CONNECT_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
                Log.d(TAG, "Opened Health Connect app")
            } else {
                // Last fallback: try to open via intent action
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    `package` = HEALTH_CONNECT_PACKAGE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(fallbackIntent)
                Log.d(TAG, "Opened Health Connect via fallback intent")
            }
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e(TAG, "Health Connect app not found, trying Play Store", e)
            openHealthConnectInPlayStore()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Health Connect settings", e)
            e.printStackTrace()
            openHealthConnectInPlayStore()
        }
    }
    
    /**
     * Directly open Health Connect to request permissions for this app
     * This is an alternative approach that opens Health Connect directly
     * Java-compatible version
     */
    fun openHealthConnectForPermissions(activity: Activity) {
        try {
            // Get our app's package name
            val appPackageName = activity.packageName
            Log.d(TAG, "Opening Health Connect for app: $appPackageName")
            
            // Try to open Health Connect app directly
            // The user can then navigate to App permissions manually
            val launchIntent = activity.packageManager.getLaunchIntentForPackage(HEALTH_CONNECT_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(launchIntent)
                Log.d(TAG, "Opened Health Connect app")
                return
            }
            
            // If Health Connect app not found, try Play Store
            throw ActivityNotFoundException("Health Connect app not found")
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e(TAG, "Health Connect not found", e)
            openHealthConnectInPlayStore()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Health Connect", e)
            openHealthConnectInPlayStore()
        }
    }
    
    /**
     * Java-compatible callback interface for async operations
     */
    interface BiometricCallback {
        @JvmSuppressWildcards
        fun onSuccess(data: List<BiometricDataModel>)
        fun onError(e: Exception)
    }
    
    /**
     * Get the last sync timestamp
     */
    fun getLastSync(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0)
    }
    
    /**
     * Update the last sync timestamp
     */
    fun updateLastSync(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
        Log.d(TAG, "Updated last sync timestamp to: $timestamp")
    }
    
    /**
     * Sync new data since last sync (for automatic sync on app open)
     * This reads only new data and updates the last sync timestamp
     */
    fun syncNewDataSinceLastSync(callback: BiometricCallback) {
        val lastSync = getLastSync()
        Log.d(TAG, "Syncing new data since last sync (last sync: ${if (lastSync > 0) java.util.Date(lastSync) else "never"})")
        // Pass 0 to readCombinedBiometricData to read since last sync
        readCombinedBiometricData(0, callback)
    }
    
    /**
     * Java-compatible wrapper for readCombinedBiometricData using callbacks
     */
    fun readCombinedBiometricData(hours: Int, callback: BiometricCallback) {
        coroutineScope.launch {
            try {
                val data = readCombinedBiometricData(hours)
                // Update last sync timestamp if we got data
                if (data.isNotEmpty()) {
                    updateLastSync(System.currentTimeMillis())
                }
                mainHandler.post {
                    callback.onSuccess(data)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onError(e)
                }
            }
        }
    }
    
    /**
     * Java-compatible wrapper for hasPermissions using CompletableFuture
     */
    fun hasPermissionsJava(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        coroutineScope.launch {
            try {
                val hasPermissions = hasPermissions()
                future.complete(hasPermissions)
            } catch (e: Exception) {
                future.complete(false)
            }
        }
        return future
    }
    
    /**
     * Java-compatible callback interface for permission requests
     */
    interface PermissionCallback {
        fun onResult(grantedPermissions: Set<String>)
    }
    
    /**
     * Get required permissions as Set<String> for Java compatibility
     */
    fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
    }
    
    /**
     * Get permission controller for creating result contract
     */
    fun getPermissionController(): PermissionController? {
        return getOrCreateClient()?.permissionController
    }
    
    /**
     * Java-compatible method to create permission launcher
     * This wraps the Kotlin PermissionController.createRequestPermissionResultContract()
     */
    fun createPermissionLauncher(
        activity: androidx.appcompat.app.AppCompatActivity,
        callback: (Set<String>) -> Unit
    ): androidx.activity.result.ActivityResultLauncher<Set<String>> {
        val contract = PermissionController.createRequestPermissionResultContract()
        return activity.registerForActivityResult(contract) { grantedPermissions ->
            callback(grantedPermissions)
        }
    }
    
    /**
     * Java-compatible version using PermissionCallback interface
     */
    fun createPermissionLauncherJava(
        activity: androidx.appcompat.app.AppCompatActivity,
        callback: PermissionCallback
    ): androidx.activity.result.ActivityResultLauncher<Set<String>>? {
        return try {
            // Ensure Health Connect client is available
            val client = getOrCreateClient()
            if (client == null) {
                Log.e(TAG, "Cannot create permission launcher: Health Connect client is null")
                return null
            }
            
            Log.d(TAG, "Creating permission contract with Health Connect client")
            // Ensure client is properly initialized
            val permissionController = client.permissionController
            Log.d(TAG, "Got permission controller from client: $permissionController")
            
            // Get the required permissions to verify they're correct
            val requiredPermissions = getRequiredPermissions()
            Log.d(TAG, "Required permissions: $requiredPermissions")
            
            // Use the static method - this is the correct approach for Health Connect SDK
            val contract = PermissionController.createRequestPermissionResultContract()
            Log.d(TAG, "Permission contract created using static method: $contract")
            
            val launcher = activity.registerForActivityResult(contract) { grantedPermissions ->
                Log.d(TAG, "Permission result received: ${grantedPermissions?.size ?: 0} permissions")
                if (grantedPermissions != null && grantedPermissions.isNotEmpty()) {
                    Log.d(TAG, "Granted permissions: $grantedPermissions")
                    Log.d(TAG, "Checking if all required permissions are granted...")
                    val allGranted = requiredPermissions.all { it in grantedPermissions }
                    if (allGranted) {
                        Log.d(TAG, "All required permissions granted!")
                    } else {
                        Log.w(TAG, "Not all permissions granted. Required: $requiredPermissions, Granted: $grantedPermissions")
                    }
                } else {
                    Log.w(TAG, "Granted permissions set is null or empty")
                    Log.w(TAG, "This could mean:")
                    Log.w(TAG, "1. The permission dialog didn't appear")
                    Log.w(TAG, "2. The user denied all permissions")
                    Log.w(TAG, "3. Health Connect didn't recognize the app")
                }
                callback.onResult(grantedPermissions ?: emptySet())
            }
            
            Log.d(TAG, "Activity result launcher registered successfully")
            launcher
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create permission launcher", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Java-compatible wrapper for requestPermissions
     */
    fun requestPermissionsJava(
        permissionController: PermissionController,
        callback: PermissionCallback
    ) {
        coroutineScope.launch {
            try {
                val granted = requestPermissions(permissionController)
                mainHandler.post {
                    callback.onResult(granted)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    callback.onResult(emptySet())
                }
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    fun shutdown() {
        coroutineScope.cancel()
    }
}

