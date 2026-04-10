package com.noom.interview.fullstack.sleep.model

import java.time.Instant
import java.time.LocalDate

/**
 * Domain entity representing a single night's sleep record.
 * Maps directly to the `sleep_log` table.
 *
 * @property id auto-generated primary key.
 * @property userId the owner of this sleep log.
 * @property bedTime when the user went to bed (full timestamp with timezone).
 * @property wakeTime when the user woke up.
 * @property feeling how the user felt in the morning.
 * @property sleepDate DB-generated column derived from [wakeTime]; treat as read-only.
 * @property totalMinutesInBed DB-generated column (wake − bed in minutes); treat as read-only.
 * @property createdAt row creation timestamp.
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
