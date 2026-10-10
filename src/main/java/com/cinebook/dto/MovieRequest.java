package com.cinebook.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body used by both the insert and the update operation.
 */
public record MovieRequest(

        @NotBlank(message = "Title is required.")
        @Size(max = 100, message = "Title must be at most 100 characters.")
        @Pattern(regexp = "^[\\p{L}\\p{N}][\\p{L}\\p{N} .,:;!?'&()\\-]*$",
                message = "Title must start with a letter or digit and may only contain letters, digits, spaces and . , : ; ! ? ' & ( ) -")
        String title,

        @NotBlank(message = "Genre is required.")
        @Size(max = 50, message = "Genre must be at most 50 characters.")
        @Pattern(regexp = "^[\\p{L}][\\p{L} &/\\-]*$",
                message = "Genre must start with a letter and may only contain letters, spaces, & / and -")
        String genre,

        @NotNull(message = "Ticket price is required.")
        @DecimalMin(value = "0.01", message = "Ticket price must be greater than 0.")
        @DecimalMax(value = "10000.00", message = "Ticket price must not exceed 10000.00.")
        @Digits(integer = 5, fraction = 2, message = "Ticket price may have at most 2 decimal places.")
        BigDecimal ticketPrice
) {

    /** Trims text fields so validation and storage see normalized values. */
    public MovieRequest {
        title = title == null ? null : title.trim();
        genre = genre == null ? null : genre.trim();
    }
}
