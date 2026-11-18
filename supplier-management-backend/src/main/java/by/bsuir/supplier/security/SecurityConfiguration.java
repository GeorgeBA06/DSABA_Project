package by.bsuir.supplier.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain filters(HttpSecurity http, SessionAuthenticationConverter currentUser) throws Exception {
        http.csrf(c->c.disable()).cors(c->{}).httpBasic(c->c.disable()).formLogin(c->c.disable())
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers(HttpMethod.POST,"/api/v1/auth/login","/api/v1/auth/refresh").permitAll()
            .requestMatchers("/actuator/health","/actuator/health/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
            .anyRequest().authenticated())
            .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(currentUser)))
            .exceptionHandling(e->{
                e.authenticationEntryPoint((req, res, err)->res.sendError(401,"Требуется авторизация"));
                e.accessDeniedHandler((req, res, err)->res.sendError(403,"Доступ запрещён"));
            });
        return http.build();
    }
    @Bean CorsConfigurationSource corsSource(@Value("${security.cors.allowed-origins:http://localhost:5173}") String origins) {
        CorsConfiguration conf = new CorsConfiguration();
        conf.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s->!s.isEmpty()).toList());
        conf.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        conf.setAllowedHeaders(List.of("Authorization","Content-Type"));
        conf.setExposedHeaders(List.of("Location"));
        conf.setAllowCredentials(false);
        conf.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", conf);
        return source;
    }
}
