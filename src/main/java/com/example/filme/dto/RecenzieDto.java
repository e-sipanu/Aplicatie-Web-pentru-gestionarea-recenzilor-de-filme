/**
 * Clasa DTO pentru recenzii
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.dto;

import java.time.LocalDate;

public class RecenzieDto {
    private Integer idRecenzie;
    private Integer idFilm;
    private Integer idUtilizator;
    private String numeUtilizator; // din JOIN cu Utilizator
    private String titluRecenzie;
    private String textRecenzie;
    private Integer rating;
    private LocalDate dataPostarii;

    public Integer getIdRecenzie() {
        return idRecenzie;
    }

    public void setIdRecenzie(Integer idRecenzie) {
        this.idRecenzie = idRecenzie;
    }

    public Integer getIdFilm() {
        return idFilm;
    }

    public void setIdFilm(Integer idFilm) {
        this.idFilm = idFilm;
    }

    public Integer getIdUtilizator() {
        return idUtilizator;
    }

    public void setIdUtilizator(Integer idUtilizator) {
        this.idUtilizator = idUtilizator;
    }

    public String getNumeUtilizator() {
        return numeUtilizator;
    }

    public void setNumeUtilizator(String numeUtilizator) {
        this.numeUtilizator = numeUtilizator;
    }

    public String getTitluRecenzie() {
        return titluRecenzie;
    }

    public void setTitluRecenzie(String titluRecenzie) {
        this.titluRecenzie = titluRecenzie;
    }

    public String getTextRecenzie() {
        return textRecenzie;
    }

    public void setTextRecenzie(String textRecenzie) {
        this.textRecenzie = textRecenzie;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public LocalDate getDataPostarii() {
        return dataPostarii;
    }

    public void setDataPostarii(LocalDate dataPostarii) {
        this.dataPostarii = dataPostarii;
    }
}
