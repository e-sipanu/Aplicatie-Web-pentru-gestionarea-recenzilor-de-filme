/**
 * Clasa repository pentru operatii JDBC cu filme
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.repository;

import com.example.filme.dto.FilmDetaliatDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class FilmJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public FilmJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FilmDetaliatDto> search(String q, String field, String sort, String dir) {

        // 1) whitelist pentru ORDER BY (foarte important)
        String orderBy;
        switch (sort == null ? "" : sort) {
            case "titlu" -> orderBy = "f.Titlu";
            case "an" -> orderBy = "f.An_aparitie";
            case "durata" -> orderBy = "f.Durata_min";
            case "rating" -> orderBy = "f.Rating_mediu";
            default -> orderBy = "f.Rating_mediu";
        }

        String orderDir = "asc".equalsIgnoreCase(dir) ? "ASC" : "DESC";

        // 2) query de bază (nu folosim STRING_AGG aici ca să evităm probleme)
        String baseSql = """
                SELECT
                    f.ID_Film,
                    f.Titlu,
                    f.An_aparitie,
                    f.Durata_min,
                    f.Descriere,
                    f.Rating_mediu
                FROM Film f
                WHERE 1=1
                """;

        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> params = new ArrayList<>();

        // 3) filtrare după criteriu (SQL manual)
        if (q != null && !q.trim().isEmpty()) {
            String pattern = "%" + q.trim() + "%";
            switch (field == null ? "titlu" : field) {
                case "gen" -> {
                    sql.append("""
                            AND EXISTS (
                                SELECT 1
                                FROM Film_Gen fg
                                JOIN Gen g ON g.ID_Gen = fg.ID_Gen
                                WHERE fg.ID_Film = f.ID_Film
                                  AND g.Denumire_gen LIKE ?
                            )
                            """);
                    params.add(pattern);
                }
                case "actor" -> {
                    sql.append("""
                            AND EXISTS (
                                SELECT 1
                                FROM Film_Actor fa
                                JOIN Actor a ON a.ID_Actor = fa.ID_Actor
                                WHERE fa.ID_Film = f.ID_Film
                                  AND (a.Nume LIKE ? OR a.Prenume LIKE ?)
                            )
                            """);
                    params.add(pattern);
                    params.add(pattern);
                }
                case "regizor" -> {
                    sql.append("""
                            AND EXISTS (
                                SELECT 1
                                FROM Film_Regizor fr
                                JOIN Regizor r ON r.ID_Regizor = fr.ID_Regizor
                                WHERE fr.ID_Film = f.ID_Film
                                  AND (r.Nume LIKE ? OR r.Prenume LIKE ?)
                            )
                            """);
                    params.add(pattern);
                    params.add(pattern);
                }
                case "titlu" -> {
                    sql.append(" AND f.Titlu LIKE ? ");
                    params.add(pattern);
                }
                default -> {
                    sql.append(" AND f.Titlu LIKE ? ");
                    params.add(pattern);
                }
            }
        }

        // 4) ORDER BY sigur
        sql.append(" ORDER BY ").append(orderBy).append(" ").append(orderDir);

        if (!"f.Titlu".equalsIgnoreCase(orderBy)) {
            sql.append(", f.Titlu ASC");
        }

        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> {
                    FilmDetaliatDto f = new FilmDetaliatDto();
                    f.setIdFilm(rs.getInt("ID_Film"));
                    f.setTitlu(rs.getString("Titlu"));
                    f.setAnAparitie(rs.getInt("An_aparitie"));
                    f.setDurataMin(rs.getInt("Durata_min"));
                    f.setDescriere(rs.getString("Descriere"));

                    Object ratingObj = rs.getObject("Rating_mediu");
                    if (ratingObj != null) {
                        f.setRatingMediu(rs.getDouble("Rating_mediu"));
                    } else {
                        f.setRatingMediu(null);
                    }

                    return f;
                });

    }

    public FilmDetaliatDto findByIdSimple(int filmId) {
        String sql = """
                SELECT
                    ID_Film, Titlu, An_aparitie, Durata_min, Descriere, Rating_mediu
                FROM Film
                WHERE ID_Film = ?
                """;

        List<FilmDetaliatDto> list = jdbcTemplate.query(sql, new Object[] { filmId }, (rs, rowNum) -> {
            FilmDetaliatDto f = new FilmDetaliatDto();
            f.setIdFilm(rs.getInt("ID_Film"));
            f.setTitlu(rs.getString("Titlu"));
            f.setAnAparitie(rs.getInt("An_aparitie"));
            f.setDurataMin(rs.getInt("Durata_min"));
            f.setDescriere(rs.getString("Descriere"));

            Object ratingObj = rs.getObject("Rating_mediu");
            f.setRatingMediu(ratingObj == null ? null : rs.getDouble("Rating_mediu"));
            return f;
        });

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Găsește un film cu toate detaliile: genuri, actori, regizori
     */
    public FilmDetaliatDto findByIdDetailed(int filmId) {
        String sql = """
                SELECT
                    f.ID_Film,
                    f.Titlu,
                    f.An_aparitie,
                    f.Durata_min,
                    f.Descriere,
                    f.Rating_mediu,
                    (SELECT STRING_AGG(g.Denumire_gen, ', ')
                     FROM Film_Gen fg
                     JOIN Gen g ON g.ID_Gen = fg.ID_Gen
                     WHERE fg.ID_Film = f.ID_Film) AS Genuri,
                    (SELECT STRING_AGG(CONCAT(a.Nume, ' ', a.Prenume), ', ')
                     FROM Film_Actor fa
                     JOIN Actor a ON a.ID_Actor = fa.ID_Actor
                     WHERE fa.ID_Film = f.ID_Film) AS Actori,
                    (SELECT STRING_AGG(CONCAT(r.Nume, ' ', r.Prenume), ', ')
                     FROM Film_Regizor fr
                     JOIN Regizor r ON r.ID_Regizor = fr.ID_Regizor
                     WHERE fr.ID_Film = f.ID_Film) AS Regizori
                FROM Film f
                WHERE f.ID_Film = ?
                """;

        List<FilmDetaliatDto> list = jdbcTemplate.query(sql, new Object[] { filmId }, (rs, rowNum) -> {
            FilmDetaliatDto f = new FilmDetaliatDto();
            f.setIdFilm(rs.getInt("ID_Film"));
            f.setTitlu(rs.getString("Titlu"));
            f.setAnAparitie(rs.getInt("An_aparitie"));
            f.setDurataMin(rs.getInt("Durata_min"));
            f.setDescriere(rs.getString("Descriere"));

            Object ratingObj = rs.getObject("Rating_mediu");
            f.setRatingMediu(ratingObj == null ? null : rs.getDouble("Rating_mediu"));

            f.setGenuri(rs.getString("Genuri"));
            f.setActori(rs.getString("Actori"));
            f.setRegizori(rs.getString("Regizori"));

            return f;
        });

        return list.isEmpty() ? null : list.get(0);
    }

    public List<FilmDetaliatDto> getFilmePopulare(int minRecenzii, double minRating) {
        String sql = """
                SELECT
                    f.ID_Film,
                    f.Titlu,
                    f.An_aparitie,
                    f.Durata_min,
                    f.Descriere,
                    f.Rating_mediu,
                    -- SELECT ÎN SELECT pentru afișare/sortare (echivalentul COUNT din GROUP BY)
                    (SELECT COUNT(*) 
                     FROM Recenzie r 
                     WHERE r.ID_Film = f.ID_Film) AS NumarRecenzii
                FROM Film f
                WHERE 
                    -- Filtrare rating (existentă pe coloana tabelului)
                    f.Rating_mediu >= ?
                    -- SELECT ÎN SELECT pentru filtrare (echivalentul HAVING)
                    AND (SELECT COUNT(*) FROM Recenzie r WHERE r.ID_Film = f.ID_Film) >= ?
                ORDER BY NumarRecenzii DESC, f.Rating_mediu DESC, f.Titlu ASC
                """;

        // Atenție la ordinea parametrilor: primul ? este rating, al doilea ? este count
        return jdbcTemplate.query(sql, new Object[] { minRating, minRecenzii }, (rs, rowNum) -> {
            FilmDetaliatDto f = new FilmDetaliatDto();
            f.setIdFilm(rs.getInt("ID_Film"));
            f.setTitlu(rs.getString("Titlu"));
            f.setAnAparitie(rs.getInt("An_aparitie"));
            f.setDurataMin(rs.getInt("Durata_min"));
            f.setDescriere(rs.getString("Descriere"));

            Object ratingObj = rs.getObject("Rating_mediu");
            f.setRatingMediu(ratingObj == null ? null : rs.getDouble("Rating_mediu"));

            // Nota: NumarRecenzii este folosit doar pentru sortare/filtrare în SQL
            // și nu este mapat în DTO-ul actual, la fel ca în varianta originală.

            return f;
        });
    }



}
