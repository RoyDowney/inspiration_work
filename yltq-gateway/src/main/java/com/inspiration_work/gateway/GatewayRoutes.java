package com.inspiration_work.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutes {

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder,
                        @Value("${services.admin.url}") String adminUrl,
                        @Value("${services.payment.url}") String paymentUrl) {
        return builder.routes()
                .route("admin", route -> route.path("/adminservice/**").uri(adminUrl))
                .route("payment", route -> route.path("/paymentservice/**").uri(paymentUrl))
                .build();
    }
}
