package com.freenow.pspgatewayservice.config.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
@Profile(value = ["freenow"])
class GlobalAuthenticationConfig : GlobalAuthenticationConfigurerAdapter() {

@Value("\${api.user.username}")
private lateinit var apiUsername: String

@Value("\${api.user.password}")
private lateinit var apiPassword: String

@Value("\${api.authority}")
private lateinit var authority: String

@Bean
    fun passwordEncoder(): BCryptPasswordEncoder {
        return BCryptPasswordEncoder()
        }

        override fun init(auth: AuthenticationManagerBuilder) {
        auth.inMemoryAuthentication()
        .passwordEncoder(passwordEncoder())
        .withUser(apiUsername)
        .password(passwordEncoder().encode(apiPassword))
        .authorities(authority)
        }
        }
