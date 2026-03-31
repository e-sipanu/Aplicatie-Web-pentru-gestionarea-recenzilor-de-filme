/**
 * Clasa controller pentru paginile de filme si recenzii (server-side rendering)
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.controller.page;

import jakarta.servlet.http.HttpSession;
import com.example.filme.dto.FilmDetaliatDto;
import com.example.filme.repository.FilmJdbcRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.filme.dto.RecenzieDto;
import com.example.filme.repository.RecenzieJdbcRepository;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/filme")
public class FilmePageController {

    private final FilmJdbcRepository filmRepo;
    private final RecenzieJdbcRepository recenzieRepo;

    public FilmePageController(FilmJdbcRepository filmRepo, RecenzieJdbcRepository recenzieRepo) {
        this.filmRepo = filmRepo;
        this.recenzieRepo = recenzieRepo;
    }

    @GetMapping
    public String pageFilme(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "field", required = false, defaultValue = "titlu") String field,
            @RequestParam(name = "sort", required = false, defaultValue = "rating") String sort,
            @RequestParam(name = "dir", required = false, defaultValue = "desc") String dir,
            Model model,
            HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        List<FilmDetaliatDto> filme = filmRepo.search(q, field, sort, dir);

        model.addAttribute("filme", filme);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("field", field);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);
        model.addAttribute("username", session.getAttribute("username"));

        return "filme";
    }

    // Pagina filmului (detalii + recenzii + form)
    @GetMapping("/{id}")
    public String filmPage(@PathVariable("id") int id,
            @RequestParam(name = "err", required = false) String err,
            @RequestParam(name = "ok", required = false) String ok,
            Model model,
            HttpSession session) {

        if (session.getAttribute("userId") == null) {
            return "redirect:/login";
        }

        FilmDetaliatDto film = filmRepo.findByIdDetailed(id);
        if (film == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Film inexistent.");
        }

        List<RecenzieDto> recenzii = recenzieRepo.findByFilmId(id);

        model.addAttribute("film", film);
        model.addAttribute("recenzii", recenzii);
        model.addAttribute("username", session.getAttribute("username"));
        model.addAttribute("error", err != null);
        model.addAttribute("success", ok != null);

        return "film"; // templates/film.html
    }

    // Adăugare recenzie (TEXT obligatoriu)
    @PostMapping("/{id}/recenzii")
    public String addRecenzie(@PathVariable("id") int id,
            @RequestParam(name = "titluRecenzie", required = false) String titlu,
            @RequestParam("textRecenzie") String text,
            @RequestParam("rating") Integer rating,
            HttpSession session) {

        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) {
            return "redirect:/login";
        }
        int userId = (Integer) userIdObj;

        // validări
        if (text == null || text.trim().isEmpty()) {
            return "redirect:/filme/" + id + "?err=1";
        }
        if (rating == null || rating < 1 || rating > 10) {
            return "redirect:/filme/" + id + "?err=1";
        }

        // Verificăm că filmul există
        if (filmRepo.findByIdSimple(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Film inexistent.");
        }

        // Verificăm dacă utilizatorul are deja o recenzie pentru acest film
        int existingCount = recenzieRepo.countUserReviewsForFilm(userId, id);
        if (existingCount > 0) {
            // Utilizatorul are deja o recenzie - returnăm eroare
            return "redirect:/filme/" + id + "?err=2";
        }

        String titluSafe = (titlu == null) ? null : titlu.trim();
        String textSafe = text.trim();

        recenzieRepo.insert(userId, id, titluSafe, textSafe, rating);
        recenzieRepo.recalcRatingMediuFilm(id, rating);

        return "redirect:/filme/" + id + "?ok=1";
    }

    /**
     * Pagina pentru filme populare (interogare complexă cu HAVING)
     */
    @GetMapping("/filme-populare")
    public String filmePopulare(
            @RequestParam(required = false, defaultValue = "1") int minRecenzii,
            @RequestParam(required = false, defaultValue = "7.0") double minRating,
            Model model,
            HttpSession session) {

        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", username);
        model.addAttribute("minRecenzii", minRecenzii);
        model.addAttribute("minRating", minRating);

        return "filme-populare";
    }

    // Ștergere recenzie (doar owner-ul)
    @PostMapping("/{filmId}/recenzii/{reviewId}/delete")
    public String deleteRecenzie(@PathVariable("filmId") int filmId,
            @PathVariable("reviewId") int reviewId,
            HttpSession session) {

        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) {
            return "redirect:/login";
        }
        int userId = (Integer) userIdObj;

        // Verificăm că recenzia aparține utilizatorului
        Integer ownerId = recenzieRepo.findOwnerUserId(reviewId);
        if (ownerId == null || ownerId != userId) {
            // Nu are dreptul să șteargă această recenzie
            return "redirect:/filme/" + filmId + "?err=3";
        }

        // Obținem rating-ul recenziei ÎNAINTE de ștergere pentru actualizare
        // incrementală
        var recenzie = recenzieRepo.findById(reviewId);
        Integer oldRating = (recenzie != null) ? recenzie.getRating() : null;

        // Ștergem recenzia
        recenzieRepo.delete(reviewId);

        // Actualizăm rating-ul incremental (scădem efectul recenziei șterse)
        // Formula inversă: rating -= (review_rating - rating) / 100
        if (oldRating != null) {
            recenzieRepo.decrementRatingMediuFilm(filmId, oldRating);
        }

        return "redirect:/filme/" + filmId + "?ok=2";
    }

    // Editare recenzie (doar owner-ul)
    @PostMapping("/{filmId}/recenzii/{reviewId}/edit")
    public String editRecenzie(@PathVariable("filmId") int filmId,
            @PathVariable("reviewId") int reviewId,
            @RequestParam(name = "titluRecenzie", required = false) String titlu,
            @RequestParam("textRecenzie") String text,
            @RequestParam("rating") Integer rating,
            HttpSession session) {

        Object userIdObj = session.getAttribute("userId");
        if (userIdObj == null) {
            return "redirect:/login";
        }
        int userId = (Integer) userIdObj;

        // Verificăm că recenzia aparține utilizatorului
        Integer ownerId = recenzieRepo.findOwnerUserId(reviewId);
        if (ownerId == null || ownerId != userId) {
            return "redirect:/filme/" + filmId + "?err=3";
        }

        // Validări
        if (text == null || text.trim().isEmpty()) {
            return "redirect:/filme/" + filmId + "?err=1";
        }
        if (rating == null || rating < 1 || rating > 10) {
            return "redirect:/filme/" + filmId + "?err=1";
        }

        String titluSafe = (titlu == null) ? null : titlu.trim();
        String textSafe = text.trim();

        // Update recenzie
        recenzieRepo.update(reviewId, titluSafe, textSafe, rating);

        // Actualizăm rating-ul filmului cu noul rating
        recenzieRepo.recalcRatingMediuFilm(filmId, rating);

        return "redirect:/filme/" + filmId + "?ok=3";
    }
}
