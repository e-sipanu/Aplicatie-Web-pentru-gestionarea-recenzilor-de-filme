# Film & Review Management Application

![Movie App](movie.png)

A comprehensive web application built with **Java (Spring Boot)** and **SQL Server** for managing films and user reviews. This project implements a full-featured system with secure authentication, advanced search and filtering, and an optimized incremental rating calculation algorithm.

---

## Features

### Authentication & Authorization
* Complete HTTP session-based authentication system.
* Secure user registration with unique username and email validation.
* Protected routes that automatically redirect unauthenticated users to the login page.

### Film Management & Discovery
* Browse a curated list of films displayed in responsive cards.
* View detailed film pages including metadata (year, duration, description), genres, actors, and directors.
* **Advanced Search:** Filter films by Title, Genre, Actor, or Director.
* **Smart Sorting:** Order results by rating, title, release year, or duration (ascending/descending).

### Review System (CRUD)
* **Create:** Users can post reviews (1-10 rating, text, optional title). Duplicate reviews per user/film are prevented.
* **Read:** All reviews for a film are displayed with the author's name, post date, and rating.
* **Update:** Inline editing for users to update their own reviews seamlessly.
* **Delete:** Secure deletion of personal reviews with JavaScript confirmation dialogues.

### Advanced Incremental Rating Algorithm
Unlike traditional systems that recalculate the average rating from scratch on every change (O(n) complexity), this application implements an **incremental rating update formula**.
* **Formula:** `new_rating = current_rating + (review_rating - current_rating) / 100`
* **Performance:** O(1) time complexity - it requires only a single UPDATE query.
* Ratings evolve gradually without sudden jumps, ensuring consistency during insertions and deletions.

---

## Technology Stack

**Backend**
* Java 21
* Spring Boot (v4.0.0)
* Spring Web MVC
* Spring Data JDBC & Spring Data JPA

**Frontend**
* Thymeleaf (Server-Side Rendering)
* HTML5 / CSS3 / Vanilla JavaScript

**Database**
* MS SQL Server
* JDBC Template (for optimized, custom querying)

**Build Tool**
* Gradle

---

## Security & Performance
* **SQL Injection Prevention:** Uses parameterized queries and secure `JdbcTemplate` implementations. Sort criteria are strictly filtered via whitelists.
* **XSS Prevention:** Thymeleaf automatically sanitizes HTML entity injections.
* **Validation:** Both client-side (HTML5) and server-side validation for all user inputs.
* **Query Optimization:** Extensive use of `EXISTS` subqueries and `STRING_AGG` functions to reduce database round-trips.

---

## Architecture

The application strictly follows the **MVC (Model-View-Controller)** pattern:
* **Presentation Layer:** Spring MVC controllers processing REST and UI requests.
* **Business Layer:** Controller/Service logic validating data and computing ratings.
* **Data Access Layer:** Dedicated `JdbcRepository` / `JpaRepository` encapsulating SQL Server interactions.
* **Database Layer:** Fully normalized relational schemas with dedicated mapping tables (e.g., `Film_Gen`, `Film_Actor`).




