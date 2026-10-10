package com.cinebook;

import com.cinebook.model.Movie;
import com.cinebook.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MovieApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MovieRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void cleanTable() {
        jdbc.update("DELETE FROM MOVIE");
    }

    private static String json(String title, String genre, String price) {
        return "{\"title\":" + title + ",\"genre\":" + genre + ",\"ticketPrice\":" + price + "}";
    }

    private long insertViaApi(String title, String genre, String price) throws Exception {
        MvcResult result = mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"" + title + "\"", "\"" + genre + "\"", price)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        String id = body.replaceAll(".*\"movieId\":(\\d+).*", "$1");
        return Long.parseLong(id);
    }

    // ---------------- INSERT ----------------

    @Test
    void insertStoresMovieAndReturnsGeneratedId() throws Exception {
        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"  Inception \"", "\"Sci-Fi\"", "250.5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.movie.movieId").isNumber())
                .andExpect(jsonPath("$.movie.title").value("Inception"))
                .andExpect(jsonPath("$.message", containsString("added successfully")));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MOVIE", Integer.class)).isEqualTo(1);
        Movie stored = jdbc.queryForObject("SELECT movie_id, title, genre, ticket_price FROM MOVIE",
                (rs, n) -> new Movie(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBigDecimal(4)));
        assertThat(stored.title()).isEqualTo("Inception");
        assertThat(stored.genre()).isEqualTo("Sci-Fi");
        assertThat(stored.ticketPrice()).isEqualByComparingTo("250.50");
    }

    @Test
    void insertRejectsMissingAndBlankFields() throws Exception {
        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"   \"", "null", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.genre").exists())
                .andExpect(jsonPath("$.errors.ticketPrice").exists());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MOVIE", Integer.class)).isZero();
    }

    @Test
    void insertRejectsInvalidPrices() throws Exception {
        for (String price : new String[]{"0", "-5", "10000.01", "12.345"}) {
            mvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("\"Up\"", "\"Animation\"", price)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.ticketPrice").exists());
        }
        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"Up\"", "\"Animation\"", "\"abc\"")))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MOVIE", Integer.class)).isZero();
    }

    @Test
    void insertRejectsTooLongAndIllegalText() throws Exception {
        String longTitle = "A".repeat(101);
        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"" + longTitle + "\"", "\"Drama\"", "100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
        mvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"<script>\"", "\"Drama1\"", "100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.genre").exists());
    }

    @Test
    void sqlInjectionAttemptIsStoredAsPlainText() throws Exception {
        String title = "Robert'); DROP TABLE MOVIE; --";
        long id = insertViaApi(title, "Drama", "99");
        assertThat(repository.findById(id)).get().extracting(Movie::title).isEqualTo(title);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MOVIE", Integer.class)).isEqualTo(1);
    }

    @Test
    void insertRejectsMalformedJsonAndWrongContentType() throws Exception {
        mvc.perform(post("/api/movies").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/movies").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ---------------- UPDATE ----------------

    @Test
    void updateChangesExistingMovie() throws Exception {
        long id = insertViaApi("Avatar", "Sci-Fi", "200");

        mvc.perform(put("/api/movies/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"Avatar: The Way of Water\"", "\"Adventure\"", "320.75")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("updated successfully")));

        Movie m = repository.findById(id).orElseThrow();
        assertThat(m.title()).isEqualTo("Avatar: The Way of Water");
        assertThat(m.genre()).isEqualTo("Adventure");
        assertThat(m.ticketPrice()).isEqualByComparingTo(new BigDecimal("320.75"));
    }

    @Test
    void updateReturns404ForUnknownId() throws Exception {
        mvc.perform(put("/api/movies/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"X\"", "\"Drama\"", "10")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("999999")));
    }

    @Test
    void updateRejectsInvalidIdAndInvalidBody() throws Exception {
        long id = insertViaApi("Heat", "Crime", "150");
        for (String badId : new String[]{"0", "-3", "abc", "1.5"}) {
            mvc.perform(put("/api/movies/" + badId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("\"X\"", "\"Drama\"", "10")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.movieId").exists());
        }
        mvc.perform(put("/api/movies/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("\"\"", "\"Crime\"", "-1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.ticketPrice").exists());
        assertThat(repository.findById(id).orElseThrow().title()).isEqualTo("Heat");
    }

    // ---------------- DELETE ----------------

    @Test
    void deleteRemovesMovie() throws Exception {
        long id = insertViaApi("Jaws", "Thriller", "120");
        mvc.perform(delete("/api/movies/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("deleted successfully")));
        assertThat(repository.findById(id)).isEmpty();

        mvc.perform(delete("/api/movies/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteRejectsInvalidId() throws Exception {
        mvc.perform(delete("/api/movies/0")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/movies/abc")).andExpect(status().isBadRequest());
    }

    // ---------------- Scope ----------------

    @Test
    void listingAndFetchingAreNotExposed() throws Exception {
        mvc.perform(get("/api/movies")).andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/movies/1")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void frontendIsServed() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(containsString("Delete Movie")));
    }
}
