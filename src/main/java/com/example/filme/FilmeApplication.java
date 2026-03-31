/**
 * Clasa principala pentru aplicatia de gestionare a filmelor
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.example.filme")
public class FilmeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FilmeApplication.class, args);
    }
}
