package com.mentorpbo.model.interfaces;

import com.mentorpbo.model.enums.StatusValidasi;

/**
 * Interface Verifiable - Kontrak untuk entitas yang dapat divalidasi oleh supervisor.
 *
 * Interface ini menerapkan prinsip ABSTRAKSI dalam OOP.
 * Guru dan Dosen sebagai supervisor memiliki kewenangan untuk
 * memvalidasi atau menolak kegiatan mentoring yang dilakukan
 * oleh siswa/mahasiswa di bawah bimbingannya.
 *
 * Alur validasi:
 * BELUM_DITINJAU → SEDANG_DITINJAU → DIVALIDASI / DITOLAK
 */
public interface Verifiable {

    /**
     * Memvalidasi dan menyetujui kegiatan/entitas ini.
     * Hanya supervisor yang berwenang yang dapat melakukan validasi.
     *
     * @param idSupervisor ID supervisor yang melakukan validasi
     * @param catatan catatan atau komentar dari supervisor
     */
    void validasi(Long idSupervisor, String catatan);

    /**
     * Menolak kegiatan/entitas ini dengan alasan yang jelas.
     *
     * @param idSupervisor ID supervisor yang menolak
     * @param alasanPenolakan alasan mengapa kegiatan ditolak
     */
    void tolakValidasi(Long idSupervisor, String alasanPenolakan);

    /**
     * Mendapatkan status validasi terkini dari entitas ini.
     *
     * @return status validasi saat ini
     */
    StatusValidasi getStatusValidasi();

    /**
     * Mengecek apakah entitas ini sudah divalidasi oleh supervisor.
     *
     * @return true jika status validasi adalah DIVALIDASI
     */
    boolean sudahDivalidasi();

    /**
     * Mengecek apakah entitas ini masih menunggu peninjauan.
     *
     * @return true jika status masih BELUM_DITINJAU atau SEDANG_DITINJAU
     */
    boolean menungguPeninjauan();
}
