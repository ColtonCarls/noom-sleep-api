package com.noom.interview.fullstack.sleep.exception

/** Thrown when a requested sleep log or sleep data does not exist. */
class SleepLogNotFoundException(message: String) : RuntimeException(message)
