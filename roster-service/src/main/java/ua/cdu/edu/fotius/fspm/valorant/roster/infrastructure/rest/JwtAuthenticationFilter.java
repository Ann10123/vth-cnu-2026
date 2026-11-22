package ua.cdu.edu.fotius.fspm.valorant.roster.infrastructure.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import ua.cdu.edu.fotius.fspm.valorant.roster.application.JwtTokenService;

/**
 * Authenticates incoming API requests by validating the {@code Authorization: Bearer <token>} header.
 *
 * <p>The login endpoint ({@code POST /api/v1/auth/login}) is intentionally excluded — it is how
 * clients obtain a token, so it must be publicly accessible.</p>
 *
 * <p>Token validation is stateless: the filter checks the token prefix
 * ({@code jwt-token-for-}) via {@link JwtTokenService}. No database lookup, no signing keys,
 * nothing secret is stored or committed.</p>
 */
@Filter(patterns = "/api/v1/**")
@Singleton
public class JwtAuthenticationFilter implements HttpServerFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService tokenService;

    @Inject
    public JwtAuthenticationFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(
            HttpRequest<?> request,
            ServerFilterChain chain) {

        // Login is public — clients need this endpoint to obtain a token
        if (request.getPath().equals(LOGIN_PATH)) {
            return chain.proceed(request);
        }

        // Every other /api/v1/** route requires a valid Bearer token.
        // The frontend's mock fallback must never reach a live backend —
        // in normal operation login issues a real "jwt-token-for-<tag>" token first.
        String authHeader = request.getHeaders().get(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Auth filter - missing/invalid Authorization header for {}", request.getPath());
            return Mono.just(HttpResponse.unauthorized()
                    .body(new RosterController.ErrorDto("MISSING_TOKEN",
                            "Authorization header with Bearer token is required")));
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!tokenService.isValidToken(token)) {
            log.warn("Auth filter - invalid token for {}", request.getPath());
            return Mono.just(HttpResponse.unauthorized()
                    .body(new RosterController.ErrorDto("INVALID_TOKEN",
                            "Token is invalid or expired")));
        }

        log.info("Auth filter - authenticated: {} {}", request.getMethod(), request.getPath());
        // Token is valid — proceed to the controller
        return chain.proceed(request);
    }
}
