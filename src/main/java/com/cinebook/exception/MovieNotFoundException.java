package com.cinebook.exception;

public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(long movieId) {
        super("No movie found with ID " + movieId + ".");
    }
}
