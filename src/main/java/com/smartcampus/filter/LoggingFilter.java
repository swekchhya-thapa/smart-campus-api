package com.smartcampus.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * API Observability Filter — logs every incoming request and outgoing response.
 *
 * Implements both ContainerRequestFilter and ContainerResponseFilter in a single
 * class, keeping the cross-cutting concern in one place.
 *
 * Benefits over manual Logger.info() in every resource method:
 *  - Single point of change: update logging format once, applies everywhere.
 *  - Guaranteed coverage: new resource methods are automatically logged.
 *  - Separation of concerns: resource classes focus purely on business logic.
 *  - Consistent format: all log entries share the same structure.
 *  - Easier to toggle: disable logging by removing @Provider without touching business code.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Called before the request reaches the resource method.
     * Logs the HTTP method and full request URI.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info(String.format(
                "[REQUEST]  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()
        ));
    }

    /**
     * Called after the resource method has produced a response.
     * Logs the HTTP status code sent back to the client.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info(String.format(
                "[RESPONSE] %s %s → %d %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus(),
                responseContext.getStatusInfo().getReasonPhrase()
        ));
    }
}
