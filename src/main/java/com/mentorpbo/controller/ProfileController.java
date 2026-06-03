package com.mentorpbo.controller;

import com.mentorpbo.model.*;
import com.mentorpbo.repository.PenggunaRepository;
import com.mentorpbo.service.MentoringService;
import com.mentorpbo.service.PenggunaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.*;

/**
 * ProfileController — menangani semua aksi POST dari dashboard:
 *   - Simpan pengaturan profil & preferensi ke H2
 *   - Upload foto profil
 *   - Unggah materi belajar baru
 *   - Unggah sumber daya baru
 */
@Controller
@RequestMapping("/profil")
public class ProfileController {

    private final PenggunaService penggunaService;
    private final MentoringService mentoringService;
    private final PenggunaRepository penggunaRepository;

    // Folder upload relatif terhadap working directory
    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    public ProfileController(PenggunaService penggunaService,
                             MentoringService mentoringService,
                             PenggunaRepository penggunaRepository) {
        this.penggunaService   = penggunaService;
        this.mentoringService  = mentoringService;
        this.penggunaRepository = penggunaRepository;
    }

    // ============================================================
    // SIMPAN PROFIL
    // ============================================================

    /**
     * POST /profil/simpan — update nama, bio, portofolio, dan field supervisor.
     */
    @PostMapping("/simpan")
    public String simpanProfil(@RequestParam(required = false) String namaLengkap,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String portofolioUrl,
                                @RequestParam(required = false) String keahlian,
                                @RequestParam(required = false) String topikKeahlian,
                                @RequestParam(required = false) String programStudi,
                                @RequestParam(required = false) String universitas,
                                @RequestParam(required = false) String namaSekolah,
                                // Supervisor-specific fields
                                @RequestParam(required = false) String jabatanFungsional,
                                @RequestParam(required = false) String mataKuliahDiampu,
                                @RequestParam(required = false) String mataPelajaranDiampu,
                                @RequestParam(required = false) String bidangRiset,
                                @RequestParam(required = false) String bidangKeahlian,
                                @RequestParam(required = false) String nidn,
                                @RequestParam(required = false) String nip,
                                @RequestParam(required = false) String fakultas,
                                HttpSession session,
                                RedirectAttributes flash) {
        Long id = (Long) session.getAttribute("penggunaId");
        if (id == null) return "redirect:/login";

        try {
            // Update entity Pengguna (nama + bio)
            penggunaService.updateProfil(id, namaLengkap, bio, portofolioUrl);

            // Update UserPreferences (keahlian, institusi, notif, dll)
            UserPreferences pref = penggunaService.getOrCreatePreferences(id);
            if (keahlian      != null) pref.setKeahlian(keahlian);
            if (topikKeahlian != null) pref.setTopikKeahlian(topikKeahlian);
            if (programStudi  != null) pref.setProgramStudi(programStudi);
            if (universitas   != null) pref.setUniversitas(universitas);
            if (namaSekolah   != null) pref.setNamaSekolah(namaSekolah);
            penggunaService.simpanPreferences(id, pref);

            // Update field supervisor spesifik (Dosen / Guru)
            penggunaRepository.findById(id).ifPresent(p -> {
                if (p instanceof com.mentorpbo.model.Dosen dosen) {
                    if (jabatanFungsional != null && !jabatanFungsional.isBlank())
                        dosen.setJabatanFungsional(jabatanFungsional.trim());
                    if (mataKuliahDiampu != null && !mataKuliahDiampu.isBlank())
                        dosen.setMataKuliahDiampu(mataKuliahDiampu.trim());
                    if (bidangRiset != null && !bidangRiset.isBlank())
                        dosen.setBidangRiset(bidangRiset.trim());
                    if (nidn != null && !nidn.isBlank())
                        dosen.setNidn(nidn.trim());
                    if (universitas != null && !universitas.isBlank())
                        dosen.setUniversitas(universitas.trim());
                    if (programStudi != null && !programStudi.isBlank())
                        dosen.setProgramStudi(programStudi.trim());
                    if (fakultas != null && !fakultas.isBlank())
                        dosen.setFakultas(fakultas.trim());
                    penggunaRepository.save(dosen);
                } else if (p instanceof com.mentorpbo.model.Guru guru) {
                    if (mataPelajaranDiampu != null && !mataPelajaranDiampu.isBlank())
                        guru.setMataPelajaranDiampu(mataPelajaranDiampu.trim());
                    if (bidangKeahlian != null && !bidangKeahlian.isBlank())
                        guru.setBidangKeahlian(bidangKeahlian.trim());
                    if (nip != null && !nip.isBlank())
                        guru.setNip(nip.trim());
                    if (namaSekolah != null && !namaSekolah.isBlank())
                        guru.setNamaSekolah(namaSekolah.trim());
                    penggunaRepository.save(guru);
                }
            });

            // Perbarui nama di session agar topbar langsung update
            session.setAttribute("penggunaNama", namaLengkap != null ? namaLengkap.trim() : session.getAttribute("penggunaNama"));

            flash.addFlashAttribute("sukses", "Profil berhasil disimpan!");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal menyimpan profil: " + e.getMessage());
        }
        return "redirect:/dashboard/pengaturan";
    }

