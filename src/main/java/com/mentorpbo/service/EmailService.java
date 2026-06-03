package com.mentorpbo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * EmailService - Mengirim email notifikasi via Brevo (Sendinblue) HTTP API.
 *
 * Keunggulan Brevo vs Resend/SMTP:
 * - Tidak butuh verifikasi domain — cukup verifikasi email pengirim
 * - Bisa kirim ke email SIAPAPUN (user yang baru daftar, dll)
 * - Gratis 300 email/hari
 * - Menggunakan HTTP API → tidak diblokir Railway
 *
 * Setup:
 * 1. Daftar di https://app.brevo.com
 * 2. Verifikasi email pengirim di Settings → Senders & IPs
 * 3. Buat API Key di Settings → API Keys
 * 4. Set BREVO_API_KEY di Railway Environment Variables
 */
@Service
public class EmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Value("${app.brevo.api-key:}")
    private String brevoApiKey;

    @Value("${app.brevo.from-email:jejakilmu1@gmail.com}")
    private String fromEmail;

    @Value("${app.brevo.from-name:Jejak Ilmu}")
    private String fromName;

    @Value("${app.admin.email:blesslysilaban@gmail.com}")
    private String adminEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    // ================================================================
    // Pengiriman email internal
    // ================================================================

    /**
     * Kirim email HTML via Brevo HTTP API.
     * Bisa kirim ke email siapapun tanpa verifikasi domain.
     * Return true jika berhasil, false jika gagal.
     */
    private boolean kirimEmail(String toEmail, String toName, String subject, String htmlBody) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            System.out.println("=== [DEV] Brevo API key belum diset — email ke " + toEmail
                + " | Subject: " + subject + " ===");
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            Map<String, Object> body = Map.of(
                "sender",      Map.of("name", fromName, "email", fromEmail),
                "to",          List.of(Map.of("email", toEmail, "name", toName != null ? toName : toEmail)),
                "subject",     subject,
                "htmlContent", htmlBody
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (success) {
                System.out.println("[BREVO] Email terkirim ke: " + toEmail + " | " + subject);
            } else {
                System.err.println("[BREVO ERROR] Status: " + response.getStatusCode()
                    + " | Body: " + response.getBody());
            }
            return success;

        } catch (Exception e) {
            System.err.println("[BREVO ERROR] Gagal kirim ke " + toEmail + " | " + e.getMessage());
            return false;
        }
    }

    // ================================================================
    // Public API
    // ================================================================

    /**
     * Kirim kode OTP 6 digit ke email pengguna yang baru mendaftar.
     * OTP berlaku 15 menit.
     * Jika Brevo gagal, OTP dicetak ke Railway logs sebagai fallback.
     */
    public void kirimEmailVerifikasi(String namaUser, String emailUser, String otp) {
        boolean terkirim = kirimEmail(
            emailUser,
            namaUser,
            "Kode Verifikasi Akun Jejak Ilmu: " + otp,
            buildOtpEmail(namaUser, otp)
        );
        if (!terkirim) {
            // Fallback — OTP tetap bisa dilihat di Railway Deploy Logs
            System.out.println("=== [FALLBACK OTP] ke: " + emailUser + " | OTP: " + otp + " ===");
        }
    }

    /**
     * Kirim notifikasi ke admin saat mentor baru mendaftar.
     */
    public void kirimNotifikasiPendaftaranMentor(String namaMentor, String emailMentor,
                                                  String institusi, String keahlian) {
        kirimEmail(
            adminEmail,
            "Admin Jejak Ilmu",
            "[Jejak Ilmu] Pendaftaran Mentor Baru: " + namaMentor,
            buildMentorRegistrationEmail(namaMentor, emailMentor, institusi, keahlian)
        );
    }

    /**
     * Kirim email konfirmasi selamat datang ke mentor yang baru mendaftar.
     */
    public void kirimKonfirmasiKeMentor(String namaMentor, String emailMentor) {
        kirimEmail(
            emailMentor,
            namaMentor,
            "Selamat Datang di Jejak Ilmu, " + namaMentor + "!",
            buildWelcomeEmail(namaMentor)
        );
    }

    // ================================================================
    // HTML Template Builder
    // ================================================================

    private String buildOtpEmail(String nama, String otp) {
        StringBuilder digitBoxes = new StringBuilder();
        for (char c : otp.toCharArray()) {
            digitBoxes.append(
                "<span style=\"display:inline-block;width:48px;height:56px;line-height:56px;" +
                "text-align:center;font-size:28px;font-weight:900;color:#061748;" +
                "background:#dce1ff;border-radius:10px;margin:0 4px;\">" + c + "</span>"
            );
        }
        String displayNama = (nama == null || nama.isBlank()) ? "Pengguna" : nama;
        return "<div style=\"font-family:Inter,Arial,sans-serif;max-width:560px;margin:0 auto;background:#f8f9fa;padding:28px;\">"
            + "<div style=\"background:#061748;padding:22px 28px;border-radius:14px;margin-bottom:20px;\">"
            + "  <h1 style=\"color:#ffffff;margin:0;font-size:20px;font-weight:900;\">Jejak Ilmu</h1>"
            + "  <p style=\"color:#b6c4fe;margin:4px 0 0 0;font-size:12px;\">Platform Mentoring Akademik</p>"
            + "</div>"
            + "<div style=\"background:#ffffff;padding:32px;border-radius:14px;border:1px solid #e5e7eb;\">"
            + "  <h2 style=\"color:#061748;margin:0 0 6px 0;font-size:20px;font-weight:900;\">Kode Verifikasi Anda</h2>"
            + "  <p style=\"color:#6b7280;margin:0 0 24px 0;font-size:14px;\">Halo <strong>" + displayNama + "</strong>, masukkan kode berikut di halaman verifikasi:</p>"
            + "  <div style=\"text-align:center;margin:24px 0;\">" + digitBoxes + "</div>"
            + "  <div style=\"background:#fef3c7;padding:12px 16px;border-radius:8px;border-left:4px solid #fea619;margin-top:24px;\">"
            + "    <p style=\"margin:0;color:#92400e;font-size:13px;font-weight:700;\">⏰ Kode berlaku 15 menit</p>"
            + "    <p style=\"margin:4px 0 0 0;color:#92400e;font-size:12px;\">Jangan bagikan kode ini kepada siapapun.</p>"
            + "  </div>"
            + "  <p style=\"color:#9ca3af;font-size:12px;margin-top:20px;\">Jika Anda tidak mendaftar di Jejak Ilmu, abaikan email ini.</p>"
            + "</div>"
            + "<p style=\"text-align:center;color:#9ca3af;font-size:11px;margin-top:20px;\">© 2025 Jejak Ilmu. Platform Mentoring Akademik Indonesia.</p>"
            + "</div>";
    }

    private String buildMentorRegistrationEmail(String nama, String email,
                                                  String institusi, String keahlian) {
        return """
            <div style="font-family:Inter,sans-serif;max-width:600px;margin:0 auto;background:#f8f9fa;padding:32px;">
              <div style="background:#061748;padding:24px;border-radius:16px;margin-bottom:24px;">
                <h1 style="color:#ffffff;margin:0;font-size:24px;">Jejak Ilmu</h1>
                <p style="color:#b6c4fe;margin:8px 0 0 0;font-size:13px;">Platform Mentoring Akademik</p>
              </div>
              <div style="background:#ffffff;padding:28px;border-radius:16px;border:1px solid #e5e7eb;">
                <h2 style="color:#061748;margin-top:0;">Pendaftaran Mentor Baru 🎉</h2>
                <p style="color:#6b7280;">Ada mentor baru yang mendaftar di Jejak Ilmu:</p>
                <table style="width:100%;border-collapse:collapse;margin:16px 0;">
                  <tr><td style="padding:8px;color:#6b7280;width:140px;">Nama</td><td style="padding:8px;font-weight:700;color:#111827;">"""
                    + nama + """
                </td></tr>
                  <tr style="background:#f9fafb;"><td style="padding:8px;color:#6b7280;">Email</td><td style="padding:8px;font-weight:700;color:#111827;">"""
                    + email + """
                </td></tr>
                  <tr><td style="padding:8px;color:#6b7280;">Institusi</td><td style="padding:8px;font-weight:700;color:#111827;">"""
                    + institusi + """
                </td></tr>
                  <tr style="background:#f9fafb;"><td style="padding:8px;color:#6b7280;">Keahlian</td><td style="padding:8px;font-weight:700;color:#111827;">"""
                    + keahlian + """
                </td></tr>
                </table>
              </div>
              <p style="text-align:center;color:#9ca3af;font-size:12px;margin-top:24px;">
                © 2025 Jejak Ilmu. Platform Mentoring Akademik Indonesia.
              </p>
            </div>
            """;
    }

    private String buildWelcomeEmail(String nama) {
        return """
            <div style="font-family:Inter,sans-serif;max-width:600px;margin:0 auto;background:#f8f9fa;padding:32px;">
              <div style="background:#061748;padding:24px;border-radius:16px;margin-bottom:24px;">
                <h1 style="color:#ffffff;margin:0;font-size:24px;">Jejak Ilmu</h1>
                <p style="color:#b6c4fe;margin:8px 0 0 0;font-size:13px;">Platform Mentoring Akademik</p>
              </div>
              <div style="background:#ffffff;padding:28px;border-radius:16px;border:1px solid #e5e7eb;">
                <h2 style="color:#061748;margin-top:0;">Selamat Datang, """ + nama + """
                ! 🎓</h2>
                <p style="color:#6b7280;line-height:1.6;">
                  Pendaftaran Anda sebagai mentor di <strong>Jejak Ilmu</strong> telah kami terima.
                  Tim kami akan meninjau profil Anda dalam <strong>1×24 jam</strong>.
                </p>
              </div>
              <p style="text-align:center;color:#9ca3af;font-size:12px;margin-top:24px;">
                © 2025 Jejak Ilmu. Platform Mentoring Akademik Indonesia.
              </p>
            </div>
            """;
    }
}
