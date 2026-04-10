package com.noom.interview.fullstack.sleep.exception

/** Thrown when a referenced user does not exist. */
class UserNotFoundException(message: String) : RuntimeException(message)
