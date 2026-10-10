package com.cinebook.service;

import com.cinebook.dto.MovieRequest;
import com.cinebook.exception.InvalidMovieIdException;
import com.cinebook.exception.MovieNotFoundException;
import com.cinebook.model.Movie;
import com.cinebook.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional
    public Movie insert(MovieRequest request) {
        return movieRepository.insert(new Movie(null, request.title(), request.genre(),
                request.ticketPrice().setScale(2, RoundingMode.UNNECESSARY)));
    }

    @Transactional
    public Movie update(long movieId, MovieRequest request) {
        requireValidId(movieId);
        Movie movie = new Movie(movieId, request.title(), request.genre(),
                request.ticketPrice().setScale(2, RoundingMode.UNNECESSARY));
        if (movieRepository.update(movie) == 0) {
            throw new MovieNotFoundException(movieId);
        }
        return movie;
    }

    @Transactional
    public void delete(long movieId) {
        requireValidId(movieId);
        if (movieRepository.deleteById(movieId) == 0) {
            throw new MovieNotFoundException(movieId);
        }
    }

    private static void requireValidId(long movieId) {
        if (movieId <= 0) {
            throw new InvalidMovieIdException("Movie ID must be a positive whole number.");
        }
    }
}
