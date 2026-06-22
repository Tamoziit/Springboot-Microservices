package com.tamojit.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// Applying authentication filter (by extending AbstractGatewayFilterFactory & overriding apply() method) to all requests passing through the API Gateway
// "JwtValidation" - tag to specify Jwt filter to applied to a route-group in application.yml --> "JwtValidation"GatewayFilterFactory builds that filter tag logic to be applied to each route to the route group
@Component
public class JwtValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    private final WebClient webClient; // to make HTTP requests to auth-service

    public JwtValidationGatewayFilterFactory(WebClient.Builder webClientBuilder, @Value("${auth.service.url}") String authServiceUrl) {
        this.webClient = webClientBuilder.baseUrl(authServiceUrl).build();
    }

    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> { // chaining filters over the HTTTP req/res exchange by webClient
            String token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (token == null || !token.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete(); // early return
            }

            // making a GET request to auth-service:4005/validate with Authorization header set to get a response (without a res.body)
            return webClient.get()
                .uri("/validate")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .toBodilessEntity()
                .then(chain.filter(exchange)); // continue req down the chain [like next() func.]
        });
    }
}
