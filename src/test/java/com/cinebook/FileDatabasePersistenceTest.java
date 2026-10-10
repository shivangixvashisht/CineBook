package com.cinebook;

import com.cinebook.dto.MovieRequest;
import com.cinebook.model.Movie;
import com.cinebook.repository.MovieRepository;
import com.cinebook.service.MovieService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the application twice against the same file-based H2 database and
 * verifies that data written in the first run is still there in the second.
 */
class FileDatabasePersistenceTest {

    @TempDir
    Path tempDir;

    private ConfigurableApplicationContext start(String url) {
        SpringApplication app = new SpringApplication(CineBookApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        return app.run("--spring.datasource.url=" + url);
    }

    @Test
    void dataSurvivesApplicationRestart() {
        String url = "jdbc:h2:file:" + tempDir.resolve("cinebook").toAbsolutePath() + ";DB_CLOSE_ON_EXIT=FALSE";

        long id;
        try (ConfigurableApplicationContext ctx = start(url)) {
            id = ctx.getBean(MovieService.class)
                    .insert(new MovieRequest("Interstellar", "Sci-Fi", new BigDecimal("275.00")))
                    .movieId();
        }

        assertThat(Files.exists(tempDir.resolve("cinebook.mv.db"))).isTrue();

        try (ConfigurableApplicationContext ctx = start(url)) {
            Movie movie = ctx.getBean(MovieRepository.class).findById(id).orElseThrow();
            assertThat(movie.title()).isEqualTo("Interstellar");
            assertThat(movie.ticketPrice()).isEqualByComparingTo("275.00");
        }
    }
}