    // ============================================================
    // SIMPAN PREFERENSI NOTIF & PRIVASI
    // ============================================================

    @PostMapping("/preferensi")
    public String simpanPreferensi(
            @RequestParam(defaultValue = "false") boolean notifEmailAktif,
            @RequestParam(defaultValue = "false") boolean notifPengingatSesi,
            @RequestParam(defaultValue = "false") boolean notifPesanBaru,
            @RequestParam(defaultValue = "false") boolean notifValidasiSesi,
            @RequestParam(defaultValue = "false") boolean notifRating,
            @RequestParam(defaultValue = "false") boolean profilPublik,
            @RequestParam(defaultValue = "false") boolean tampilDiRanking,
            @RequestParam(defaultValue = "false") boolean bagikanStatistik,
            HttpSession session, RedirectAttributes flash) {
        Long id = (Long) session.getAttribute("penggunaId");
        if (id == null) return "redirect:/login";

        try {
            UserPreferences form = new UserPreferences();
            form.setNotifEmailAktif(notifEmailAktif);
            form.setNotifPengingatSesi(notifPengingatSesi);
            form.setNotifPesanBaru(notifPesanBaru);
            form.setNotifValidasiSesi(notifValidasiSesi);
            form.setNotifRating(notifRating);
            form.setProfilPublik(profilPublik);
            form.setTampilDiRanking(tampilDiRanking);
            form.setBagikanStatistik(bagikanStatistik);
            penggunaService.simpanPreferences(id, form);
            flash.addFlashAttribute("sukses", "Preferensi berhasil disimpan!");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal menyimpan preferensi: " + e.getMessage());
        }
        return "redirect:/dashboard/pengaturan";
    }

    // ============================================================
    // UPLOAD FOTO PROFIL
    // ============================================================

