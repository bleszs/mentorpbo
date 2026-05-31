package com.mentorpbo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * EmailService - Mengirim email notifikasi via Gmail SMTP.
 *
 * Konfigurasi SMTP di application.properties:
 *   spring.mail.host=smtp.gmail.com
 *   spring.mail.port=587
 *   spring.mail.username=emailkamu@gmail.com
 *   spring.mail.password=app_password_gmail
 */
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@jejak-ilmu.id}")
    private String fromEmail;

    @Value("${app.admin.email:${spring.mail.username:admin@jejak-ilmu.id}}")
    private String adminEmail;

    /**
     * Kirim notifikasi ke admin saat mentor baru mendaftar.
     * Jika mail tidak dikonfigurasi, log saja dan lanjutkan.
     */
    public void kirimNotifikasiPendaftaranMentor(String namaMentor, String emailMentor,
                                                  String institusi, String keahlian) {
        if (mailSender == null) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("[Jejak Ilmu] Pendaftaran Mentor Baru: " + namaMentor);

            String html = buildMentorRegistrationEmail(namaMentor, emailMentor, institusi, keahlian);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            // Jangan gagalkan registrasi jika email gagal
            System.err.println("Email gagal dikirim: " + e.getMessage());
        }
    }

    /**
     * Kirim email konfirmasi ke mentor yang baru mendaftar.
     */
    public void kirimKonfirmasiKeMentor(String namaMentor, String emailMentor) {
        if (mailSender == null) return;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "Jejak Ilmu");
            helper.setTo(emailMentor);
            helper.setSubject("Selamat Datang di Jejak Ilmu, " + namaMentor + "!");

            String html = buildWelcomeEmail(namaMentor);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Email konfirmasi gagal: " + e.getMessage());
        }
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
                <a href="http://localhost:8080/monitoring-mentor"
                   style="display:inline-block;padding:12px 24px;background:#061748;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:700;font-size:13px;">
                  Lihat di Dashboard
                </a>
              </div>
              <p style="text-align:center;color:#9ca3af;font-size:12px;margin-top:24px;">
                © 2024 Jejak Ilmu. Academic Excellence through Elite Mentorship.
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
                <p style="color:#6b7280;line-height:1.6;">
                  Setelah diverifikasi, Anda dapat langsung mulai menerima mentee dan menjadwalkan sesi bimbingan.
                </p>
                <div style="background:#f0f7ff;padding:16px;border-radius:8px;border-left:4px solid #061748;margin:20px 0;">
                  <p style="margin:0;color:#061748;font-weight:700;font-size:14px;">💡 Tips untuk Mentor Baru</p>
                  <p style="margin:8px 0 0 0;color:#6b7280;font-size:13px;">
                    Lengkapi profil Anda dengan foto dan bio yang menarik untuk meningkatkan kepercayaan mentee.
                  </p>
                </div>
                <a href="http://localhost:8080/login"
                   style="display:inline-block;padding:12px 24px;background:#061748;color:#ffffff;text-decoration:none;border-radius:8px;font-weight:700;font-size:13px;">
                  Masuk ke Akun
                </a>
              </div>
              <p style="text-align:center;color:#9ca3af;font-size:12px;margin-top:24px;">
                © 2024 Jejak Ilmu. Academic Excellence through Elite Mentorship.
              </p>
            </div>
            """;
    }
}
