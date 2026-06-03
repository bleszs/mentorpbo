package com.mentorpbo.controller;

import com.mentorpbo.dto.MentorRegistrationDTO;
import com.mentorpbo.model.Mahasiswa;
import com.mentorpbo.model.Pengguna;
import com.mentorpbo.model.Siswa;
import com.mentorpbo.service.EmailService;
import com.mentorpbo.service.PenggunaService;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final PenggunaService penggunaService;
    private final EmailService emailService;

    @Value("${spring.security.oauth2.client.registration.google.client-id:PLACEHOLDER}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:PLACEHOLDER}")
    private String googleClientSecret;

    @Autowired
    public AuthController(PenggunaService penggunaService, EmailService emailService) {
        this.penggunaService = penggunaService;
        this.emailService = emailService;
    }

    private String encode(String s) {
        if (s == null) return "";
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }

    private boolean isGoogleOAuth2Ready() {
        String id  = googleClientId  != null ? googleClientId.trim()  : "";
        String sec = googleClientSecret != null ? googleClientSecret.trim() : "";
        return id.endsWith(".apps.googleusercontent.com")
            && !sec.equals("PLACEHOLDER") && !sec.isBlank();
    }

    @GetMapping("/login")
    public String halamanLogin(Model model) {
        model.addAttribute("googleOAuth2Ready", isGoogleOAuth2Ready());
        return "auth/login";
    }

    // Ketika OAuth2 belum aktif, Spring Security tidak mengintersep URL ini.
    // Redirect diam-diam ke login tanpa pesan error yang menakutkan.
    @GetMapping("/oauth2/authorization/google")
    public String handleGoogleOAuth2() {
        return "redirect:/login";
    }

    /**
     * Menampilkan halaman login khusus untuk Pengawas Akademik (Guru/Dosen).
     */
    @GetMapping("/login-pengawas")
    public String halamanLoginPengawas() {
        return "auth/login-pengawas";
    }

    /**
     * Memproses form login.
     * Jika berhasil, menyimpan data pengguna ke session dan redirect ke dashboard.
     * Jika email belum diverifikasi, tampilkan pesan khusus.
     */
    @PostMapping("/login")
    public String prosesLogin(@RequestParam String email,
                              @RequestParam String kataSandi,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Optional<Pengguna> pengguna = penggunaService.login(email, kataSandi);

        if (pengguna.isPresent()) {
            Pengguna user = pengguna.get();

            if (!user.isEmailVerified()) {
                redirectAttributes.addFlashAttribute("error",
                    "Email belum diverifikasi. Masukkan kode OTP yang dikirim ke " + user.getEmail() + ".");
                return "redirect:/verify-otp?email=" + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8);
            }

            session.setAttribute("penggunaLogin", user);
            session.setAttribute("penggunaId", user.getId());
            session.setAttribute("penggunaRole", user.getRole().name());
            session.setAttribute("penggunaNama", user.getNamaLengkap());

            return "redirect:/dashboard";
        }

        return "redirect:/login?error";
    }

    // ============================================================
    // OTP VERIFICATION ENDPOINTS
    // ============================================================

    /**
     * Halaman input kode OTP setelah registrasi.
     * Email pengguna dikirim via query param agar halaman bisa menampilkan
     * "Kode dikirim ke xxx@gmail.com".
     */
    @GetMapping("/verify-otp")
    public String halamanVerifyOtp(@RequestParam(required = false) String email,
                                   Model model) {
        String emailVal = email != null ? email : "";
        model.addAttribute("email", emailVal);
        // Tampilkan OTP aktif di halaman (dev mode — bantu user yang tidak menerima email)
        model.addAttribute("devOtp", penggunaService.getOtpForDev(emailVal));
        return "auth/verify-otp";
    }

    /**
     * Proses verifikasi OTP yang dimasukkan user secara manual.
     * Jika benar → akun aktif → redirect ke login dengan pesan sukses.
     * Jika salah/expired → kembali ke halaman OTP dengan pesan error.
     */
    @PostMapping("/verify-otp")
    public String prosesVerifyOtp(@RequestParam String email,
                                   @RequestParam String otp,
                                   RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email dan kode OTP harus diisi.");
            return "redirect:/verify-otp?email=" + encode(email);
        }

        boolean berhasil = penggunaService.verifikasiOtp(email, otp.replaceAll("\\s", ""));
        if (berhasil) {
            redirectAttributes.addFlashAttribute("sukses",
                "Email berhasil diverifikasi! Silakan login sekarang.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error",
                "Kode OTP salah atau sudah kedaluwarsa (15 menit). Klik 'Kirim Ulang' untuk kode baru.");
            return "redirect:/verify-otp?email=" + encode(email);
        }
    }

    /**
     * Kirim ulang OTP baru ke email yang sama.
     */
    @PostMapping("/verify-otp/resend")
    public String kirimUlangOtp(@RequestParam String email,
                                 RedirectAttributes redirectAttributes) {
        try {
            String otp = penggunaService.kirimUlangOtp(email);
            emailService.kirimEmailVerifikasi("", email, otp);
            redirectAttributes.addFlashAttribute("sukses",
                "Kode baru berhasil dikirim ke " + email + ". Cek inbox (berlaku 15 menit).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/verify-otp?email=" + encode(email);
    }

    /**
     * Verifikasi email lama via link token (backwards-compat untuk email lama di inbox).
     */
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam(required = false) String token,
                              RedirectAttributes redirectAttributes) {
        if (token == null || token.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Link verifikasi tidak valid.");
            return "redirect:/login";
        }
        boolean berhasil = penggunaService.verifikasiEmail(token);
        if (berhasil) {
            redirectAttributes.addFlashAttribute("sukses",
                "Email berhasil diverifikasi! Silakan login sekarang.");
        } else {
            redirectAttributes.addFlashAttribute("error",
                "Link tidak valid atau kedaluwarsa. Gunakan halaman verifikasi kode OTP.");
        }
        return "redirect:/login";
    }

    /**
     * Memproses logout pengguna.
     * Menghapus seluruh data dari session dan redirect dengan parameter ?logout.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    /**
     * Menampilkan halaman registrasi.
     */
    @GetMapping("/register")
    public String halamanRegister() {
        return "auth/register";
    }

    /**
     * Halaman beranda (landing page).
     */
    @GetMapping("/")
    public String root() {
        return "beranda";
    }

    /**
     * Menampilkan halaman form pendaftaran mentor (Langkah 1: Data Pribadi).
     */
    @GetMapping("/register/mentor")
    public String halamanRegisterMentor(Model model, HttpSession session) {
        MentorRegistrationDTO existing = (MentorRegistrationDTO) session.getAttribute("mentorRegDto");
        model.addAttribute("mentorDto", existing != null ? existing : new MentorRegistrationDTO());
        return "auth/register-mentor";
    }

    /**
     * Memproses form pendaftaran mentor (Langkah 1: Data Pribadi).
     * Menyimpan data ke session dan redirect ke Langkah 2.
     */
    @PostMapping("/register/mentor")
    public String prosesRegisterMentor(@ModelAttribute("mentorDto") MentorRegistrationDTO mentorDto,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        if (mentorDto.getEmail() == null || mentorDto.getEmail().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email harus diisi.");
            return "redirect:/register/mentor";
        }
        if (mentorDto.getKataSandi() == null || mentorDto.getKataSandi().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Kata sandi minimal 6 karakter.");
            return "redirect:/register/mentor";
        }
        session.setAttribute("mentorRegDto", mentorDto);
        return "redirect:/register/mentor/step2";
    }

    /**
     * Menampilkan halaman Langkah 2: Profil Akademik.
     */
    @GetMapping("/register/mentor/step2")
    public String halamanRegisterMentorStep2(Model model, HttpSession session) {
        MentorRegistrationDTO dto = (MentorRegistrationDTO) session.getAttribute("mentorRegDto");
        if (dto == null) return "redirect:/register/mentor";
        model.addAttribute("dto", dto);
        return "auth/register-mentor-step2";
    }

    /**
     * Memproses form Langkah 2: Profil Akademik.
     */
    @PostMapping("/register/mentor/step2")
    public String prosesRegisterMentorStep2(@RequestParam String keahlian,
                                             @RequestParam(required = false) String topikKeahlian,
                                             @RequestParam(required = false) String kelas,
                                             @RequestParam(required = false) String semester,
                                             @RequestParam(required = false) String programStudi,
                                             @RequestParam(required = false) String fakultas,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        MentorRegistrationDTO dto = (MentorRegistrationDTO) session.getAttribute("mentorRegDto");
        if (dto == null) return "redirect:/register/mentor";

        if (keahlian == null || keahlian.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Keahlian harus diisi.");
            return "redirect:/register/mentor/step2";
        }

        dto.setKeahlian(keahlian);
        dto.setTopikKeahlian(topikKeahlian);
        dto.setKelas(kelas);
        dto.setSemester(semester);
        dto.setProgramStudi(programStudi);
        dto.setFakultas(fakultas);
        session.setAttribute("mentorRegDto", dto);
        return "redirect:/register/mentor/step3";
    }

    /**
     * Menampilkan halaman Langkah 3: Motivasi.
     */
    @GetMapping("/register/mentor/step3")
    public String halamanRegisterMentorStep3(Model model, HttpSession session) {
        MentorRegistrationDTO dto = (MentorRegistrationDTO) session.getAttribute("mentorRegDto");
        if (dto == null) return "redirect:/register/mentor";
        model.addAttribute("dto", dto);
        return "auth/register-mentor-step3";
    }

    /**
     * Memproses form Langkah 3: Motivasi.
     * Menyimpan mentor baru ke database dan redirect ke halaman login.
     */
    @PostMapping("/register/mentor/step3")
    public String prosesRegisterMentorStep3(@RequestParam String motivasi,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        MentorRegistrationDTO dto = (MentorRegistrationDTO) session.getAttribute("mentorRegDto");
        if (dto == null) return "redirect:/register/mentor";

        if (motivasi == null || motivasi.isBlank() || motivasi.length() < 10) {
            redirectAttributes.addFlashAttribute("error", "Motivasi harus diisi (min. 10 karakter).");
            return "redirect:/register/mentor/step3";
        }
        dto.setMotivasi(motivasi);

        try {
            Pengguna savedUser;
            if (dto.isSiswa()) {
                Siswa siswa = new Siswa(
                    dto.getNamaLengkap(),
                    dto.getEmail(),
                    dto.getKataSandi(),
                    dto.getKelas() != null ? dto.getKelas() : "",
                    dto.getInstitusi(),
                    dto.getNimNisn() != null ? dto.getNimNisn() : ""
                );
                siswa.setMentor(true);
                siswa.setStatusValidasiMentor(com.mentorpbo.model.enums.StatusValidasi.BELUM_DITINJAU);
                siswa.setMataPelajaranKeahlian(dto.getKeahlian());
                savedUser = penggunaService.daftarSiswa(siswa);
            } else {
                int semesterInt = 1;
                try { semesterInt = Integer.parseInt(dto.getSemester()); } catch (Exception ignored) {}
                Mahasiswa mhs = new Mahasiswa(
                    dto.getNamaLengkap(),
                    dto.getEmail(),
                    dto.getKataSandi(),
                    dto.getNimNisn() != null ? dto.getNimNisn() : "",
                    dto.getProgramStudi() != null ? dto.getProgramStudi() : "",
                    dto.getFakultas() != null ? dto.getFakultas() : "",
                    dto.getInstitusi(),
                    semesterInt
                );
                mhs.aktifkanSebagaiMentor(dto.getKeahlian(), dto.getTopikKeahlian());
                mhs.setStatusValidasiMentor(com.mentorpbo.model.enums.StatusValidasi.BELUM_DITINJAU);
                if (dto.getIpk() != null && !dto.getIpk().isBlank()) {
                    try {
                        String ipkStr = dto.getIpk().replace(",", ".").replaceAll("[^0-9.]", "").trim();
                        if (!ipkStr.isEmpty()) mhs.setIpk(Double.parseDouble(ipkStr));
                    } catch (Exception ignored) {}
                }
                savedUser = penggunaService.daftarMahasiswa(mhs);
            }

            session.removeAttribute("mentorRegDto");

            // Generate OTP dan kirim ke email mentor
            String otp = penggunaService.generateTokenVerifikasi(savedUser);
            emailService.kirimEmailVerifikasi(dto.getNamaLengkap(), dto.getEmail(), otp);
            try { emailService.kirimNotifikasiPendaftaranMentor(
                    dto.getNamaLengkap(), dto.getEmail(),
                    dto.getInstitusi() != null ? dto.getInstitusi() : "-",
                    dto.getKeahlian() != null ? dto.getKeahlian() : "-"); }
            catch (Exception ignored) {}

            return "redirect:/verify-otp?email=" + encode(dto.getEmail());

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register/mentor/step3";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            return "redirect:/register/mentor/step3";
        }
    }

    /**
     * Menampilkan halaman form pendaftaran mentee.
     */
    @GetMapping("/register/mentee")
    public String halamanRegisterMentee(Model model) {
        model.addAttribute("form", new com.mentorpbo.dto.MenteeRegistrationDTO());
        model.addAttribute("googleOAuth2Ready", isGoogleOAuth2Ready());
        return "auth/register-mentee";
    }

    /**
     * Memproses form pendaftaran mentee.
     * Membuat akun Siswa atau Mahasiswa tanpa isMentor dan redirect ke login.
     */
    @PostMapping("/register/mentee")
    public String prosesRegisterMentee(@RequestParam String namaLengkap,
                                        @RequestParam(required = false) String nimNisn,
                                        @RequestParam String tingkatPendidikan,
                                        @RequestParam String institusi,
                                        @RequestParam String email,
                                        @RequestParam String kataSandi,
                                        @RequestParam String minatBelajar,
                                        RedirectAttributes redirectAttributes) {
        if (email == null || email.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email harus diisi.");
            return "redirect:/register/mentee";
        }
        if (kataSandi == null || kataSandi.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Kata sandi minimal 6 karakter.");
            return "redirect:/register/mentee";
        }

        try {
            boolean isSiswa = tingkatPendidikan != null && tingkatPendidikan.startsWith("SMA");
            Pengguna user;
            if (isSiswa) {
                Siswa siswa = new Siswa(
                    namaLengkap, email, kataSandi,
                    "", institusi,
                    nimNisn != null ? nimNisn : ""
                );
                siswa.setMataPelajaranKeahlian(minatBelajar);
                user = penggunaService.daftarSiswa(siswa);
            } else {
                Mahasiswa mhs = new Mahasiswa(
                    namaLengkap, email, kataSandi,
                    nimNisn != null ? nimNisn : "",
                    "", "", institusi, 1
                );
                mhs.setMataKuliahKeahlian(minatBelajar);
                user = penggunaService.daftarMahasiswa(mhs);
            }

            // Generate OTP dan kirim ke email mentee
            String otp = penggunaService.generateTokenVerifikasi(user);
            emailService.kirimEmailVerifikasi(namaLengkap, email, otp);

            return "redirect:/verify-otp?email=" + encode(email);

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register/mentee";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            return "redirect:/register/mentee";
        }
    }

    /**
     * Menampilkan halaman form pendaftaran Pengawas Akademik (Guru/Dosen).
     */
    @GetMapping("/register-pengawas")
    public String halamanRegisterPengawas(Model model) {
        model.addAttribute("pengawasDto", new com.mentorpbo.dto.PengawasRegistrationDTO());
        model.addAttribute("googleOAuth2Ready", isGoogleOAuth2Ready());
        return "auth/register-pengawas";
    }

    /**
     * Memproses form pendaftaran Pengawas Akademik (Guru/Dosen) — menyimpan ke database.
     */
    @PostMapping("/register-pengawas")
    public String prosesRegisterPengawas(
            @ModelAttribute("pengawasDto") com.mentorpbo.dto.PengawasRegistrationDTO pengawasDto,
            @RequestParam(value = "dokumenVerifikasi", required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {
            if (pengawasDto.getNamaLengkap() == null || pengawasDto.getNamaLengkap().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Nama lengkap harus diisi!");
                return "redirect:/register-pengawas";
            }
            if (pengawasDto.getEmail() == null || pengawasDto.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email harus diisi!");
                return "redirect:/register-pengawas";
            }
            if (pengawasDto.getKataSandi() == null || pengawasDto.getKataSandi().length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Kata sandi minimal 6 karakter!");
                return "redirect:/register-pengawas";
            }
            if (!pengawasDto.isSetujuSyaratKetentuan()) {
                redirectAttributes.addFlashAttribute("error", "Anda harus menyetujui Syarat dan Ketentuan!");
                return "redirect:/register-pengawas";
            }

            // Tentukan tipe: Guru (sekolah) atau Dosen (kampus) berdasarkan tipeInstitusi
            String tipe = pengawasDto.getTipeInstitusi();
            boolean isGuru = "SEKOLAH".equalsIgnoreCase(tipe);

            Pengguna savedUser;
            if (isGuru) {
                com.mentorpbo.model.Guru guru = new com.mentorpbo.model.Guru(
                    pengawasDto.getNamaLengkap(),
                    pengawasDto.getEmail(),
                    pengawasDto.getKataSandi(),
                    pengawasDto.getNidnNip() != null ? pengawasDto.getNidnNip() : "",
                    pengawasDto.getNamaInstitusi() != null ? pengawasDto.getNamaInstitusi() : "",
                    pengawasDto.getDepartemen() != null ? pengawasDto.getDepartemen() : ""
                );
                if (pengawasDto.getJabatan() != null) guru.setBidangKeahlian(pengawasDto.getJabatan());
                if (pengawasDto.getGelarAkademik() != null)
                    guru.setBio("Gelar: " + pengawasDto.getGelarAkademik());
                savedUser = penggunaService.daftarGuru(guru);
            } else {
                com.mentorpbo.model.Dosen dosen = new com.mentorpbo.model.Dosen(
                    pengawasDto.getNamaLengkap(),
                    pengawasDto.getEmail(),
                    pengawasDto.getKataSandi(),
                    pengawasDto.getNidnNip() != null ? pengawasDto.getNidnNip() : "",
                    pengawasDto.getDepartemen() != null ? pengawasDto.getDepartemen() : "",
                    "",
                    pengawasDto.getNamaInstitusi() != null ? pengawasDto.getNamaInstitusi() : ""
                );
                if (pengawasDto.getJabatan() != null) dosen.setJabatanFungsional(pengawasDto.getJabatan());
                if (pengawasDto.getMinatRiset() != null) dosen.setBidangRiset(pengawasDto.getMinatRiset());
                if (pengawasDto.getGelarAkademik() != null)
                    dosen.setBio("Gelar: " + pengawasDto.getGelarAkademik());
                savedUser = penggunaService.daftarDosen(dosen);
            }

            // Generate OTP dan kirim ke email pengawas
            String otp = penggunaService.generateTokenVerifikasi(savedUser);
            emailService.kirimEmailVerifikasi(pengawasDto.getNamaLengkap(), pengawasDto.getEmail(), otp);

            return "redirect:/verify-otp?email=" + encode(pengawasDto.getEmail());

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register-pengawas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Terjadi kesalahan: " + e.getMessage());
            return "redirect:/register-pengawas";
        }
    }
}
