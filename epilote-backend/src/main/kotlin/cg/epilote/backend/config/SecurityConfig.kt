package cg.epilote.backend.config

import cg.epilote.backend.auth.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(private val jwtAuthFilter: JwtAuthFilter) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // change-password DOIT être authentifié au niveau URL — l'ordre des
                // matchers est significatif (Spring Security évalue le premier qui
                // matche). Sans cette règle, /api/auth/** = permitAll laisserait
                // passer la requête au niveau filtre et seul @PreAuthorize protège,
                // ce qui est inconsistant avec /api/super-admin/** etc.
                it.requestMatchers("/api/auth/change-password").authenticated()
                it.requestMatchers("/api/auth/**").permitAll()
                it.requestMatchers("/actuator/health").permitAll()
                it.requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")
                it.requestMatchers("/api/groupes/**").hasAnyRole("SUPER_ADMIN", "ADMIN_GROUPE")
                it.requestMatchers("/api/schools/**").hasAnyRole("SUPER_ADMIN", "ADMIN_GROUPE")
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
