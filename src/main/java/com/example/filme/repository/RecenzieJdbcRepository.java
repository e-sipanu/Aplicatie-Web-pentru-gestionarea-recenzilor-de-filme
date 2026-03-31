/**
 * Clasa repository pentru operatii JDBC cu recenzii
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.repository;

import com.example.filme.dto.RecenzieDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Repository
public class RecenzieJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public RecenzieJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * LISTARE recenzii pentru un film (join cu Utilizator ca să afișăm username)
     */
    public List<RecenzieDto> findByFilmId(int filmId) {
        String sql = """
                SELECT
                    r.ID_Recenzie,
                    r.ID_Film,
                    r.ID_Utilizator,
                    u.Nume_utilizator,
                    r.Titlu_recenzie,
                    r.Text_recenzie,
                    r.Rating,
                    r.Data_postarii
                FROM Recenzie r
                JOIN Utilizator u ON u.ID_Utilizator = r.ID_Utilizator
                WHERE r.ID_Film = ?
                ORDER BY r.Data_postarii DESC, r.ID_Recenzie DESC
                """;

        return jdbcTemplate.query(sql, new Object[] { filmId }, (rs, rowNum) -> {
            RecenzieDto dto = new RecenzieDto();
            dto.setIdRecenzie(rs.getInt("ID_Recenzie"));
            dto.setIdFilm(rs.getInt("ID_Film"));
            dto.setIdUtilizator(rs.getInt("ID_Utilizator"));
            dto.setNumeUtilizator(rs.getString("Nume_utilizator"));
            dto.setTitluRecenzie(rs.getString("Titlu_recenzie"));
            dto.setTextRecenzie(rs.getString("Text_recenzie"));

            Object ratingObj = rs.getObject("Rating");
            dto.setRating(ratingObj == null ? null : rs.getInt("Rating"));

            Date d = rs.getDate("Data_postarii");
            dto.setDataPostarii(d == null ? null : d.toLocalDate());
            return dto;
        });
    }

    /**
     * Găsește o recenzie după ID (pentru editare)
     */
    public RecenzieDto findById(int reviewId) {
        String sql = """
                SELECT
                    r.ID_Recenzie,
                    r.ID_Film,
                    r.ID_Utilizator,
                    u.Nume_utilizator,
                    r.Titlu_recenzie,
                    r.Text_recenzie,
                    r.Rating,
                    r.Data_postarii
                FROM Recenzie r
                JOIN Utilizator u ON u.ID_Utilizator = r.ID_Utilizator
                WHERE r.ID_Recenzie = ?
                """;

        List<RecenzieDto> list = jdbcTemplate.query(sql, new Object[] { reviewId }, (rs, rowNum) -> {
            RecenzieDto dto = new RecenzieDto();
            dto.setIdRecenzie(rs.getInt("ID_Recenzie"));
            dto.setIdFilm(rs.getInt("ID_Film"));
            dto.setIdUtilizator(rs.getInt("ID_Utilizator"));
            dto.setNumeUtilizator(rs.getString("Nume_utilizator"));
            dto.setTitluRecenzie(rs.getString("Titlu_recenzie"));
            dto.setTextRecenzie(rs.getString("Text_recenzie"));

            Object ratingObj = rs.getObject("Rating");
            dto.setRating(ratingObj == null ? null : rs.getInt("Rating"));

            Date d = rs.getDate("Data_postarii");
            dto.setDataPostarii(d == null ? null : d.toLocalDate());
            return dto;
        });

        return list.isEmpty() ? null : list.get(0);
    }

    /** INSERT recenzie: întoarce ID-ul creat */
    public int insert(int userId, int filmId, String titlu, String text, int rating) {

        // Validări simple (ca să nu explodeze DB / să ai puncte extra)
        if (rating < 1 || rating > 10) {
            throw new IllegalArgumentException("Ratingul trebuie să fie între 1 și 10.");
        }

        String sql = """
                INSERT INTO Recenzie (ID_Utilizator, ID_Film, Titlu_recenzie, Text_recenzie, Rating, Data_postarii)
                VALUES (?, ?, ?, ?, ?, GETDATE())
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setInt(2, filmId);
            ps.setString(3, titlu);
            ps.setString(4, text);
            ps.setInt(5, rating);
            return ps;
        }, keyHolder);

        // SQL Server returnează cheia în KeyHolder
        return Objects.requireNonNull(keyHolder.getKey()).intValue();
    }

    /**
     * UPDATE recenzie (doar owner-ul ar trebui să poată; verificarea o facem în
     * controller)
     */
    public int update(int reviewId, String titlu, String text, int rating) {
        if (rating < 1 || rating > 10) {
            throw new IllegalArgumentException("Ratingul trebuie să fie între 1 și 10.");
        }

        String sql = """
                UPDATE Recenzie
                SET Titlu_recenzie = ?,
                    Text_recenzie = ?,
                    Rating = ?
                WHERE ID_Recenzie = ?
                """;

        return jdbcTemplate.update(sql, titlu, text, rating, reviewId);
    }

    /** DELETE recenzie */
    public int delete(int reviewId) {
        String sql = "DELETE FROM Recenzie WHERE ID_Recenzie = ?";
        return jdbcTemplate.update(sql, reviewId);
    }

    /** Găsește owner-ul recenziei (pt. autorizare simplă) */
    public Integer findOwnerUserId(int reviewId) {
        String sql = "SELECT ID_Utilizator FROM Recenzie WHERE ID_Recenzie = ?";
        List<Integer> ids = jdbcTemplate.query(sql, new Object[] { reviewId }, (rs, rowNum) -> rs.getInt(1));
        return ids.isEmpty() ? null : ids.get(0);
    }

    /**
     * Updates film rating incrementally based on new review rating.
     * Each new review changes the rating by 1/100th of the difference:
     * new_rating = current_rating + (review_rating - current_rating) / 100
     * Result is rounded to 2 decimal places.
     */
    public void recalcRatingMediuFilm(int filmId, int newReviewRating) {
        String sql = """
                UPDATE Film
                SET Rating_mediu = ROUND(Rating_mediu + (CAST(? AS FLOAT) - Rating_mediu) / 100.0, 2)
                WHERE ID_Film = ?
                """;
        jdbcTemplate.update(sql, newReviewRating, filmId);
    }

    /**
     * Decreases film rating incrementally based on deleted review rating.
     * Inverse formula for deletion: rating -= (deleted_rating - rating) / 100
     * This maintains consistency with incremental additions.
     * Result is rounded to 2 decimal places.
     */
    public void decrementRatingMediuFilm(int filmId, int deletedReviewRating) {
        String sql = """
                UPDATE Film
                SET Rating_mediu = ROUND(Rating_mediu - (CAST(? AS FLOAT) - Rating_mediu) / 100.0, 2)
                WHERE ID_Film = ?
                """;
        jdbcTemplate.update(sql, deletedReviewRating, filmId);
    }

    public int countUserReviewsForFilm(int userId, int filmId) {
        String sql = "SELECT COUNT(*) FROM Recenzie WHERE ID_Utilizator = ? AND ID_Film = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, userId, filmId);
    }

    /**
     * Clasă helper pentru a returna ID film și rating
     */
    public static class FilmRatingPair {
        public int filmId;
        public int rating;

        public FilmRatingPair(int filmId, int rating) {
            this.filmId = filmId;
            this.rating = rating;
        }
    }

    /**
     * Găsește toate recenziile utilizatorului cu ID-ul filmului și rating-ul
     * (folosit pentru decrementare corectă a rating-ului la ștergere cont)
     */
    public List<FilmRatingPair> getUserReviewsWithRatings(int userId) {
        String sql = "SELECT ID_Film, Rating FROM Recenzie WHERE ID_Utilizator = ?";
        return jdbcTemplate.query(sql, new Object[] { userId },
                (rs, rowNum) -> new FilmRatingPair(rs.getInt("ID_Film"), rs.getInt("Rating")));
    }

    /**
     * Găsește toate ID-urile filmelor pentru care utilizatorul are recenzii
     * (folosit pentru recalculare rating la ștergere cont)
     */
    public List<Integer> getFilmIdsForUserReviews(int userId) {
        String sql = "SELECT DISTINCT ID_Film FROM Recenzie WHERE ID_Utilizator = ?";
        return jdbcTemplate.query(sql, new Object[] { userId }, (rs, rowNum) -> rs.getInt("ID_Film"));
    }

    /**
     * DELETE toate recenziile unui utilizator (folosit înainte de ștergere cont)
     */
    public int deleteAllUserReviews(int userId) {
        String sql = "DELETE FROM Recenzie WHERE ID_Utilizator = ?";
        return jdbcTemplate.update(sql, userId);
    }
}
