package com.flowstate.services

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.flowstate.data.local.entities.HrLocal
import com.flowstate.data.local.entities.SleepLocal
import com.flowstate.data.local.entities.HrvLocal
import com.flowstate.data.local.entities.StepsLocal
import com.flowstate.data.local.entities.WorkoutLocal
import com.flowstate.data.local.entities.BodyTempLocal
import kotlinx.coroutines.future.future
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.guava.future // Removing this import to fix ambiguity
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture

/**
 * Manager for interacting with Health Connect API (Kotlin implementation).
 */
class HealthConnectManager(private val context: Context) {

    private val healthConnectClient: HealthConnectClient = HealthConnectClient.getOrCreate(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    fun isAvailable(): Boolean {
        // Checking availability by trying to get the SDK status
        val availabilityStatus = HealthConnectClient.getSdkStatus(context)
        Log.d(TAG, "Health Connect SDK Status: $availabilityStatus")
        if (availabilityStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            Log.w(TAG, "Health Connect provider update required")
        }
        return availabilityStatus == HealthConnectClient.SDK_AVAILABLE
    }

    fun getRequiredPermissions(): Set<String> {
        return setOf(
            // Read-only permissions - no write permissions needed
            // Core metrics
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
            // Additional activity metrics
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ActivityIntensityRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(ElevationGainedRecord::class),
            HealthPermission.getReadPermission(FloorsClimbedRecord::class),
            HealthPermission.getReadPermission(SpeedRecord::class),
            HealthPermission.getReadPermission(PowerRecord::class),
            HealthPermission.getReadPermission(Vo2MaxRecord::class),
            HealthPermission.getReadPermission(WheelchairPushesRecord::class),
            HealthPermission.getReadPermission(CyclingPedalingCadenceRecord::class),
            // Missing permissions added
            HealthPermission.getReadPermission(SkinTemperatureRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(LeanBodyMassRecord::class),
            HealthPermission.getReadPermission(BoneMassRecord::class),
            HealthPermission.getReadPermission(BodyWaterMassRecord::class),
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
            // Vitals
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(RespiratoryRateRecord::class),
            HealthPermission.getReadPermission(SkinTemperatureRecord::class),
            // Body measurements
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(HeightRecord::class),
            HealthPermission.getReadPermission(BodyFatRecord::class),
            HealthPermission.getReadPermission(LeanBodyMassRecord::class),
            HealthPermission.getReadPermission(BoneMassRecord::class),
            HealthPermission.getReadPermission(BodyWaterMassRecord::class),
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
            // Nutrition & Hydration
            HealthPermission.getReadPermission(HydrationRecord::class),
            HealthPermission.getReadPermission(NutritionRecord::class),
            // Wellness
            HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
            // Blood & Glucose
            HealthPermission.getReadPermission(BloodGlucoseRecord::class)
        )
    }

    /**
     * Checks if all required permissions are granted.
     */
    fun hasAllPermissions(): CompletableFuture<Boolean> = scope.future {
        if (!isAvailable()) return@future false
        try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            granted.containsAll(getRequiredPermissions())
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }

    /**
     * Reads heart rate data and maps it to local entities.
     * Returns a CompletableFuture for Java interoperability.
     */
    fun readHeartRate(start: Instant, end: Instant): CompletableFuture<List<HrLocal>> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )

            // Map to local entities
            response.records.flatMap { record ->
                record.samples.map { sample ->
                    HrLocal().apply {
                        timestamp = sample.time.toEpochMilli()
                        bpm = sample.beatsPerMinute.toInt()
                        synced = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading heart rate. Permission granted? " + hasAllPermissions().get(), e)
            emptyList()
        }
    }

    /**
     * Reads sleep data and maps it to local entities.
     */
    fun readSleep(start: Instant, end: Instant): CompletableFuture<List<SleepLocal>> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )

            // Map to local entities
            response.records.mapNotNull { record ->
                val startMs = record.startTime.toEpochMilli()
                val endMs = record.endTime.toEpochMilli()
                val durationMinutes = ChronoUnit.MINUTES.between(record.startTime, record.endTime).toInt()

                if (durationMinutes > 0) {
                    SleepLocal().apply {
                        sleep_start = startMs
                        sleep_end = endMs
                        duration = durationMinutes
                        synced = false
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sleep data", e)
            emptyList()
        }
    }

    /**
     * Reads HRV data and maps to local entities.
     */
    fun readHRV(start: Instant, end: Instant): CompletableFuture<List<HrvLocal>> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.map { record ->
                HrvLocal().apply {
                    timestamp = record.time.toEpochMilli()
                    rmssd = record.heartRateVariabilityMillis
                    synced = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading HRV", e)
            emptyList()
        }
    }

    /**
     * Reads Steps data and maps to local entities.
     */
    fun readSteps(start: Instant, end: Instant): CompletableFuture<List<StepsLocal>> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.map { record ->
                StepsLocal().apply {
                    start_time = record.startTime.toEpochMilli()
                    end_time = record.endTime.toEpochMilli()
                    count = record.count.toInt()
                    synced = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps", e)
            emptyList()
        }
    }

    /**
     * Reads Workouts and maps to local entities.
     */
    fun readWorkouts(start: Instant, end: Instant): CompletableFuture<List<WorkoutLocal>> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.map { record ->
                val durationMinutes = ChronoUnit.MINUTES.between(record.startTime, record.endTime).toInt()
                WorkoutLocal().apply {
                    start_time = record.startTime.toEpochMilli()
                    end_time = record.endTime.toEpochMilli()
                    duration_minutes = durationMinutes
                    type = record.exerciseType.toString() // Or map int to string nicely
                    synced = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading workouts", e)
            emptyList()
        }
    }

    /**
     * Reads Body Temperature and maps to local entities.
     */
    fun readBodyTemperature(start: Instant, end: Instant): CompletableFuture<List<BodyTempLocal>> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = BodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.map { record ->
                BodyTempLocal().apply {
                    timestamp = record.time.toEpochMilli()
                    temperature_celsius = record.temperature.inCelsius
                    synced = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading body temperature", e)
            emptyList()
        }
    }

    /**
     * Reads additional health data types for comprehensive energy prediction
     * These methods read directly from Health Connect without storing in local DB
     */
    
    // Activity Metrics
    fun readActiveCalories(start: Instant, end: Instant): CompletableFuture<Double> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.sumOf { it.energy.inKilocalories.toDouble() }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading active calories", e)
            0.0
        }
    }
    
