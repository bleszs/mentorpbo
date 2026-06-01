package com.mentorpbo.service;

import com.mentorpbo.model.Mahasiswa;
import com.mentorpbo.repository.MahasiswaRepository;
import com.mentorpbo.repository.PenggunaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CustomOAuth2UserService - Menangani data pengguna dari Google OAuth2.
 *
 * Alur:
 * 1. Setelah Google auth sukses, Spring memanggil loadUser()
 * 2. Kita ambil email dan nama dari Google profile
 * 3. Jika email sudah terdaftar → biarkan (akan di-login di OAuth2SuccessHandler)
 * 4. Jika email baru → buat akun Mahasiswa otomatis
 */
@Service
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private PenggunaRepository penggunaRepository;

    @Autowired
    private MahasiswaRepository mahasiswaRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String namaLengkap = oAuth2User.getAttribute("name");

        if (email == null) return oAuth2User;

        // Jika pengguna belum terdaftar → buat akun Mahasiswa baru
        if (!penggunaRepository.existsByEmail(email)) {
            Mahasiswa newUser = new Mahasiswa(
                namaLengkap != null ? namaLengkap : email,
                email,
                "google-oauth2-" + System.currentTimeMillis(), // placeholder password
                "", "", "", "", 1
            );
            newUser.setBio("Akun dibuat via Google Login.");
            // Google sudah verifikasi email — tidak perlu verifikasi manual
            newUser.setEmailVerified(true);
            mahasiswaRepository.save(newUser);
        } else {
            // User sudah ada — pastikan emailVerified=true jika login via Google
            penggunaRepository.findByEmail(email).ifPresent(p -> {
                if (!p.isEmailVerified()) {
                    p.setEmailVerified(true);
                    penggunaRepository.save(p);
                }
            });
        }

        return oAuth2User;
    }
}
