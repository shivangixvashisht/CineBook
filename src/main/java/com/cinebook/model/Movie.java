package com.cinebook.model;

import java.math.BigDecimal;

/**
 * A row of the MOVIE table.
 */
public record Movie(Long movieId, String title, String genre, BigDecimal ticketPrice) {
}
