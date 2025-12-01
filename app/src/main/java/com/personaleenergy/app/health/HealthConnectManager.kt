package com.personaleenergy.app.health

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.common.util.concurrent.ListenableFuture
import java.time.Instant

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient: HealthConnectClient = HealthConnectClient.getOrCreate(context)

    interface HealthDataCallback {
        fun onDataLoaded(data: String)
        fun onError(e: Exception)
    }

    fun checkPermissionsAndRun(requestPermissions: ActivityResultLauncher<Array<String>>) {
        val grantedPermissionsFuture = healthConnectClient.permissionController.getGrantedPermissions()
        grantedPermissionsFuture.addListener({
            val granted = try {
                grantedPermissionsFuture.get()
            } catch (e: Exception) {
                Log.e("HealthConnectManager", "Error checking permissions", e)
                emptySet()
            }

            if (granted.containsAll(PERMISSIONS)) {
                // Permissions already granted, no action needed
            } else {
                requestPermissions.launch(PERMISSIONS.toTypedArray())
            }
        }, context.mainExecutor)
    }

    fun readData(start: Instant, end: Instant, callback: HealthDataCallback) {
        readHeartRate(start, end, callback)
        readSleepSessions(start, end, callback)
        readSteps(start, end, callback)
        readTotalCaloriesBurned(start, end, callback)
    }

    private fun readHeartRate(start: Instant, end: Instant, callback: HealthDataCallback) {
        val request = ReadRecordsRequest(HeartRateRecord::class, TimeRangeFilter.between(start, end))
        processRequest(request, "Heart Rate Data", callback) { record ->
            record.samples.joinToString("\n") { "  ${it.beatsPerMinute} bpm at ${it.time}" }
        }
    }

    private fun readSleepSessions(start: Instant, end: Instant, callback: HealthDataCallback) {
        val request = ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(start, end))
        processRequest(request, "Sleep Session Data", callback) { record ->
            "  Slept from ${record.startTime} to ${record.endTime}"
        }
    }

    private fun readSteps(start: Instant, end: Instant, callback: HealthDataCallback) {
        val request = ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))
        processRequest(request, "Steps Data", callback) { record ->
            "  ${record.count} steps between ${record.startTime} and ${record.endTime}"
        }
    }

    private fun readTotalCaloriesBurned(start: Instant, end: Instant, callback: HealthDataCallback) {
        val request = ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))
        processRequest(request, "Calories Burned Data", callback) { record ->
            "  ${record.energy.inCalories} kcal between ${record.startTime} and ${record.endTime}"
        }
    }

    private fun <T : Record> processRequest(
        request: ReadRecordsRequest<T>,
        header: String,
        callback: HealthDataCallback,
        formatter: (T) -> String
    ) {
        val future: ListenableFuture<ReadRecordsResponse<T>> = healthConnectClient.readRecords(request)
        future.addListener({
            try {
                val response = future.get()
                val data = response.records.joinToString("\n", prefix = "$header:\n") { record ->
                    formatter(record)
                }
                callback.onDataLoaded(data)
            } catch (e: Exception) {
                callback.onError(e)
            }
        }, context.mainExecutor)
    }

    companion object {
        private val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
        )
    }
}
