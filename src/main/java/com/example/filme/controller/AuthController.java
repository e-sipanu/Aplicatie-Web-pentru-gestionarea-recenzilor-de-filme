/**
 * Clasa controller REST pentru autentificare si inregistrare utilizatori
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.controller;

import com.example.filme.dto.LoginRequest;
import com.example.filme.dto.RegisterRequest;
import com.example.filme.entity.Utilizator;
import com.example.filme.exception.InvalidRequestException;
import com.example.filme.exception.UserAlreadyExistsException;
import com.example.filme.repository.UtilizatorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilizatorRepository utilizatorRepository;

    public AuthController(UtilizatorRepository utilizatorRepository) {
        this.utilizatorRepository = utilizatorRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        // 1. VALIDĂRI DE INPUT
        if (request.getNumeUtilizator() == null || request.getNumeUtilizator().trim().isEmpty()) {
            throw new InvalidRequestException("Numele de utilizator nu poate fi gol.");
        }

        if (request.getParola() == null || request.getParola().trim().isEmpty()) {
            throw new InvalidRequestException("Parola nu poate fi goală.");
        }

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new InvalidRequestException("Email-ul nu poate fi gol.");
        }

        String username = request.getNumeUtilizator().trim();
        String email = request.getEmail().trim();
        String parola = request.getParola().trim();

        if (username.length() < 3 || username.length() > 50) {
            throw new InvalidRequestException("Numele de utilizator trebuie să aibă între 3 și 50 de caractere.");
        }

        if (parola.length() < 5) {
            throw new InvalidRequestException("Parola trebuie să aibă cel puțin 5 caractere.");
        }

        if (!email.contains("@") || !email.contains(".")) {
            throw new InvalidRequestException("Email-ul nu este valid.");
        }

        // 2. VERIFICĂM EXISTENȚA USERNAME / EMAIL (SQL manual)
        Optional<Utilizator> existingUser = utilizatorRepository.findByUsernameNative(username);
        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("Numele de utilizator este deja folosit.");
        }

        if (utilizatorRepository.findByEmailNative(email).isPresent()) {
            throw new UserAlreadyExistsException("Email-ul este deja folosit.");
        }

        // 3. INSERȚIE CU SQL MANUAL
        utilizatorRepository.insertUserNative(
                username,
                parola,
                email,
                LocalDate.now());

        // putem întoarce un mesaj simplu sau un mic JSON
        return ResponseEntity.ok("Utilizator creat cu succes!");
    }

    // =======================
    // LOGIN
    // =======================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // 1. VALIDĂRI SIMPLE DE INPUT
        if (request.getNumeUtilizator() == null || request.getNumeUtilizator().trim().isEmpty()) {
            throw new InvalidRequestException("Numele de utilizator nu poate fi gol.");
        }

        if (request.getParola() == null || request.getParola().trim().isEmpty()) {
            throw new InvalidRequestException("Parola nu poate fi goală.");
        }

        String username = request.getNumeUtilizator().trim();
        String parola = request.getParola().trim();

        // 2. CĂUTĂM ÎN BAZA DE DATE CU SQL MANUAL
        Optional<Utilizator> userOpt = utilizatorRepository.loginNative(username, parola);

        if (userOpt.isEmpty()) {
            // aici putem fie să aruncăm o excepție custom, fie să întoarcem 401
            return ResponseEntity
                    .status(401)
                    .body("Credentiale invalide.");
        }

        // 3. LOGIN OK
        return ResponseEntity.ok("Login reușit pentru utilizatorul: " + username);
    }

    /**
     * SCHIMBARE PAROLĂ (UPDATE pentru Utilizator)
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestParam int userId,
            @RequestParam String parolaVeche,
            @RequestParam String parolaNoua) {

        // 1. VALIDĂRI
        if (parolaVeche == null || parolaVeche.trim().isEmpty()) {
            throw new InvalidRequestException("Parola veche nu poate fi goală.");
        }

        if (parolaNoua == null || parolaNoua.trim().isEmpty()) {
            throw new InvalidRequestException("Parola nouă nu poate fi goală.");
        }

        if (parolaNoua.trim().length() < 5) {
            throw new InvalidRequestException("Parola nouă trebuie să aibă cel puțin 5 caractere.");
        }

        // 2. VERIFICĂM DACĂ UTILIZATORUL EXISTĂ
        Optional<Utilizator> userOpt = utilizatorRepository.findByIdNative(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Utilizatorul nu a fost găsit.");
        }

        // 3. UPDATE PAROLĂ (verifică parola veche în query)
        int rowsUpdated = utilizatorRepository.updatePassword(userId, parolaVeche.trim(), parolaNoua.trim());

        if (rowsUpdated == 0) {
            return ResponseEntity.status(401).body("Parola veche este incorectă.");
        }

        return ResponseEntity.ok("Parola a fost schimbată cu succes!");
    }

    /**
     * ȘTERGERE CONT (DELETE pentru Utilizator)
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@RequestParam int userId) {

        // 1. VERIFICĂM DACĂ UTILIZATORUL EXISTĂ
        Optional<Utilizator> userOpt = utilizatorRepository.findByIdNative(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Utilizatorul nu a fost găsit.");
        }

        // 2. DELETE UTILIZATOR (recenziile vor fi șterse automat prin CASCADE)
        utilizatorRepository.deleteUser(userId);

        return ResponseEntity.ok("Contul a fost șters cu succes!");
    }
}
