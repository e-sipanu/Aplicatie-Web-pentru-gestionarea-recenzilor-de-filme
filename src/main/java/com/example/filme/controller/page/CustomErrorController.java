/**
 * Clasa controller pentru gestionarea paginilor de eroare personalizate
 * @author Sipanu Eduard Nicusor
 * @version 12 Ianuarie 2026
 */
package com.example.filme.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

// @Controller - DISABLED TO AVOID CONFLICT WITH BasicErrorController
public class CustomErrorController {

	// @RequestMapping("/error") - DISABLED
	public String handleError(HttpServletRequest request, Model model) {
		Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

		System.out.println("===== ERROR DETAILS =====");
		System.out.println("Status: " + status);
		System.out.println("Exception: " + exception);
		System.out.println("Message: " + message);
		System.out.println("========================");

		if (exception != null) {
			((Exception) exception).printStackTrace();
		}

		model.addAttribute("status", status);
		model.addAttribute("message", message != null ? message : "An unexpected error occurred");

		return "error";
	}
}
