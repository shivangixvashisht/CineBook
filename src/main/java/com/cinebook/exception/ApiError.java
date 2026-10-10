package com.cinebook.exception;

import java.util.Map;

/**
 * JSON error body returned to the frontend.
 */
public record ApiError(int status, String message, Map<String, String> errors) {
}
