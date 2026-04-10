package com.noom.interview.fullstack.sleep.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource

@ExtendWith(MockitoExtension::class)
@DisplayName("UserRepository")
class UserRepositoryTest {

    @Mock
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @InjectMocks
    private lateinit var repository: UserRepository

    @Test
    fun `existsById returns true when user exists`() {
        whenever(jdbcTemplate.queryForObject(any<String>(), any<SqlParameterSource>(), eq(Int::class.java)))
            .thenReturn(1)

        assertThat(repository.existsById(1L)).isTrue()

        verify(jdbcTemplate).queryForObject(
            argThat<String> { contains("SELECT 1 FROM users") },
            argThat<SqlParameterSource> { getValue("userId") == 1L },
            eq(Int::class.java)
        )
    }

    @Test
    fun `existsById returns false when user does not exist`() {
        whenever(jdbcTemplate.queryForObject(any<String>(), any<SqlParameterSource>(), eq(Int::class.java)))
            .thenThrow(EmptyResultDataAccessException(1))

        assertThat(repository.existsById(999L)).isFalse()
    }
}
