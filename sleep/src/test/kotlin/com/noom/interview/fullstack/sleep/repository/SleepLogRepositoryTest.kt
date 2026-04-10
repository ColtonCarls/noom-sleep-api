package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.model.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("SleepLogRepository")
class SleepLogRepositoryTest {

    @Mock
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @InjectMocks
    private lateinit var repository: SleepLogRepository

    private val sampleLog = SleepLog(
        id = 1L,
        userId = 1L,
        bedTime = Instant.parse("2026-04-08T22:53:00Z"),
        wakeTime = Instant.parse("2026-04-09T07:05:00Z"),
        feeling = MorningFeeling.GOOD,
        sleepDate = LocalDate.of(2026, 4, 9),
        totalMinutesInBed = 492,
        createdAt = Instant.parse("2026-04-09T07:10:00Z")
    )

    @Nested
    @DisplayName("insert")
    inner class Insert {

        @Test
        fun `persists sleep log with correct parameters and returns mapped result`() {
            val request = CreateSleepLogRequest(
                bedTime = sampleLog.bedTime,
                wakeTime = sampleLog.wakeTime,
                feeling = MorningFeeling.GOOD
            )
            whenever(jdbcTemplate.queryForObject(any<String>(), any<SqlParameterSource>(), any<RowMapper<SleepLog>>()))
                .thenReturn(sampleLog)

            val result = repository.insert(1L, request)

            assertThat(result).isEqualTo(sampleLog)
            verify(jdbcTemplate).queryForObject(
                argThat<String> { contains("INSERT INTO sleep_log") && contains("RETURNING") },
                argThat<SqlParameterSource> {
                    getValue("userId") == 1L &&
                    getValue("feeling") == "GOOD"
                },
                any<RowMapper<SleepLog>>()
            )
        }
    }

    @Nested
    @DisplayName("findByUserIdAndDate")
    inner class FindByUserIdAndDate {

        @Test
        fun `returns sleep log when a matching record exists`() {
            val date = LocalDate.of(2026, 4, 9)
            whenever(jdbcTemplate.queryForObject(any<String>(), any<SqlParameterSource>(), any<RowMapper<SleepLog>>()))
                .thenReturn(sampleLog)

            val result = repository.findByUserIdAndDate(1L, date)

            assertThat(result).isNotNull
            assertThat(result).isEqualTo(sampleLog)
            verify(jdbcTemplate).queryForObject(
                argThat<String> { contains("sleep_date = :date") },
                argThat<SqlParameterSource> {
                    getValue("userId") == 1L &&
                    getValue("date") == date
                },
                any<RowMapper<SleepLog>>()
            )
        }

        @Test
        fun `returns null when no matching record exists`() {
            whenever(jdbcTemplate.queryForObject(any<String>(), any<SqlParameterSource>(), any<RowMapper<SleepLog>>()))
                .thenThrow(EmptyResultDataAccessException(1))

            val result = repository.findByUserIdAndDate(1L, LocalDate.now())

            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("findByUserIdAndDateRange")
    inner class FindByUserIdAndDateRange {

        @Test
        fun `returns logs within the specified date range`() {
            val startDate = LocalDate.of(2026, 3, 11)
            val endDate = LocalDate.of(2026, 4, 9)
            whenever(jdbcTemplate.query(any<String>(), any<SqlParameterSource>(), any<RowMapper<SleepLog>>()))
                .thenReturn(listOf(sampleLog))

            val result = repository.findByUserIdAndDateRange(1L, startDate, endDate)

            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(sampleLog)
            verify(jdbcTemplate).query(
                argThat<String> { contains("BETWEEN :startDate AND :endDate") },
                argThat<SqlParameterSource> {
                    getValue("userId") == 1L &&
                    getValue("startDate") == startDate &&
                    getValue("endDate") == endDate
                },
                any<RowMapper<SleepLog>>()
            )
        }

        @Test
        fun `returns empty list when no logs exist in range`() {
            whenever(jdbcTemplate.query(any<String>(), any<SqlParameterSource>(), any<RowMapper<SleepLog>>()))
                .thenReturn(emptyList())

            val result = repository.findByUserIdAndDateRange(1L, LocalDate.now().minusDays(29), LocalDate.now())

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("RowMapper")
    inner class RowMapping {

        @Test
        fun `maps ResultSet columns to SleepLog domain object`() {
            val rs: ResultSet = mock {
                on { getLong("id") } doReturn 1L
                on { getLong("user_id") } doReturn 1L
                on { getTimestamp("bed_time") } doReturn Timestamp.from(sampleLog.bedTime)
                on { getTimestamp("wake_time") } doReturn Timestamp.from(sampleLog.wakeTime)
                on { getString("feeling") } doReturn "GOOD"
                on { getDate("sleep_date") } doReturn java.sql.Date.valueOf(sampleLog.sleepDate)
                on { getInt("total_minutes_in_bed") } doReturn 492
                on { getTimestamp("created_at") } doReturn Timestamp.from(sampleLog.createdAt)
            }
            whenever(jdbcTemplate.queryForObject(any<String>(), any<SqlParameterSource>(), any<RowMapper<SleepLog>>()))
                .thenAnswer { invocation ->
                    invocation.getArgument<RowMapper<SleepLog>>(2).mapRow(rs, 0)
                }

            val request = CreateSleepLogRequest(
                bedTime = sampleLog.bedTime,
                wakeTime = sampleLog.wakeTime,
                feeling = MorningFeeling.GOOD
            )
            val result = repository.insert(1L, request)

            assertThat(result.id).isEqualTo(1L)
            assertThat(result.userId).isEqualTo(1L)
            assertThat(result.bedTime).isEqualTo(sampleLog.bedTime)
            assertThat(result.wakeTime).isEqualTo(sampleLog.wakeTime)
            assertThat(result.feeling).isEqualTo(MorningFeeling.GOOD)
            assertThat(result.sleepDate).isEqualTo(sampleLog.sleepDate)
            assertThat(result.totalMinutesInBed).isEqualTo(492)
        }
    }
}
