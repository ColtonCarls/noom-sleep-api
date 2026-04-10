package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.exception.SleepLogNotFoundException
import com.noom.interview.fullstack.sleep.exception.UserNotFoundException
import com.noom.interview.fullstack.sleep.model.*
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import com.noom.interview.fullstack.sleep.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@Service
class SleepLogService(
    private val repository: SleepLogRepository,
    private val userRepository: UserRepository
) {

    // MARK: - Public API

    /**
     * Creates a new sleep log entry after validating the request.
     *
     * @param userId the ID of the user creating the log.
     * @param request the sleep data to record.
     * @return the created sleep log as a response DTO.
     * @throws UserNotFoundException if the user does not exist.
     * @throws IllegalArgumentException if [CreateSleepLogRequest.wakeTime] is not after [CreateSleepLogRequest.bedTime].
     */
    @Transactional
    fun createSleepLog(userId: Long, request: CreateSleepLogRequest): SleepLogResponse {
        validateUserExists(userId)
        require(request.wakeTime.isAfter(request.bedTime)) {
            "Wake time must be after bed time"
        }

        val sleepLog = repository.insert(userId, request)
        return sleepLog.toResponse()
    }

    /**
     * Retrieves today's sleep log for the given user.
     * "Last night" is identified by `sleep_date = today`, since `sleep_date`
     * is derived from [SleepLog.wakeTime] (i.e. the morning you woke up).
     *
     * @param userId the ID of the user to look up.
     * @return the sleep log response for today.
     * @throws UserNotFoundException if the user does not exist.
     * @throws SleepLogNotFoundException if no log exists for today.
     */
    @Transactional(readOnly = true)
    fun getLastNightSleepLog(userId: Long): SleepLogResponse {
        validateUserExists(userId)
        val today = LocalDate.now()
        val sleepLog = repository.findByUserIdAndDate(userId, today)
            ?: throw SleepLogNotFoundException("No sleep log found for user $userId on $today")
        return sleepLog.toResponse()
    }

    /**
     * Computes averages over the last 30 days (inclusive) for the given user.
     *
     * @param userId the ID of the user.
     * @return aggregated sleep stats including avg duration, avg bed/wake times, and feeling counts.
     * @throws UserNotFoundException if the user does not exist.
     * @throws SleepLogNotFoundException if no logs exist in the 30-day window.
     */
    @Transactional(readOnly = true)
    fun getThirtyDayAverages(userId: Long): SleepAveragesResponse {
        validateUserExists(userId)
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(29) // 30 days inclusive

        val logs = repository.findByUserIdAndDateRange(userId, startDate, endDate)
        if (logs.isEmpty()) {
            throw SleepLogNotFoundException(
                "No sleep logs found for user $userId between $startDate and $endDate"
            )
        }

        return computeAverages(logs, startDate, endDate)
    }

    // MARK: - Averaging Logic

    /**
     * Builds the [SleepAveragesResponse] from a list of sleep logs.
     *
     * @param logs non-empty list of sleep logs within the date range.
     * @param startDate beginning of the averaging window.
     * @param endDate end of the averaging window.
     */
    private fun computeAverages(
        logs: List<SleepLog>,
        startDate: LocalDate,
        endDate: LocalDate
    ): SleepAveragesResponse {
        val avgMinutes = logs.map { it.totalMinutesInBed }.average().toInt()
        val avgBedTime = averageTimeOfDay(logs.map { it.bedTime })
        val avgWakeTime = averageTimeOfDay(logs.map { it.wakeTime })

        // Ensure all enum values appear in the map, even if count is 0
        val feelingFrequencies = MorningFeeling.values().associateWith { feeling ->
            logs.count { it.feeling == feeling }
        }

        return SleepAveragesResponse(
            startDate = startDate,
            endDate = endDate,
            averageTotalMinutesInBed = avgMinutes,
            averageBedTime = avgBedTime,
            averageWakeTime = avgWakeTime,
            feelingFrequencies = feelingFrequencies
        )
    }

    /**
     * Averages times-of-day using a noon-offset approach so that times
     * crossing midnight (e.g. 11 PM and 1 AM) average correctly to ~midnight
     * instead of producing a nonsensical noon result.
     *
     * @param instants the timestamps whose time-of-day components will be averaged.
     * @return the average [LocalTime] in UTC.
     */
    private fun averageTimeOfDay(instants: List<Instant>): LocalTime {
        val secondsInDay = 86_400L
        val noonOffset = 43_200L

        val avgOffset = instants.map { instant ->
            val secondOfDay = instant.atZone(ZoneOffset.UTC).toLocalTime().toSecondOfDay().toLong()
            (secondOfDay - noonOffset + secondsInDay) % secondsInDay
        }.average().toLong()

        val secondOfDay = (avgOffset + noonOffset) % secondsInDay
        return LocalTime.ofSecondOfDay(secondOfDay)
    }

    // MARK: - Validation

    private fun validateUserExists(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw UserNotFoundException("User with id $userId not found")
        }
    }

    // MARK: - Mapping

    private fun SleepLog.toResponse() = SleepLogResponse(
        id = id,
        sleepDate = sleepDate,
        bedTime = bedTime,
        wakeTime = wakeTime,
        totalMinutesInBed = totalMinutesInBed,
        feeling = feeling
    )
}
