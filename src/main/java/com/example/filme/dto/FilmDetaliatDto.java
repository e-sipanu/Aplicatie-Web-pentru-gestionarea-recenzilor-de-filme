/**
 * Clasa DTO pentru filme cu informatii detaliate (genuri, actori, regizori)
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.dto;

public class FilmDetaliatDto {

    private Integer idFilm;
    private String titlu;
    private Integer anAparitie;
    private Integer durataMin;
    private String descriere;
    private Double ratingMediu;

    private String genuri; // ex: "Drama, Actiune"
    private String actori; // ex: "Tim Robbins, Morgan Freeman"
    private String regizori; // ex: "Christopher Nolan"

    public Integer getIdFilm() {
        return idFilm;
    }

    public void setIdFilm(Integer idFilm) {
        this.idFilm = idFilm;
    }

    public String getTitlu() {
        return titlu;
    }

    public void setTitlu(String titlu) {
        this.titlu = titlu;
    }

    public Integer getAnAparitie() {
        return anAparitie;
    }

    public void setAnAparitie(Integer anAparitie) {
        this.anAparitie = anAparitie;
    }

    public Integer getDurataMin() {
        return durataMin;
    }

    public void setDurataMin(Integer durataMin) {
        this.durataMin = durataMin;
    }

    public String getDescriere() {
        return descriere;
    }

    public void setDescriere(String descriere) {
        this.descriere = descriere;
    }

    public Double getRatingMediu() {
        return ratingMediu;
    }

    public void setRatingMediu(Double ratingMediu) {
        this.ratingMediu = ratingMediu;
    }

    public String getGenuri() {
        return genuri;
    }

    public void setGenuri(String genuri) {
        this.genuri = genuri;
    }

    public String getActori() {
        return actori;
    }

    public void setActori(String actori) {
        this.actori = actori;
    }

    public String getRegizori() {
        return regizori;
    }

    public void setRegizori(String regizori) {
        this.regizori = regizori;
    }
}
