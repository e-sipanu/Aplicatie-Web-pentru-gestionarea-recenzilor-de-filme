/**
 * Clasa controller pentru paginile de autentificare si inregistrare (server-side rendering)
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.controller.page;

import com.example.filme.entity.Utilizator;
import com.example.filme.repository.UtilizatorRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class AuthPageController {

    private final UtilizatorRepository utilizatorRepository;
    private final com.example.filme.repository.RecenzieJdbcRepository recenzieJdbcRepository;

    public AuthPageController(UtilizatorRepository utilizatorRepository,
            com.example.filme.repository.RecenzieJdbcRepository recenzieJdbcRepository) {
        this.utilizatorRepository = utilizatorRepository;
        this.recenzieJdbcRepository = recenzieJdbcRepository;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(name = "err", required = false) String err, Model model) {
        model.addAttribute("error", err != null);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam("numeUtilizator") String numeUtilizator,
            @RequestParam("parola") String parola,
            HttpSession session) {

        if (numeUtilizator == null || numeUtilizator.trim().isEmpty()
                || parola == null || parola.trim().isEmpty()) {
            return "redirect:/login?err=1";
        }

        Optional<Utilizator> userOpt = utilizatorRepository.loginNative(numeUtilizator.trim(), parola.trim());
        if (userOpt.isEmpty()) {
            return "redirect:/login?err=1";
        }

        Utilizator user = userOpt.get();
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getNumeUtilizator());

        return "redirect:/filme";
    }

    @GetMapping("/register")
    public String registerPage(@RequestParam(name = "err", required = false) String err,
            @RequestParam(name = "ok", required = false) String ok,
            Model model) {
        model.addAttribute("error", err != null);
        model.addAttribute("success", ok != null);
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam("numeUtilizator") String numeUtilizator,
            @RequestParam("parola") String parola,
            @RequestParam("email") String email) {

        // validări simple
        if (numeUtilizator == null || numeUtilizator.trim().length() < 3)
            return "redirect:/register?err=1";
        if (parola == null || parola.trim().length() < 5)
            return "redirect:/register?err=1";
        if (email == null || !email.contains("@"))
            return "redirect:/register?err=1";

        String u = numeUtilizator.trim();
        String e = email.trim();
        String p = parola.trim();

        if (utilizatorRepository.findByUsernameNative(u).isPresent())
            return "redirect:/register?err=1";
        if (utilizatorRepository.findByEmailNative(e).isPresent())
            return "redirect:/register?err=1";

        utilizatorRepository.insertUserNative(u, p, e, LocalDate.now());
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /**
     /**
     * Pagina de setări utilizator + Statistici (Select in Select)
     */
    @GetMapping("/setari")
    public String settingsPage(HttpSession session, Model model,
                               @RequestParam(name = "success", required = false) String success,
                               @RequestParam(name = "err", required = false) String err) {

        Integer userId = (Integer) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        if (userId == null) {
            return "redirect:/login";
        }

        // 1. Fetch User Stats (Select in Select Feature)
        java.util.Map<String, Object> stats = utilizatorRepository.getUserStatistics(userId);

        // Extract values safely (handling nulls if user has no reviews)
        Object nrRecenzii = stats.get("Nr_Recenzii"); // Key case matches SQL alias usually, or uppercase depending on DB
        Object medieRating = stats.get("Medie_Rating");

        // Note: Spring Data JPA native query maps often use uppercase keys or original alias
        // Let's ensure we pass something valid
        model.addAttribute("nrRecenzii", nrRecenzii != null ? nrRecenzii : 0);
        model.addAttribute("medieRating", medieRating != null ? medieRating : "N/A");

        // 2. Standard attributes
        model.addAttribute("username", username);
        model.addAttribute("userId", userId);
        model.addAttribute("success", success != null);
        model.addAttribute("error", err != null);

        return "setari";
    }

    /**
     * Procesare schimbare parolă
     */
    @PostMapping("/setari/change-password")
    public String changePassword(HttpSession session,
            @RequestParam("parolaVeche") String parolaVeche,
            @RequestParam("parolaNoua") String parolaNoua,
            @RequestParam("parolaConfirmare") String parolaConfirmare) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        // Validări
        if (parolaNoua == null || parolaNoua.trim().length() < 5) {
            return "redirect:/setari?err=1";
        }

        if (!parolaNoua.equals(parolaConfirmare)) {
            return "redirect:/setari?err=2";
        }

        // UPDATE parolă
        int rowsUpdated = utilizatorRepository.updatePassword(userId, parolaVeche.trim(), parolaNoua.trim());

        if (rowsUpdated == 0) {
            return "redirect:/setari?err=3"; // Parolă veche incorectă
        }

        return "redirect:/setari?success=1";
    }

    /**
     * Procesare ștergere cont
     */
    @PostMapping("/setari/delete-account")
    public String deleteAccount(HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        // 1. Obținem toate recenziile utilizatorului cu rating-urile lor (pentru
        // decrementare corectă)
        java.util.List<com.example.filme.repository.RecenzieJdbcRepository.FilmRatingPair> reviews = recenzieJdbcRepository
                .getUserReviewsWithRatings(userId);

        // 2. DELETE toate recenziile utilizatorului (pentru a evita eroarea de foreign
        // key)
        recenzieJdbcRepository.deleteAllUserReviews(userId);

        // 3. Decrementăm rating-ul pentru fiecare recenzie ștearsă (exact ca la
        // ștergere normală)
        for (com.example.filme.repository.RecenzieJdbcRepository.FilmRatingPair review : reviews) {
            recenzieJdbcRepository.decrementRatingMediuFilm(review.filmId, review.rating);
        }

        // 4. DELETE utilizator
        utilizatorRepository.deleteUser(userId);

        // 5. Invalidare sesiune
        session.invalidate();

        return "redirect:/login";
    }
}
