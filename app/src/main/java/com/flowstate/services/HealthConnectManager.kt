package com.flowstate.services

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.flowstate.data.local.entities.HrLocal
import com.flowstate.data.local.entities.SleepLocal
import com.flowstate.data.local.entities.StepsLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * Manages integration with Health Connect for biometric data
 */
class HealthConnectManager(private val context: Context) {
    
    private val healthConnectClient: HealthConnectClient? = try {
        HealthConnectClient.getOrCreate(context)
    } catch (e: Exception) {
        null
    }

    /**
     * Check if Health Connect is available on this device
     */
    fun isAvailable(): Boolean = healthConnectClient != null

    /**
     * Read heart rate data from Health Connect
     */
    fun readHeartRate(startTimeMillis: Long, endTimeMillis: Long): CompletableFuture<List<HrLocal>> {
        val future = CompletableFuture<List<HrLocal>>()
        
        if (healthConnectClient == null) {
            future.complete(emptyList())
            return future
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timeRange = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTimeMillis),
                    Instant.ofEpochMilli(endTimeMillis)
                )
                
                val request = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = timeRange
                )
                
                val response = healthConnectClient.readRecords(request)
                
                val result = response.records.flatMap { record ->
                    record.samples.map { sample ->
                        HrLocal().apply {
                            timestamp = sample.time.toEpochMilli()
                            bpm = sample.beatsPerMinute.toInt()
                            synced = false
                        }
                    }
                }
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    /**
     * Read sleep data from Health Connect
     */
    fun readSleep(startTimeMillis: Long, endTimeMillis: Long): CompletableFuture<List<SleepLocal>> {
        val future = CompletableFuture<List<SleepLocal>>()
        
        if (healthConnectClient == null) {
            future.complete(emptyList())
            return future
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timeRange = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTimeMillis),
                    Instant.ofEpochMilli(endTimeMillis)
                )
                
                val request = ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = timeRange
                )
                
                val response = healthConnectClient.readRecords(request)
                
                val result = response.records.map { record ->
                    val sleepStart = record.startTime.toEpochMilli()
                    val sleepEnd = record.endTime.toEpochMilli()
                    val durationMinutes = ((sleepEnd - sleepStart) / (60 * 1000)).toInt()
                    
                    SleepLocal().apply {
                        sleep_start = sleepStart
                        sleep_end = sleepEnd
                        duration = durationMinutes
                        synced = false
                    }
                }
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    /**
     * Read steps count data from Health Connect
     */
    fun readSteps(startTimeMillis: Long, endTimeMillis: Long): CompletableFuture<List<StepsLocal>> {
        val future = CompletableFuture<List<StepsLocal>>()
        
        if (healthConnectClient == null) {
            future.complete(emptyList())
            return future
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timeRange = TimeRangeFilter.between(
                    Instant.ofEpochMilli(startTimeMillis),
                    Instant.ofEpochMilli(endTimeMillis)
                )
                
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = timeRange
                )
                
                val response = healthConnectClient.readRecords(request)
                
                val result = response.records.map { record ->
                    StepsLocal().apply {
                        timestamp = record.startTime.toEpochMilli()
                        steps = record.count.toInt()
                        synced = false
                    }
                }
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }
}
