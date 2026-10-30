package com.inspiration_work.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class JwtSecurity {

    private JwtSecurity() {
    }

    public static JwtDecoder decoder(String jwkSetUri, String issuer, String audience) {
        requireConfiguration(jwkSetUri, issuer, audience);
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(validator(issuer, audience));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> validator(String issuer, String audience) {
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), jwt ->
                jwt.getAudience().contains(audience)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null)));
    }

    public static void requireConfiguration(String jwkSetUri, String issuer, String audience) {
        if (!StringUtils.hasText(jwkSetUri) || !StringUtils.hasText(issuer) || !StringUtils.hasText(audience)) {
            throw new IllegalStateException("JWT issuer, JWKS URI and audience must be configured");
        }
    }

    public static JwtAuthenticationConverter authenticationConverter(String rolesClaim) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopes.convert(jwt));
            Object roles = jwt.getClaim(rolesClaim);
            if (roles instanceof Collection<?> values) {
                values.stream().filter(String.class::isInstance).map(String.class::cast)
                        .forEach(role -> addRole(authorities, role));
            } else if (roles instanceof String value) {
                for (String role : value.split("[,\\s]+")) {
                    addRole(authorities, role);
                }
            }
            return authorities;
        });
        return converter;
    }

    private static void addRole(Set<GrantedAuthority> authorities, String role) {
        if (StringUtils.hasText(role)) {
            authorities.add(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
        }
    }
}
