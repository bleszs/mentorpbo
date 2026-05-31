package com.mentorpbo.controller;

import com.mentorpbo.dto.MentorRegistrationDTO;
import com.mentorpbo.model.Mahasiswa;
import com.mentorpbo.model.Pengguna;
import com.mentorpbo.model.Siswa;
import com.mentorpbo.service.PenggunaService;
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

    @Autowired
    public AuthController(PenggunaService penggunaService) {
        this.penggunaService = penggunaService;
    }

    /**
     * Menampilkan halaman login.
     */
    @GetMapping("/login")
    public String halamanLogin() {
        return "auth/login";
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
     * Jika gagal, kembali ke halaman login dengan pesan error.
     */
    @PostMapping("/login")
    public String prosesLogin(@RequestParam String email,
                              @RequestParam String kataSandi,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Optional<Pengguna> pengguna = penggunaService.login(email, kataSandi);

        if (pengguna.isPresent()) {
            Pengguna user = pengguna.get();
            session.setAttribute("penggunaLogin", user);
            session.setAttribute("penggunaId", user.getId());
            session.setAttribute("penggunaRole", user.getRole().name());
            session.setAttribute("penggunaNama", user.getNamaLengkap());

            // Redirect ke dashboard sesuai role (polimorfisme via getDashboardView())
            return "redirect:/dashboard";
        }

        // Redirect dengan URL parameter ?error untuk ditangkap oleh Thymeleaf
        return "redirect:/login?error";
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
                siswa.setMataPelajaranKeahlian(dto.getKeahlian());
                penggunaService.daftarSiswa(siswa);
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
                if (dto.getIpk() != null && !dto.getIpk().isBlank()) {
                    try {
                        String ipkStr = dto.getIpk().replace(",", ".").replaceAll("[^0-9.]", "").trim();
                        if (!ipkStr.isEmpty()) mhs.setIpk(Double.parseDouble(ipkStr));
                    } catch (Exception ignored) {}
                }
                penggunaService.daftarMahasiswa(mhs);
            }

            session.removeAttribute("mentorRegDto");
            redirectAttributes.addFlashAttribute("sukses",
                "Selamat! Akun mentor berhasil dibuat. Silakan login dengan email dan kata sandi Anda.");
            return "redirect:/login?registered";

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
            if (isSiswa) {
                Siswa siswa = new Siswa(
                    namaLengkap, email, kataSandi,
                    "", institusi,
                    nimNisn != null ? nimNisn : ""
                );
                siswa.setMataPelajaranKeahlian(minatBelajar);
                penggunaService.daftarSiswa(siswa);
            } else {
                Mahasiswa mhs = new Mahasiswa(
                    namaLengkap, email, kataSandi,
                    nimNisn != null ? nimNisn : "",
                    "", "", institusi, 1
                );
                mhs.setMataKuliahKeahlian(minatBelajar);
                penggunaService.daftarMahasiswa(mhs);
            }
            redirectAttributes.addFlashAttribute("sukses",
                "Akun mentee berhasil dibuat! Silakan login dengan email dan kata sandi Anda.");
            return "redirect:/login?registered";

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
        return "auth/register-pengawas";
    }

    /**
     * Memproses form pendaftaran Pengawas Akademik dengan upload file.
     */
    @PostMapping("/register-pengawas")
    public String prosesRegisterPengawas(
            @ModelAttribute("pengawasDto") com.mentorpbo.dto.PengawasRegistrationDTO pengawasDto,
            @RequestParam(value = "dokumenVerifikasi", required = false) MultipartFile file,
            RedirectAttributes redirectAttributes) {
        
        try {
            // Validasi data
            if (pengawasDto.getNamaLengkap() == null || pengawasDto.getNamaLengkap().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Nama lengkap harus diisi!");
                return "redirect:/register-pengawas";
            }

            if (pengawasDto.getEmail() == null || pengawasDto.getEmail().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Email harus diisi!");
                return "redirect:/register-pengawas";
            }

            if (!pengawasDto.isSetujuSyaratKetentuan()) {
                redirectAttributes.addFlashAttribute("error", "Anda harus menyetujui Syarat dan Ketentuan!");
                return "redirect:/register-pengawas";
            }

            // Proses upload file jika ada
            String namaFileTersimpan = null;
            if (file != null && !file.isEmpty()) {
                // Validasi ukuran file (max 5MB)
                if (file.getSize() > 5 * 1024 * 1024) {
                    redirectAttributes.addFlashAttribute("error", "Ukuran file maksimal 5MB!");
                    return "redirect:/register-pengawas";
                }

                // Validasi tipe file
                String contentType = file.getContentType();
                if (contentType == null || 
                    (!contentType.equals("image/jpeg") && 
                     !contentType.equals("image/png") && 
                     !contentType.equals("application/pdf"))) {
                    redirectAttributes.addFlashAttribute("error", "Format file harus JPG, PNG, atau PDF!");
                    return "redirect:/register-pengawas";
                }

                // Simpan file (untuk demo, kita hanya log nama file)
                String originalFilename = file.getOriginalFilename();
                namaFileTersimpan = System.currentTimeMillis() + "_" + originalFilename;
                
                // Dalam implementasi nyata, simpan ke folder uploads
                // Path uploadPath = Paths.get("uploads/dokumen-verifikasi");
                // Files.createDirectories(uploadPath);
                // Path filePath = uploadPath.resolve(namaFileTersimpan);
                // Files.copy(file.getInputStream(), filePath);
                
                System.out.println("File uploaded: " + namaFileTersimpan);
            }

            // Log data pendaftaran (dalam implementasi nyata, simpan ke database)
            System.out.println("=== Pendaftaran Pengawas Akademik ===");
            System.out.println("Nama: " + pengawasDto.getNamaLengkap());
            System.out.println("Gelar: " + pengawasDto.getGelarAkademik());
            System.out.println("Email: " + pengawasDto.getEmail());
            System.out.println("Institusi: " + pengawasDto.getNamaInstitusi());
            System.out.println("NIDN/NIP: " + pengawasDto.getNidnNip());
            System.out.println("Departemen: " + pengawasDto.getDepartemen());
            System.out.println("Dokumen: " + namaFileTersimpan);
            System.out.println("=====================================");

            redirectAttributes.addFlashAttribute("sukses", 
                "Pendaftaran berhasil dikirim! Tim kami akan meninjau aplikasi Anda dalam 1-3 hari kerja. " +
                "Anda akan menerima email konfirmasi di " + pengawasDto.getEmail());
            
            return "redirect:/register-pengawas";

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", 
                "Terjadi kesalahan saat memproses pendaftaran: " + e.getMessage());
            return "redirect:/register-pengawas";
        }
    }
}