    fun readTotalCalories(start: Instant, end: Instant): CompletableFuture<Double> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.sumOf { it.energy.inKilocalories.toDouble() }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading total calories", e)
            0.0
        }
    }
    
    fun readDistance(start: Instant, end: Instant): CompletableFuture<Double> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(DistanceRecord::class, TimeRangeFilter.between(start, end))
            )
            // Note: DistanceRecord API may vary - returning 0.0 for now
            // TODO: Implement proper distance reading when API is confirmed
            Log.d(TAG, "Distance records found: ${response.records.size}, but distance reading not yet implemented")
            0.0
        } catch (e: Exception) {
            Log.w(TAG, "Error reading distance", e)
            0.0
        }
    }
    
    fun readElevationGained(start: Instant, end: Instant): CompletableFuture<Double> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(ElevationGainedRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.sumOf { it.elevation.inMeters.toDouble() }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading elevation", e)
            0.0
        }
    }
    
    fun readFloorsClimbed(start: Instant, end: Instant): CompletableFuture<Int> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(FloorsClimbedRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.sumOf { it.floors.toInt() }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading floors climbed", e)
            0
        }
    }
    
    fun readRestingHeartRate(start: Instant, end: Instant): CompletableFuture<Double?> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(RestingHeartRateRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.lastOrNull()?.beatsPerMinute?.toDouble()
        } catch (e: Exception) {
            Log.w(TAG, "Error reading resting heart rate", e)
            null
        }
    }
    
    fun readBloodPressure(start: Instant, end: Instant): CompletableFuture<Pair<Double?, Double?>> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(BloodPressureRecord::class, TimeRangeFilter.between(start, end))
            )
            val latest = response.records.lastOrNull()
            Pair(
                latest?.systolic?.inMillimetersOfMercury?.toDouble(),
                latest?.diastolic?.inMillimetersOfMercury?.toDouble()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error reading blood pressure", e)
            Pair(null, null)
        }
    }
    
    fun readOxygenSaturation(start: Instant, end: Instant): CompletableFuture<Double?> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(OxygenSaturationRecord::class, TimeRangeFilter.between(start, end))
            )
            val record = response.records.lastOrNull()
            // percentage property is a Percentage object
            record?.percentage?.value
        } catch (e: Exception) {
            Log.w(TAG, "Error reading oxygen saturation", e)
            null
        }
    }
    
    fun readRespiratoryRate(start: Instant, end: Instant): CompletableFuture<Double?> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(RespiratoryRateRecord::class, TimeRangeFilter.between(start, end))
            )
            val record = response.records.lastOrNull()
            // RespiratoryRateRecord.rate returns a Double (breaths per minute)
            record?.rate
        } catch (e: Exception) {
            Log.w(TAG, "Error reading respiratory rate: ${e.message}", e)
            null
        }
    }
    
    fun readWeight(start: Instant, end: Instant): CompletableFuture<Double?> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(start, end))
            )
            val record = response.records.lastOrNull()
            record?.weight?.inKilograms?.toDouble()
        } catch (e: Exception) {
            Log.w(TAG, "Error reading weight", e)
            null
        }
    }
    
    fun readHeight(start: Instant, end: Instant): CompletableFuture<Double?> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(HeightRecord::class, TimeRangeFilter.between(start, end))
            )
            val record = response.records.lastOrNull()
            record?.height?.inMeters?.toDouble()
        } catch (e: Exception) {
            Log.w(TAG, "Error reading height", e)
            null
        }
    }
    
    fun readHydration(start: Instant, end: Instant): CompletableFuture<Double> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(HydrationRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.sumOf { it.volume.inLiters.toDouble() }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading hydration", e)
            0.0
        }
    }
    
    fun readBloodGlucose(start: Instant, end: Instant): CompletableFuture<Double?> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(BloodGlucoseRecord::class, TimeRangeFilter.between(start, end))
            )
            val record = response.records.lastOrNull()
            record?.level?.inMillimolesPerLiter?.toDouble()
        } catch (e: Exception) {
            Log.w(TAG, "Error reading blood glucose", e)
            null
        }
    }
    
    fun readMindfulness(start: Instant, end: Instant): CompletableFuture<Double> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(MindfulnessSessionRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.sumOf { 
                ChronoUnit.MINUTES.between(it.startTime, it.endTime).toDouble()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error reading mindfulness", e)
            0.0
        }
    }
    
    fun readVo2Max(start: Instant, end: Instant): CompletableFuture<Double?> = scope.future {
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(Vo2MaxRecord::class, TimeRangeFilter.between(start, end))
            )
            response.records.lastOrNull()?.vo2MillilitersPerMinuteKilogram?.toDouble()
        } catch (e: Exception) {
            Log.w(TAG, "Error reading VO2 max", e)
            null
        }
    }

    companion object {
        private const val TAG = "HealthConnectManager"
    }
}
