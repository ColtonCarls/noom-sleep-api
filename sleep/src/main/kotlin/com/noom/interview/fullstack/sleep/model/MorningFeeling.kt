package com.noom.interview.fullstack.sleep.model

/**
 * Represents how the user felt when they woke up.
 * Maps directly to the `morning_feeling` Postgres enum type.
 */
enum class MorningFeeling {
    BAD, OK, GOOD
}
