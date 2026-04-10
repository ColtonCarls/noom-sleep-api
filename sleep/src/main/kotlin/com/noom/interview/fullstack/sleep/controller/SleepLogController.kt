package com.noom.interview.fullstack.sleep.controller

import com.noom.interview.fullstack.sleep.model.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.model.SleepAveragesResponse
import com.noom.interview.fullstack.sleep.model.SleepLogResponse
import com.noom.interview.fullstack.sleep.service.SleepLogService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for sleep log operations.
 * All endpoints are scoped to a specific user via the `userId` path variable.
 * Authentication is out of scope — userId is accepted as a path parameter.
 */
@RestController
@RequestMapping("/api/users/{userId}/sleep-logs")
class SleepLogController(
    private val sleepLogService: SleepLogService
) {

    /**
     * Creates a new sleep log entry for the given user.
     *
     * @param userId the ID of the user creating the log.
     * @param request the sleep data (bed time, wake time, morning feeling).
     * @return the created sleep log with HTTP 201.
     */
    @PostMapping
    fun createSleepLog(
        @PathVariable userId: Long,
        @RequestBody request: CreateSleepLogRequest
    ): ResponseEntity<SleepLogResponse> {
        val response = sleepLogService.createSleepLog(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * Fetches the sleep log for last night (today's date) for the given user.
     *
     * @param userId the ID of the user.
     * @return the sleep log response, or 404 if no entry exists for today.
     */
    @GetMapping("/last-night")
    fun getLastNightSleepLog(
        @PathVariable userId: Long
    ): ResponseEntity<SleepLogResponse> {
        val response = sleepLogService.getLastNightSleepLog(userId)
        return ResponseEntity.ok(response)
    }

    /**
     * Returns 30-day sleep averages for the given user.
     *
     * @param userId the ID of the user.
     * @return aggregated sleep stats, or 404 if no logs exist in the window.
     */
    @GetMapping("/averages")
    fun getThirtyDayAverages(
        @PathVariable userId: Long
    ): ResponseEntity<SleepAveragesResponse> {
        val response = sleepLogService.getThirtyDayAverages(userId)
        return ResponseEntity.ok(response)
    }
}
