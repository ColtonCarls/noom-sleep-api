package com.noom.interview.fullstack.sleep.service

import com.noom.interview.fullstack.sleep.exception.SleepLogNotFoundException
import com.noom.interview.fullstack.sleep.exception.UserNotFoundException
import com.noom.interview.fullstack.sleep.model.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import com.noom.interview.fullstack.sleep.repository.SleepLogRepository
import com.noom.interview.fullstack.sleep.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class SleepLogServiceTest {

    private val sleepLogRepository: SleepLogRepository = mock()
    private val userRepository: UserRepository = mock()
    private val service = SleepLogService(sleepLogRepository, userRepository)

    private val userId = 1L

    @BeforeEach
    fun setUp() {
        whenever(userRepository.existsById(userId)).thenReturn(true)
    }

    // MARK: - Test Helpers

    private fun buildSleepLog(
        bedTime: Instant = Instant.parse("2026-04-08T22:53:00Z"),
        wakeTime: Instant = Instant.parse("2026-04-09T07:05:00Z"),
        feeling: MorningFeeling = MorningFeeling.GOOD,
        sleepDate: LocalDate = LocalDate.of(2026, 4, 9),
        totalMinutesInBed: Int = 492
    ) = SleepLog(
        id = 1L,
        userId = userId,
        bedTime = bedTime,
        wakeTime = wakeTime,
        feeling = feeling,
        sleepDate = sleepDate,
        totalMinutesInBed = totalMinutesInBed,
        createdAt = Instant.now()
    )

    // MARK: - createSleepLog

    @Nested
    inner class CreateSleepLog {

        @Test
        fun `creates sleep log and returns response`() {
            val request = CreateSleepLogRequest(
                bedTime = Instant.parse("2026-04-08T22:53:00Z"),
                wakeTime = Instant.parse("2026-04-09T07:05:00Z"),
                feeling = MorningFeeling.GOOD
            )
            val savedLog = buildSleepLog()
            whenever(sleepLogRepository.insert(userId, request)).thenReturn(savedLog)

            val response = service.createSleepLog(userId, request)

            assertThat(response.id).isEqualTo(1L)
            assertThat(response.feeling).isEqualTo(MorningFeeling.GOOD)
            assertThat(response.totalMinutesInBed).isEqualTo(492)
            verify(sleepLogRepository).insert(userId, request)
        }

        @Test
        fun `throws UserNotFoundException when user does not exist`() {
            whenever(userRepository.existsById(999L)).thenReturn(false)

            val request = CreateSleepLogRequest(
                bedTime = Instant.parse("2026-04-08T22:53:00Z"),
                wakeTime = Instant.parse("2026-04-09T07:05:00Z"),
                feeling = MorningFeeling.GOOD
            )

            assertThatThrownBy { service.createSleepLog(999L, request) }
                .isInstanceOf(UserNotFoundException::class.java)
                .hasMessageContaining("999")

            verify(sleepLogRepository, never()).insert(any(), any())
        }

        @Test
        fun `throws IllegalArgumentException when wake time is before bed time`() {
            val request = CreateSleepLogRequest(
                bedTime = Instant.parse("2026-04-09T07:05:00Z"),
                wakeTime = Instant.parse("2026-04-08T22:53:00Z"),
                feeling = MorningFeeling.GOOD
            )

            assertThatThrownBy { service.createSleepLog(userId, request) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Wake time must be after bed time")

            verify(sleepLogRepository, never()).insert(any(), any())
        }
    }

    // MARK: - getLastNightSleepLog

    @Nested
    inner class GetLastNightSleepLog {

        @Test
        fun `returns sleep log for today`() {
            val today = LocalDate.now()
            val log = buildSleepLog(sleepDate = today)
            whenever(sleepLogRepository.findByUserIdAndDate(userId, today)).thenReturn(log)

            val response = service.getLastNightSleepLog(userId)

            assertThat(response.sleepDate).isEqualTo(today)
            assertThat(response.feeling).isEqualTo(MorningFeeling.GOOD)
        }

        @Test
        fun `throws UserNotFoundException when user does not exist`() {
            whenever(userRepository.existsById(999L)).thenReturn(false)

            assertThatThrownBy { service.getLastNightSleepLog(999L) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `throws SleepLogNotFoundException when no log exists for today`() {
            val today = LocalDate.now()
            whenever(sleepLogRepository.findByUserIdAndDate(userId, today)).thenReturn(null)

            assertThatThrownBy { service.getLastNightSleepLog(userId) }
                .isInstanceOf(SleepLogNotFoundException::class.java)
                .hasMessageContaining(userId.toString())
        }
    }

    // MARK: - getThirtyDayAverages

    @Nested
    inner class GetThirtyDayAverages {

        @Test
        fun `returns correct averages for multiple logs`() {
            val today = LocalDate.now()
            val startDate = today.minusDays(29)

            val logs = listOf(
                buildSleepLog(
                    bedTime = Instant.parse("2026-04-08T23:00:00Z"),
                    wakeTime = Instant.parse("2026-04-09T07:00:00Z"),
                    feeling = MorningFeeling.GOOD,
                    totalMinutesInBed = 480
                ),
                buildSleepLog(
                    bedTime = Instant.parse("2026-04-07T22:00:00Z"),
                    wakeTime = Instant.parse("2026-04-08T06:00:00Z"),
                    feeling = MorningFeeling.BAD,
                    totalMinutesInBed = 480
                ),
                buildSleepLog(
                    bedTime = Instant.parse("2026-04-06T23:30:00Z"),
                    wakeTime = Instant.parse("2026-04-07T07:30:00Z"),
                    feeling = MorningFeeling.OK,
                    totalMinutesInBed = 480
                )
            )
            whenever(sleepLogRepository.findByUserIdAndDateRange(userId, startDate, today))
                .thenReturn(logs)

            val response = service.getThirtyDayAverages(userId)

            assertThat(response.startDate).isEqualTo(startDate)
            assertThat(response.endDate).isEqualTo(today)
            assertThat(response.averageTotalMinutesInBed).isEqualTo(480)
            assertThat(response.feelingFrequencies[MorningFeeling.GOOD]).isEqualTo(1)
            assertThat(response.feelingFrequencies[MorningFeeling.BAD]).isEqualTo(1)
            assertThat(response.feelingFrequencies[MorningFeeling.OK]).isEqualTo(1)
        }

        @Test
        fun `includes all feeling values even when count is zero`() {
            val today = LocalDate.now()
            val startDate = today.minusDays(29)

            val logs = listOf(
                buildSleepLog(feeling = MorningFeeling.GOOD, totalMinutesInBed = 480)
            )
            whenever(sleepLogRepository.findByUserIdAndDateRange(userId, startDate, today))
                .thenReturn(logs)

            val response = service.getThirtyDayAverages(userId)

            assertThat(response.feelingFrequencies).containsKeys(
                MorningFeeling.BAD, MorningFeeling.OK, MorningFeeling.GOOD
            )
            assertThat(response.feelingFrequencies[MorningFeeling.BAD]).isEqualTo(0)
            assertThat(response.feelingFrequencies[MorningFeeling.OK]).isEqualTo(0)
            assertThat(response.feelingFrequencies[MorningFeeling.GOOD]).isEqualTo(1)
        }

        @Test
        fun `averages bed times correctly across midnight`() {
            val today = LocalDate.now()
            val startDate = today.minusDays(29)

            val logs = listOf(
                buildSleepLog(
                    bedTime = Instant.parse("2026-04-08T23:00:00Z"),
                    wakeTime = Instant.parse("2026-04-09T07:00:00Z"),
                    totalMinutesInBed = 480
                ),
                buildSleepLog(
                    bedTime = Instant.parse("2026-04-07T01:00:00Z"),
                    wakeTime = Instant.parse("2026-04-07T09:00:00Z"),
                    totalMinutesInBed = 480
                )
            )
            whenever(sleepLogRepository.findByUserIdAndDateRange(userId, startDate, today))
                .thenReturn(logs)

            val response = service.getThirtyDayAverages(userId)

            // 23:00 and 01:00 should average to midnight, not noon
            assertThat(response.averageBedTime).isEqualTo(LocalTime.MIDNIGHT)
        }

        @Test
        fun `throws UserNotFoundException when user does not exist`() {
            whenever(userRepository.existsById(999L)).thenReturn(false)

            assertThatThrownBy { service.getThirtyDayAverages(999L) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `throws SleepLogNotFoundException when no logs in window`() {
            val today = LocalDate.now()
            val startDate = today.minusDays(29)
            whenever(sleepLogRepository.findByUserIdAndDateRange(userId, startDate, today))
                .thenReturn(emptyList())

            assertThatThrownBy { service.getThirtyDayAverages(userId) }
                .isInstanceOf(SleepLogNotFoundException::class.java)
        }

        @Test
        fun `single log returns that log's values as averages`() {
            val today = LocalDate.now()
            val startDate = today.minusDays(29)

            val logs = listOf(
                buildSleepLog(
                    bedTime = Instant.parse("2026-04-08T22:30:00Z"),
                    wakeTime = Instant.parse("2026-04-09T06:30:00Z"),
                    feeling = MorningFeeling.OK,
                    totalMinutesInBed = 480
                )
            )
            whenever(sleepLogRepository.findByUserIdAndDateRange(userId, startDate, today))
                .thenReturn(logs)

            val response = service.getThirtyDayAverages(userId)

            assertThat(response.averageTotalMinutesInBed).isEqualTo(480)
            assertThat(response.averageBedTime).isEqualTo(LocalTime.of(22, 30))
            assertThat(response.averageWakeTime).isEqualTo(LocalTime.of(6, 30))
        }
    }
}
