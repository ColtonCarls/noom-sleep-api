package com.noom.interview.fullstack.sleep.repository

import com.noom.interview.fullstack.sleep.model.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepLog
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDate

/**
 * Data access layer for the `sleep_log` table.
 * Uses [NamedParameterJdbcTemplate] for direct JDBC access (no ORM).
 */
@Repository
class SleepLogRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    // MARK: - Row Mapping

    /** Maps a result set row to a [SleepLog] domain entity. */
    private val rowMapper = RowMapper<SleepLog> { rs, _ ->
        SleepLog(
            id = rs.getLong("id"),
            userId = rs.getLong("user_id"),
            bedTime = rs.getTimestamp("bed_time").toInstant(),
            wakeTime = rs.getTimestamp("wake_time").toInstant(),
            feeling = MorningFeeling.valueOf(rs.getString("feeling")),
            sleepDate = rs.getDate("sleep_date").toLocalDate(),
            totalMinutesInBed = rs.getInt("total_minutes_in_bed"),
            createdAt = rs.getTimestamp("created_at").toInstant()
        )
    }

    // MARK: - Queries

    /**
     * Inserts a new sleep log and returns the full persisted row.
     * Uses `RETURNING *` to get DB-generated columns (sleep_date, total_minutes_in_bed)
     * in a single round trip. The feeling value requires an explicit Postgres enum cast.
     *
     * @param userId the owner of this sleep log.
     * @param request the sleep data to persist.
     * @return the complete [SleepLog] including generated columns.
     */
    fun insert(userId: Long, request: CreateSleepLogRequest): SleepLog {
        val sql = """
            INSERT INTO sleep_log (user_id, bed_time, wake_time, feeling)
            VALUES (:userId, :bedTime, :wakeTime, :feeling::morning_feeling)
            RETURNING *
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("bedTime", Timestamp.from(request.bedTime))
            .addValue("wakeTime", Timestamp.from(request.wakeTime))
            .addValue("feeling", request.feeling.name)

        return jdbcTemplate.queryForObject(sql, params, rowMapper)!!
    }

    /**
     * Looks up a single sleep log for a user on a specific date.
     *
     * @param userId the user to query for.
     * @param date the sleep_date to match (typically today for "last night").
     * @return the matching [SleepLog], or `null` if no entry exists.
     */
    fun findByUserIdAndDate(userId: Long, date: LocalDate): SleepLog? {
        val sql = """
            SELECT * FROM sleep_log
            WHERE user_id = :userId AND sleep_date = :date
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("date", date)

        return try {
            jdbcTemplate.queryForObject(sql, params, rowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    /**
     * Fetches all sleep logs for a user within a date range (inclusive).
     * Used by the service layer to compute 30-day averages in-memory.
     *
     * @param userId the user to query for.
     * @param startDate beginning of the range (inclusive).
     * @param endDate end of the range (inclusive).
     * @return list of [SleepLog] entries ordered by sleep_date descending.
     */
    fun findByUserIdAndDateRange(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SleepLog> {
        val sql = """
            SELECT * FROM sleep_log
            WHERE user_id = :userId
              AND sleep_date BETWEEN :startDate AND :endDate
            ORDER BY sleep_date DESC
        """.trimIndent()

        val params = MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("startDate", startDate)
            .addValue("endDate", endDate)

        return jdbcTemplate.query(sql, params, rowMapper)
    }
}
