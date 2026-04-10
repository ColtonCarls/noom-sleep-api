package com.noom.interview.fullstack.sleep.model

import java.time.Instant
import java.time.LocalDate

/**
 * Domain entity representing a single night's sleep record.
 * Maps directly to the sleep_log table. Note that [sleepDate] and
 * [totalMinutesInBed] are generated columns in Postgres — they're
 * derived from bed/wake times and should be treated as read-only.
 */
data class SleepLog(
    val id: Long,
    val userId: Long,
    val bedTime: Instant,
    val wakeTime: Instant,
    val feeling: MorningFeeling,
    val sleepDate: LocalDate,
    val totalMinutesInBed: Int,
    val createdAt: Instant
)
