package cg.epilote.backend.config

import cg.epilote.backend.admin.AdminRealtimeInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val adminRealtimeInterceptor: AdminRealtimeInterceptor
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(adminRealtimeInterceptor)
            .addPathPatterns("/api/super-admin/**")
    }
}
