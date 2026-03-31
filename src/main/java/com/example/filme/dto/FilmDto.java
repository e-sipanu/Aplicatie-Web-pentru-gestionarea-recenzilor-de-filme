/**
 * Clasa DTO pentru filme cu informatii de baza
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.dto;

public class FilmDto {

    private Integer idFilm;
    private String titlu;
    private Integer anAparitie;
    private Integer durataMin;
    private String descriere;
    private Double ratingMediu; // atenție: Double, nu BigDecimal

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
}
