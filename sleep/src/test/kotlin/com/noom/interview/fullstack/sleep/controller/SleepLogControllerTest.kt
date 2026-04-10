package com.noom.interview.fullstack.sleep.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.noom.interview.fullstack.sleep.exception.SleepLogNotFoundException
import com.noom.interview.fullstack.sleep.exception.UserNotFoundException
import com.noom.interview.fullstack.sleep.model.CreateSleepLogRequest
import com.noom.interview.fullstack.sleep.model.MorningFeeling
import com.noom.interview.fullstack.sleep.model.SleepAveragesResponse
import com.noom.interview.fullstack.sleep.model.SleepLogResponse
import com.noom.interview.fullstack.sleep.service.SleepLogService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(SleepLogController::class)
@DisplayName("SleepLogController")
class SleepLogControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var sleepLogService: SleepLogService

    private val userId = 1L
    private val basePath = "/api/users/$userId/sleep-logs"

    @Nested
    @DisplayName("POST /api/users/{userId}/sleep-logs")
    inner class CreateSleepLog {

        @Test
        fun `returns 201 with created sleep log`() {
            val request = CreateSleepLogRequest(
                bedTime = Instant.parse("2026-04-08T22:53:00Z"),
                wakeTime = Instant.parse("2026-04-09T07:05:00Z"),
                feeling = MorningFeeling.GOOD
            )
            val response = SleepLogResponse(
                id = 1L,
                sleepDate = LocalDate.of(2026, 4, 9),
                bedTime = request.bedTime,
                wakeTime = request.wakeTime,
                totalMinutesInBed = 492,
                feeling = MorningFeeling.GOOD
            )
            whenever(sleepLogService.createSleepLog(eq(userId), any())).thenReturn(response)

            mockMvc.perform(
                post(basePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.feeling").value("GOOD"))
                .andExpect(jsonPath("$.totalMinutesInBed").value(492))
        }

        @Test
        fun `returns 400 when wake time is before bed time`() {
            whenever(sleepLogService.createSleepLog(eq(userId), any()))
                .thenThrow(IllegalArgumentException("Wake time must be after bed time"))

            val body = """{"bedTime":"2026-04-09T07:05:00Z","wakeTime":"2026-04-08T22:53:00Z","feeling":"GOOD"}"""

            mockMvc.perform(
                post(basePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Wake time must be after bed time"))
        }

        @Test
        fun `returns 400 when request body has invalid enum`() {
            val body = """{"bedTime":"2026-04-08T22:53:00Z","wakeTime":"2026-04-09T07:05:00Z","feeling":"TERRIBLE"}"""

            mockMvc.perform(
                post(basePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error").value("Bad Request"))
        }

        @Test
        fun `returns 400 when required field is missing`() {
            val body = """{"bedTime":"2026-04-08T22:53:00Z","feeling":"GOOD"}"""

            mockMvc.perform(
                post(basePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        fun `returns 404 when user does not exist`() {
            whenever(sleepLogService.createSleepLog(eq(userId), any()))
                .thenThrow(UserNotFoundException("User with id $userId not found"))

            val body = """{"bedTime":"2026-04-08T22:53:00Z","wakeTime":"2026-04-09T07:05:00Z","feeling":"GOOD"}"""

            mockMvc.perform(
                post(basePath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.message").value("User with id $userId not found"))
        }
    }

    @Nested
    @DisplayName("GET /api/users/{userId}/sleep-logs/last-night")
    inner class GetLastNightSleepLog {

        @Test
        fun `returns 200 with sleep log`() {
            val response = SleepLogResponse(
                id = 1L,
                sleepDate = LocalDate.now(),
                bedTime = Instant.parse("2026-04-08T22:53:00Z"),
                wakeTime = Instant.parse("2026-04-09T07:05:00Z"),
                totalMinutesInBed = 492,
                feeling = MorningFeeling.GOOD
            )
            whenever(sleepLogService.getLastNightSleepLog(userId)).thenReturn(response)

            mockMvc.perform(get("$basePath/last-night"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.feeling").value("GOOD"))
        }

        @Test
        fun `returns 404 when no log exists for today`() {
            whenever(sleepLogService.getLastNightSleepLog(userId))
                .thenThrow(SleepLogNotFoundException("No sleep log found for user $userId on ${LocalDate.now()}"))

            mockMvc.perform(get("$basePath/last-night"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("Not Found"))
        }

        @Test
        fun `returns 404 when user does not exist`() {
            whenever(sleepLogService.getLastNightSleepLog(userId))
                .thenThrow(UserNotFoundException("User with id $userId not found"))

            mockMvc.perform(get("$basePath/last-night"))
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("GET /api/users/{userId}/sleep-logs/averages")
    inner class GetThirtyDayAverages {

        @Test
        fun `returns 200 with averages`() {
            val response = SleepAveragesResponse(
                startDate = LocalDate.now().minusDays(29),
                endDate = LocalDate.now(),
                averageTotalMinutesInBed = 480,
                averageBedTime = LocalTime.of(23, 0),
                averageWakeTime = LocalTime.of(7, 0),
                feelingFrequencies = mapOf(
                    MorningFeeling.BAD to 2,
                    MorningFeeling.OK to 5,
                    MorningFeeling.GOOD to 10
                )
            )
            whenever(sleepLogService.getThirtyDayAverages(userId)).thenReturn(response)

            mockMvc.perform(get("$basePath/averages"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.averageTotalMinutesInBed").value(480))
                .andExpect(jsonPath("$.averageBedTime").value("23:00:00"))
                .andExpect(jsonPath("$.averageWakeTime").value("07:00:00"))
                .andExpect(jsonPath("$.feelingFrequencies.GOOD").value(10))
                .andExpect(jsonPath("$.feelingFrequencies.BAD").value(2))
                .andExpect(jsonPath("$.feelingFrequencies.OK").value(5))
        }

        @Test
        fun `returns 404 when no logs exist in window`() {
            whenever(sleepLogService.getThirtyDayAverages(userId))
                .thenThrow(SleepLogNotFoundException("No sleep logs found"))

            mockMvc.perform(get("$basePath/averages"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error").value("Not Found"))
        }

        @Test
        fun `returns 404 when user does not exist`() {
            whenever(sleepLogService.getThirtyDayAverages(userId))
                .thenThrow(UserNotFoundException("User with id $userId not found"))

            mockMvc.perform(get("$basePath/averages"))
                .andExpect(status().isNotFound)
        }
    }
}
