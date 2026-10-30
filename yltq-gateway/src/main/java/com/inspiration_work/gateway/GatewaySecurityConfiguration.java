package com.inspiration_work.gateway;

import com.inspiration_work.security.JwtSecurity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                  @Value("${security.jwt.roles-claim}") String rolesClaim,
                                                  @Value("${security.roles.upload}") String uploadRole,
                                                  @Value("${security.roles.payment-delete}") String deleteRole) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.POST, "/adminservice/execl/operate/reportImportExcelAnalysis")
                        .hasAuthority("ROLE_" + uploadRole)
                        .pathMatchers(HttpMethod.DELETE, "/paymentservice/payment/removeById/*")
                        .hasAuthority("ROLE_" + deleteRole)
                        .pathMatchers(HttpMethod.GET, "/paymentservice/payment/**").authenticated()
                        .anyExchange().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(
                        new ReactiveJwtAuthenticationConverterAdapter(JwtSecurity.authenticationConverter(rolesClaim)))))
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(@Value("${security.jwt.jwk-set-uri}") String jwkSetUri,
                                  @Value("${security.jwt.issuer}") String issuer,
                                  @Value("${security.jwt.audience}") String audience) {
        JwtSecurity.requireConfiguration(jwkSetUri, issuer, audience);
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtSecurity.validator(issuer, audience));
        return decoder;
    }
}