    @PostMapping("/foto")
    public String uploadFoto(@RequestParam("foto") MultipartFile foto,
                              HttpSession session, RedirectAttributes flash) {
        Long id = (Long) session.getAttribute("penggunaId");
        if (id == null) return "redirect:/login";

        if (foto.isEmpty()) {
            flash.addFlashAttribute("error", "File foto tidak boleh kosong.");
            return "redirect:/dashboard/pengaturan";
        }

        try {
            Path uploadPath = Paths.get(UPLOAD_DIR + "profil/");
            Files.createDirectories(uploadPath);
            String filename = "profil_" + id + "_" + System.currentTimeMillis()
                + getExtension(foto.getOriginalFilename());
            Path target = uploadPath.resolve(filename);
            Files.copy(foto.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String fotoUrl = "/uploads/profil/" + filename;

            // Simpan ke UserPreferences
            UserPreferences pref = penggunaService.getOrCreatePreferences(id);
            pref.setFotoProfilUrl(fotoUrl);
            penggunaService.simpanPreferences(id, pref);

            // Simpan ke entity Pengguna (persisten ke H2)
            penggunaRepository.findById(id).ifPresent(p -> {
                p.setFotoProfil(fotoUrl);
                penggunaRepository.save(p);
            });

            flash.addFlashAttribute("sukses", "Foto profil berhasil diperbarui!");
            flash.addFlashAttribute("fotoProfilUrl", fotoUrl);
        } catch (IOException e) {
            flash.addFlashAttribute("error", "Gagal upload foto: " + e.getMessage());
        }
        return "redirect:/dashboard/pengaturan";
    }

    // ============================================================
    // UNGGAH MATERI BELAJAR BARU
    // ============================================================

    /**
     * POST /profil/materi/unggah — simpan materi baru ke DB.
     */
    @PostMapping("/materi/unggah")
    public String unggahMateri(@RequestParam String judul,
                                @RequestParam(required = false) String deskripsi,
                                @RequestParam String jenisMateri,
                                @RequestParam(required = false) String mataPelajaran,
                                @RequestParam(required = false) String tautanMateri,
                                @RequestParam(value = "fileMateri", required = false) MultipartFile fileMateri,
                                @RequestParam(value = "sampulFile", required = false) MultipartFile sampulFile,
                                HttpSession session, RedirectAttributes flash) {
        Long id = (Long) session.getAttribute("penggunaId");
        if (id == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(id).orElse(null);
        if (pengguna == null) return "redirect:/login";

        try {
            MateriBelajar materi = new MateriBelajar();
            materi.setJudul(judul.trim());
            materi.setDeskripsi(deskripsi);
            materi.setJenisMateri(jenisMateri);
            materi.setMataPelajaran(mataPelajaran);
            materi.setPengunggah(pengguna);
            materi.setTipeKonten("MATERI");

            // Upload file materi
            if (fileMateri != null && !fileMateri.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIR + "materi/");
                Files.createDirectories(uploadPath);
                String filename = "materi_" + id + "_" + System.currentTimeMillis()
                    + getExtension(fileMateri.getOriginalFilename());
                Path target = uploadPath.resolve(filename);
                Files.copy(fileMateri.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                materi.setTautanMateri("/uploads/materi/" + filename);
                materi.setNamaFile(fileMateri.getOriginalFilename());
                materi.setUkuranFile(fileMateri.getSize());
            } else if (tautanMateri != null && !tautanMateri.isBlank()) {
                materi.setTautanMateri(tautanMateri.trim());
            }

            // Upload sampul/cover image
            if (sampulFile != null && !sampulFile.isEmpty()) {
                Path sampulPath = Paths.get(UPLOAD_DIR + "sampul/");
                Files.createDirectories(sampulPath);
                String sampulFilename = "sampul_" + id + "_" + System.currentTimeMillis()
                    + getExtension(sampulFile.getOriginalFilename());
                Path sampulTarget = sampulPath.resolve(sampulFilename);
                Files.copy(sampulFile.getInputStream(), sampulTarget, StandardCopyOption.REPLACE_EXISTING);
                materi.setSampulUrl("/uploads/sampul/" + sampulFilename);
            }

            mentoringService.unggahMateri(materi);
            flash.addFlashAttribute("sukses", "Materi \"" + judul + "\" berhasil diunggah!");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal mengunggah materi: " + e.getMessage());
        }
        return "redirect:/dashboard/materi";
    }

    // ============================================================
    // UNGGAH SUMBER DAYA BARU
    // ============================================================

    /**
     * POST /profil/sumber-daya/unggah — same as materi, kategori = sumber daya.
     */
    @PostMapping("/sumber-daya/unggah")
    public String unggahSumberDaya(@RequestParam String judul,
                                    @RequestParam(required = false) String deskripsi,
                                    @RequestParam(required = false) String kategori,
                                    @RequestParam(required = false) String tautan,
                                    @RequestParam(value = "fileSumberDaya", required = false) MultipartFile fileSumberDaya,
                                    HttpSession session, RedirectAttributes flash) {
        Long id = (Long) session.getAttribute("penggunaId");
        if (id == null) return "redirect:/login";

        Pengguna pengguna = penggunaService.getPenggunaById(id).orElse(null);
        if (pengguna == null) return "redirect:/login";

        try {
            MateriBelajar sumberDaya = new MateriBelajar();
            sumberDaya.setJudul(judul.trim());
            sumberDaya.setDeskripsi(deskripsi);
            sumberDaya.setJenisMateri(kategori != null ? kategori : "DOKUMEN");
            sumberDaya.setTipeKonten("SUMBER_DAYA");
            sumberDaya.setPengunggah(pengguna);

            if (fileSumberDaya != null && !fileSumberDaya.isEmpty()) {
                Path uploadPath = Paths.get(UPLOAD_DIR + "sumber-daya/");
                Files.createDirectories(uploadPath);
                String filename = "sd_" + id + "_" + System.currentTimeMillis()
                    + getExtension(fileSumberDaya.getOriginalFilename());
                Path target = uploadPath.resolve(filename);
                Files.copy(fileSumberDaya.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                sumberDaya.setTautanMateri("/uploads/sumber-daya/" + filename);
                sumberDaya.setNamaFile(fileSumberDaya.getOriginalFilename());
                sumberDaya.setUkuranFile(fileSumberDaya.getSize());
            } else if (tautan != null && !tautan.isBlank()) {
                sumberDaya.setTautanMateri(tautan.trim());
            }

            mentoringService.unggahMateri(sumberDaya);
            flash.addFlashAttribute("sukses", "Sumber daya \"" + judul + "\" berhasil ditambahkan!");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal menambahkan sumber daya: " + e.getMessage());
        }
        return "redirect:/dashboard/sumber-daya";
    }

    // ============================================================
    // HAPUS & EDIT MATERI / SUMBER DAYA
    // ============================================================

    @PostMapping("/materi/{id}/hapus")
    public String hapusMateri(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            mentoringService.getMateriById(id).ifPresentOrElse(materi -> {
                if (!materi.getPengunggah().getId().equals(penggunaId)) {
                    flash.addFlashAttribute("error", "Materi bukan milik Anda.");
                } else {
                    mentoringService.hapusMateri(id);
                    flash.addFlashAttribute("sukses", "Materi berhasil dihapus.");
                }
            }, () -> flash.addFlashAttribute("error", "Materi tidak ditemukan."));
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal menghapus: " + e.getMessage());
        }
        return "redirect:/dashboard/materi";
    }

    @PostMapping("/materi/{id}/hapus-sd")
    public String hapusSumberDaya(@PathVariable Long id, HttpSession session, RedirectAttributes flash) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        try {
            mentoringService.getMateriById(id).ifPresentOrElse(sd -> {
                if (!sd.getPengunggah().getId().equals(penggunaId)) {
                    flash.addFlashAttribute("error", "Sumber daya bukan milik Anda.");
                } else {
                    mentoringService.hapusMateri(id);
                    flash.addFlashAttribute("sukses", "Sumber daya berhasil dihapus.");
                }
            }, () -> flash.addFlashAttribute("error", "Sumber daya tidak ditemukan."));
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal menghapus: " + e.getMessage());
        }
        return "redirect:/dashboard/sumber-daya";
    }

    @PostMapping("/materi/{id}/edit")
    public String editMateri(@PathVariable Long id,
                              @RequestParam String judul,
                              @RequestParam(required = false) String deskripsi,
                              @RequestParam(required = false) String mataPelajaran,
                              @RequestParam(required = false) String jenisMateri,
                              @RequestParam(value = "sampulFile", required = false) MultipartFile sampulFile,
                              HttpSession session, RedirectAttributes flash) {
        Long penggunaId = (Long) session.getAttribute("penggunaId");
        if (penggunaId == null) return "redirect:/login";

        final String[] tipeKonten = {null};
        try {
            mentoringService.getMateriById(id).ifPresentOrElse(materi -> {
                if (!materi.getPengunggah().getId().equals(penggunaId)) {
                    flash.addFlashAttribute("error", "Materi bukan milik Anda.");
                    return;
                }
                tipeKonten[0] = materi.getTipeKonten();
                materi.setJudul(judul.trim());
                materi.setDeskripsi(deskripsi);
                if (mataPelajaran != null) materi.setMataPelajaran(mataPelajaran);
                if (jenisMateri != null && !jenisMateri.isBlank()) materi.setJenisMateri(jenisMateri);

                // Update sampul jika ada file baru
                if (sampulFile != null && !sampulFile.isEmpty()) {
                    try {
                        Path sampulPath = Paths.get(UPLOAD_DIR + "sampul/");
                        Files.createDirectories(sampulPath);
                        String sampulFilename = "sampul_" + penggunaId + "_" + System.currentTimeMillis()
                            + getExtension(sampulFile.getOriginalFilename());
                        Files.copy(sampulFile.getInputStream(), sampulPath.resolve(sampulFilename),
                            StandardCopyOption.REPLACE_EXISTING);
                        materi.setSampulUrl("/uploads/sampul/" + sampulFilename);
                    } catch (java.io.IOException ex) {
                        flash.addFlashAttribute("error", "Gagal upload sampul: " + ex.getMessage());
                        return;
                    }
                }
                mentoringService.unggahMateri(materi);
                flash.addFlashAttribute("sukses", "\"" + judul + "\" berhasil diperbarui!");
            }, () -> flash.addFlashAttribute("error", "Item tidak ditemukan."));
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Gagal mengedit: " + e.getMessage());
        }
        return "SUMBER_DAYA".equals(tipeKonten[0])
                ? "redirect:/dashboard/sumber-daya"
                : "redirect:/dashboard/materi";
    }

    // ============================================================
    // HELPER
    // ============================================================

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.'));
    }
}
