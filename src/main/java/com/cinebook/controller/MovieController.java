package com.cinebook.controller;

import com.cinebook.dto.MovieRequest;
import com.cinebook.model.Movie;
import com.cinebook.service.MovieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes exactly three operations: insert, update by ID, delete by ID.
 */
@RestController
@RequestMapping(path = "/api/movies", produces = MediaType.APPLICATION_JSON_VALUE)
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> insert(@Valid @RequestBody MovieRequest request) {
        Movie saved = movieService.insert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Movie added successfully with ID " + saved.movieId() + ".",
                "movie", saved));
    }

    @PutMapping(path = "/{movieId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> update(@PathVariable long movieId,
                                                      @Valid @RequestBody MovieRequest request) {
        Movie updated = movieService.update(movieId, request);
        return ResponseEntity.ok(Map.of(
                "message", "Movie with ID " + movieId + " updated successfully.",
                "movie", updated));
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable long movieId) {
        movieService.delete(movieId);
        return ResponseEntity.ok(Map.of(
                "message", "Movie with ID " + movieId + " deleted successfully."));
    }
}
