/**
 * Clasa repository pentru operatii cu utilizatori
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.repository;

import com.example.filme.entity.Utilizator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

public interface UtilizatorRepository extends JpaRepository<Utilizator, Integer> {

        // 1. Căutăm după nume utilizator (pt. register)
        @Query(value = "SELECT * FROM Utilizator WHERE Nume_utilizator = :username", nativeQuery = true)
        Optional<Utilizator> findByUsernameNative(@Param("username") String username);

        // 2. Căutăm după email (pt. register)
        @Query(value = "SELECT * FROM Utilizator WHERE Email = :email", nativeQuery = true)
        Optional<Utilizator> findByEmailNative(@Param("email") String email);

        // 3. Login (o păstrăm pentru mai târziu, dar e tot SQL manual)
        @Query(value = "SELECT * FROM Utilizator WHERE Nume_utilizator = :username AND Parola = :parola", nativeQuery = true)
        Optional<Utilizator> loginNative(@Param("username") String username,
                        @Param("parola") String parola);

        // 4. INSERT manual (folosit de REGISTER)
        @Modifying
        @Transactional
        @Query(value = """
                        INSERT INTO Utilizator (Nume_utilizator, Parola, Email, Data_inregistrarii)
                        VALUES (:username, :parola, :email, :dataInreg)
                        """, nativeQuery = true)
        void insertUserNative(@Param("username") String username,
                        @Param("parola") String parola,
                        @Param("email") String email,
                        @Param("dataInreg") LocalDate dataInregistrarii);

        // 5. UPDATE parola utilizator (pentru schimbare parolă)
        @Modifying
        @Transactional
        @Query(value = """
                        UPDATE Utilizator
                        SET Parola = :parolaNoua
                        WHERE ID_Utilizator = :userId AND Parola = :parolaVeche
                        """, nativeQuery = true)
        int updatePassword(@Param("userId") int userId,
                        @Param("parolaVeche") String parolaVeche,
                        @Param("parolaNoua") String parolaNoua);

        // 6. DELETE utilizator (pentru ștergere cont)
        @Modifying
        @Transactional
        @Query(value = "DELETE FROM Utilizator WHERE ID_Utilizator = :userId", nativeQuery = true)
        void deleteUser(@Param("userId") int userId);

        // 7. SELECT by ID (pentru verificare existență)
        @Query(value = "SELECT * FROM Utilizator WHERE ID_Utilizator = :userId", nativeQuery = true)
        Optional<Utilizator> findByIdNative(@Param("userId") int userId);



    // Returnează un Map cu cheile "NR_RECENZII" și "MEDIE_RATING"
    @Query(value = """
            SELECT 
                (SELECT COUNT(*) 
                 FROM Recenzie r 
                 WHERE r.ID_Utilizator = u.ID_Utilizator) AS Nr_Recenzii,
                 
                (SELECT ROUND(AVG(CAST(r.Rating AS FLOAT)), 2) 
                 FROM Recenzie r 
                 WHERE r.ID_Utilizator = u.ID_Utilizator) AS Medie_Rating
            FROM Utilizator u
            WHERE u.ID_Utilizator = :userId
            """, nativeQuery = true)
    java.util.Map<String, Object> getUserStatistics(@Param("userId") int userId);



}
