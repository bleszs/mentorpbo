package com.mentorpbo.service;

import com.mentorpbo.model.Pengguna;
import com.mentorpbo.repository.PenggunaRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OAuth2SuccessHandler - Dipanggil setelah Google OAuth2 berhasil.
 *
 * Tugasnya:
 * 1. Ambil email pengguna dari Google profile
 * 2. Cari pengguna di DB berdasarkan email
 * 3. Set session attributes yang sama dengan login manual
 * 4. Redirect ke /dashboard
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private PenggunaRepository penggunaRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (email != null) {
            Optional<Pengguna> opt = penggunaRepository.findByEmail(email);
            if (opt.isPresent()) {
                Pengguna pengguna = opt.get();

                // Update waktu login terakhir
                pengguna.setTerakhirLogin(LocalDateTime.now());
                penggunaRepository.save(pengguna);

                // Set session attributes — sama persis dengan login manual
                HttpSession session = request.getSession(true);
                session.setAttribute("penggunaLogin", pengguna);
                session.setAttribute("penggunaId", pengguna.getId());
                session.setAttribute("penggunaRole", pengguna.getRole().name());
                session.setAttribute("penggunaNama", pengguna.getNamaLengkap());
            }
        }

        getRedirectStrategy().sendRedirect(request, response, "/dashboard");
    }
}
