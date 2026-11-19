package edu.cit.alvarado.phishmenot.common;

import java.time.Instant;

/** Uniform JSON error body returned by the API. */
public record ApiError(int status, String error, String message, String path, Instant timestamp) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now());
    }
}
