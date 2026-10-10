package com.cinebook.repository;

import com.cinebook.model.Movie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

/**
 * JDBC access to the MOVIE table. Every statement uses '?' placeholders,
 * so user input is never concatenated into SQL.
 */
@Repository
public class MovieRepository {

    private static final String INSERT_SQL =
            "INSERT INTO MOVIE (title, genre, ticket_price) VALUES (?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE MOVIE SET title = ?, genre = ?, ticket_price = ? WHERE movie_id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM MOVIE WHERE movie_id = ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT movie_id, title, genre, ticket_price FROM MOVIE WHERE movie_id = ?";

    private final JdbcTemplate jdbcTemplate;

    public MovieRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Inserts a movie and returns it with the generated movie_id. */
    public Movie insert(Movie movie) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"movie_id"});
            ps.setString(1, movie.title());
            ps.setString(2, movie.genre());
            ps.setBigDecimal(3, movie.ticketPrice());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return a generated movie_id.");
        }
        return new Movie(key.longValue(), movie.title(), movie.genre(), movie.ticketPrice());
    }

    /** Updates a movie; returns the number of affected rows (0 or 1). */
    public int update(Movie movie) {
        return jdbcTemplate.update(UPDATE_SQL,
                movie.title(), movie.genre(), movie.ticketPrice(), movie.movieId());
    }

    /** Deletes a movie; returns the number of affected rows (0 or 1). */
    public int deleteById(long movieId) {
        return jdbcTemplate.update(DELETE_SQL, movieId);
    }

    /** Internal lookup used to confirm writes (not exposed as a user operation). */
    public Optional<Movie> findById(long movieId) {
        return jdbcTemplate.query(FIND_BY_ID_SQL, (rs, rowNum) -> new Movie(
                        rs.getLong("movie_id"),
                        rs.getString("title"),
                        rs.getString("genre"),
                        rs.getBigDecimal("ticket_price")),
                movieId).stream().findFirst();
    }
}
