package com.noom.interview.fullstack.sleep.exception

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

/**
 * Centralized exception handling for all REST controllers.
 * Translates domain exceptions into appropriate HTTP responses
 * so controllers stay clean and consistent.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handles cases where a requested sleep log does not exist.
     *
     * @param ex the not-found exception thrown by the service layer.
     * @return HTTP 404 with an error body.
     */
    @ExceptionHandler(SleepLogNotFoundException::class)
    fun handleNotFound(ex: SleepLogNotFoundException): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Resource not found",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
    }

    /**
     * Handles requests referencing a user that does not exist.
     *
     * @param ex the user-not-found exception thrown by the service layer.
     * @return HTTP 404 with an error body.
     */
    @ExceptionHandler(UserNotFoundException::class)
    fun handleUserNotFound(ex: UserNotFoundException): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "User not found",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
    }

    /**
     * Handles validation failures (e.g. wake time before bed time).
     *
     * @param ex the illegal argument exception from input validation.
     * @return HTTP 400 with an error body.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid request",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    /**
     * Handles malformed JSON or invalid enum values in the request body.
     * For example, sending "TERRIBLE" for a MorningFeeling field.
     *
     * @param ex the deserialization exception from Jackson.
     * @return HTTP 400 with a descriptive error body.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        val message = when (val cause = ex.cause) {
            is InvalidFormatException -> {
                val fieldName = cause.path.joinToString(".") { it.fieldName ?: "" }
                val accepted = cause.targetType.enumConstants?.joinToString { it.toString() }
                if (accepted != null) {
                    "Invalid value '${cause.value}' for field '$fieldName'. Accepted values: [$accepted]"
                } else {
                    "Invalid value '${cause.value}' for field '$fieldName'"
                }
            }
            is MissingKotlinParameterException -> {
                val fieldName = cause.parameter.name ?: "unknown"
                "Missing required field: '$fieldName'"
            }
            else -> "Malformed request body"
        }

        val body = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = message,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    /**
     * Handles DB constraint violations such as duplicate sleep logs
     * for the same user and date, or foreign key failures.
     *
     * @param ex the data integrity exception from the persistence layer.
     * @return HTTP 409 with a conflict error body.
     */
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleConflict(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> {
        val message = when {
            ex.message?.contains("uq_user_sleep_date") == true ->
                "A sleep log already exists for this user on that date"
            ex.message?.contains("sleep_log_user_id_fkey") == true ->
                "User does not exist"
            else -> "Data integrity violation"
        }
        val body = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = "Conflict",
            message = message,
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body)
    }

    /**
     * Catch-all for unexpected errors. Avoids leaking stack traces to the client.
     *
     * @param ex the unhandled exception.
     * @return HTTP 500 with a generic error body.
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception", ex)
        val body = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "An unexpected error occurred",
            timestamp = Instant.now()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body)
    }
}

/**
 * Standard error response body returned by all error handlers.
 *
 * @property status the HTTP status code.
 * @property error short error label (e.g. "Not Found").
 * @property message human-readable description of what went wrong.
 * @property timestamp when the error occurred.
 */
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val timestamp: Instant
)
