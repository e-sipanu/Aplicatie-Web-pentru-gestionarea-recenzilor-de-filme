/**
 * Clasa exceptie pentru cereri HTTP invalide
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.exception;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
