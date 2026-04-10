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

@Repository
class SleepLogRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    // MARK: - Row Mapping

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
     * Inserts a new sleep log and returns the full row (including DB-generated
     * columns like sleep_date and total_minutes_in_bed) via RETURNING *.
     * The feeling value needs an explicit Postgres enum cast.
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

    /** Returns a single sleep log for the given user and date, or null if none exists. */
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
