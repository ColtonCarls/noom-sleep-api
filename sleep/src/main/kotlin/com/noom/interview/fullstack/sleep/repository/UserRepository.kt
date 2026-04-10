package com.noom.interview.fullstack.sleep.repository

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

/**
 * Lightweight data access layer for the `users` table.
 * Only provides an existence check — full user management is out of scope.
 */
@Repository
class UserRepository(
    private val jdbcTemplate: NamedParameterJdbcTemplate
) {

    /**
     * Checks whether a user with the given ID exists.
     *
     * @param userId the ID to look up.
     * @return `true` if the user exists, `false` otherwise.
     */
    fun existsById(userId: Long): Boolean {
        val sql = "SELECT 1 FROM users WHERE id = :userId"
        val params = MapSqlParameterSource("userId", userId)

        return try {
            jdbcTemplate.queryForObject(sql, params, Int::class.java)
            true
        } catch (e: EmptyResultDataAccessException) {
            false
        }
    }
}
