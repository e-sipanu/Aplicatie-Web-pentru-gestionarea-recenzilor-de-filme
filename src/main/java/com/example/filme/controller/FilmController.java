/**
 * Clasa controller REST pentru operatii cu filme (listare, filtrare, sortare)
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.controller;

import com.example.filme.dto.FilmDto;
import com.example.filme.repository.FilmJdbcRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.example.filme.dto.FilmDetaliatDto;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/filme")
public class FilmController {

    private final JdbcTemplate jdbcTemplate;
    private final FilmJdbcRepository filmJdbcRepository;

    public FilmController(JdbcTemplate jdbcTemplate, FilmJdbcRepository filmJdbcRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.filmJdbcRepository = filmJdbcRepository;
    }

    // Mapper care transformă un rând din ResultSet într-un FilmDto
    private final RowMapper<FilmDto> filmRowMapper = new RowMapper<FilmDto>() {
        @Override
        public FilmDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            FilmDto f = new FilmDto();
            f.setIdFilm(rs.getInt("ID_Film"));
            f.setTitlu(rs.getString("Titlu"));
            f.setAnAparitie(rs.getInt("An_aparitie"));
            f.setDurataMin(rs.getInt("Durata_min"));
            f.setDescriere(rs.getString("Descriere"));

            // Rating_mediu poate fi NULL în DB
            Object ratingObj = rs.getObject("Rating_mediu");
            if (ratingObj != null) {
                f.setRatingMediu(rs.getDouble("Rating_mediu"));
            } else {
                f.setRatingMediu(null);
            }

            return f;
        }
    };

    // 1. Toate filmele din baza de date, ordonate în SQL după rating desc, titlu
    // asc
    @GetMapping
    public ResponseEntity<List<FilmDto>> getToateFilmele() {
        String sql = """
                SELECT ID_Film, Titlu, An_aparitie, Durata_min, Descriere, Rating_mediu
                FROM Film
                ORDER BY Rating_mediu DESC, Titlu ASC
                """;

        List<FilmDto> filme = jdbcTemplate.query(sql, filmRowMapper);
        return ResponseEntity.ok(filme);
    }

    // 2. Filme filtrate + sortate în Java (bonus: comparare, sortare, filtrare)
    @GetMapping("/filtrate")
    public ResponseEntity<List<FilmDto>> getFilmeFiltrate(
            @RequestParam(required = false) String titlu,
            @RequestParam(required = false) Integer anMin,
            @RequestParam(required = false) Integer anMax,
            @RequestParam(required = false) Integer durataMax,
            @RequestParam(required = false) Double ratingMin,
            @RequestParam(required = false, defaultValue = "rating") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        String sql = """
                SELECT ID_Film, Titlu, An_aparitie, Durata_min, Descriere, Rating_mediu
                FROM Film
                """;

        List<FilmDto> toateFilmele = jdbcTemplate.query(sql, filmRowMapper);

        var stream = toateFilmele.stream();

        // FILTRARE

        if (titlu != null && !titlu.trim().isEmpty()) {
            String t = titlu.trim().toLowerCase();
            stream = stream.filter(f -> f.getTitlu() != null &&
                    f.getTitlu().toLowerCase().contains(t));
        }

        if (anMin != null) {
            stream = stream.filter(f -> f.getAnAparitie() != null &&
                    f.getAnAparitie() >= anMin);
        }

        if (anMax != null) {
            stream = stream.filter(f -> f.getAnAparitie() != null &&
                    f.getAnAparitie() <= anMax);
        }

        if (durataMax != null) {
            stream = stream.filter(f -> f.getDurataMin() != null &&
                    f.getDurataMin() <= durataMax);
        }

        if (ratingMin != null) {
            stream = stream.filter(f -> f.getRatingMediu() != null &&
                    f.getRatingMediu() >= ratingMin);
        }

        // SORTARE – aici era eroarea ta cu Comparator

        Comparator<FilmDto> comparator;

        switch (sortBy) {
            case "titlu" -> comparator = Comparator.comparing(
                    FilmDto::getTitlu,
                    Comparator.nullsLast(String::compareToIgnoreCase));
            case "an" -> comparator = Comparator.comparing(
                    FilmDto::getAnAparitie,
                    Comparator.nullsLast(Integer::compareTo));
            case "durata" -> comparator = Comparator.comparing(
                    FilmDto::getDurataMin,
                    Comparator.nullsLast(Integer::compareTo));
            case "rating" -> comparator = Comparator.comparing(
                    FilmDto::getRatingMediu,
                    Comparator.nullsLast(Double::compareTo) // acum e Double, deci ok
                );
            default -> comparator = Comparator.comparing(
                    FilmDto::getTitlu,
                    Comparator.nullsLast(String::compareToIgnoreCase));
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            comparator = comparator.reversed();
        }

        List<FilmDto> rezultat = stream
                .sorted(comparator)
                .collect(Collectors.toList());

        return ResponseEntity.ok(rezultat);
    }

    @GetMapping("/cauta-detaliat")
    public ResponseEntity<List<FilmDetaliatDto>> cautaDetaliat(
            @RequestParam(required = false) String titlu,
            @RequestParam(required = false) String gen,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String regizor) {
        String baseSql = """
                SELECT
                    f.ID_Film,
                    f.Titlu,
                    f.An_aparitie,
                    f.Durata_min,
                    f.Descriere,
                    f.Rating_mediu,
                    STRING_AGG( g.Denumire_gen, ', ') AS Genuri,
                    STRING_AGG( CONCAT(a.Nume, ' ', a.Prenume), ', ') AS Actori,
                    STRING_AGG( CONCAT(r.Nume, ' ', r.Prenume), ', ') AS Regizori
                FROM Film f
                LEFT JOIN Film_Gen fg      ON fg.ID_Film = f.ID_Film
                LEFT JOIN Gen g            ON g.ID_Gen = fg.ID_Gen
                LEFT JOIN Film_Actor fa    ON fa.ID_Film = f.ID_Film
                LEFT JOIN Actor a          ON a.ID_Actor = fa.ID_Actor
                LEFT JOIN Film_Regizor fr  ON fr.ID_Film = f.ID_Film
                LEFT JOIN Regizor r        ON r.ID_Regizor = fr.ID_Regizor
                WHERE 1=1
                """;

        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> params = new ArrayList<>();

        if (titlu != null && !titlu.trim().isEmpty()) {
            sql.append(" AND f.Titlu LIKE ? ");
            params.add("%" + titlu.trim() + "%");
        }

        if (gen != null && !gen.trim().isEmpty()) {
            sql.append(" AND g.Denumire_gen LIKE ? ");
            params.add("%" + gen.trim() + "%");
        }

        if (actor != null && !actor.trim().isEmpty()) {
            sql.append(" AND (a.Nume LIKE ? OR a.Prenume LIKE ?) ");
            String p = "%" + actor.trim() + "%";
            params.add(p);
            params.add(p);
        }

        if (regizor != null && !regizor.trim().isEmpty()) {
            sql.append(" AND (r.Nume LIKE ? OR r.Prenume LIKE ?) ");
            String p = "%" + regizor.trim() + "%";
            params.add(p);
            params.add(p);
        }

        sql.append("""
                GROUP BY
                    f.ID_Film, f.Titlu, f.An_aparitie, f.Durata_min, f.Descriere, f.Rating_mediu
                ORDER BY f.Rating_mediu DESC, f.Titlu ASC
                """);

        List<FilmDetaliatDto> rezultat = jdbcTemplate.query(
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
                    }

                    f.setGenuri(rs.getString("Genuri"));
                    f.setActori(rs.getString("Actori"));
                    f.setRegizori(rs.getString("Regizori"));

                    return f;
                });

        return ResponseEntity.ok(rezultat);
    }

    /**
     * Endpoint pentru filme populare (interogare complexă cu HAVING)
     * Returnează filme care au cel puțin un număr minim de recenzii și un rating
     * minim
     */
    @GetMapping("/populare")
    public ResponseEntity<List<FilmDetaliatDto>> getFilmePopulare(
            @RequestParam(defaultValue = "3") int minRecenzii,
            @RequestParam(defaultValue = "7.0") double minRating) {

        List<FilmDetaliatDto> rezultat = filmJdbcRepository.getFilmePopulare(minRecenzii, minRating);

        return ResponseEntity.ok(rezultat);
    }

}
