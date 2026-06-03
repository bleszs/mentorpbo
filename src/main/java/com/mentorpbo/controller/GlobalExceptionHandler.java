package com.mentorpbo.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Global exception handler untuk error yang terjadi di semua controller.
 * Menangkap MaxUploadSizeExceededException (HTTP 413) agar user mendapat
 * pesan yang jelas, bukan halaman kosong "This page isn't working".
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(MaxUploadSizeExceededException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error",
            "Ukuran file terlalu besar. Maksimum yang diizinkan adalah 10MB. " +
            "Kompres file Anda atau gunakan format yang lebih kecil.");

        // Redirect kembali ke halaman asal yang men-trigger upload
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/register-pengawas")) {
            return "redirect:/register-pengawas";
        }
        if (referer != null && referer.contains("/profil/foto")) {
            return "redirect:/dashboard/pengaturan";
        }
        // Fallback: kembali ke dashboard
        return "redirect:/dashboard";
    }
}
