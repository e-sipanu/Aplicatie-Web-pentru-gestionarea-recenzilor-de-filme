/**
 * Clasa entitate pentru utilizatori
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Utilizator")
public class Utilizator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_Utilizator")
    private Integer id;

    @Column(name = "Nume_utilizator", nullable = false, length = 50, unique = true)
    private String numeUtilizator;

    @Column(name = "Parola", nullable = false, length = 255)
    private String parola;

    @Column(name = "Email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "Data_inregistrarii")
    private LocalDate dataInregistrarii;

    // GETTERS & SETTERS

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumeUtilizator() {
        return numeUtilizator;
    }

    public void setNumeUtilizator(String numeUtilizator) {
        this.numeUtilizator = numeUtilizator;
    }

    public String getParola() {
        return parola;
    }

    public void setParola(String parola) {
        this.parola = parola;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getDataInregistrarii() {
        return dataInregistrarii;
    }

    public void setDataInregistrarii(LocalDate dataInregistrarii) {
        this.dataInregistrarii = dataInregistrarii;
    }
}
