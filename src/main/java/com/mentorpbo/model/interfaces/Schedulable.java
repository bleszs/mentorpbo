package com.mentorpbo.model.interfaces;

import java.time.LocalDateTime;

/**
 * Interface Schedulable - Kontrak untuk entitas yang dapat dijadwalkan.
 *
 * Interface ini menerapkan prinsip ABSTRAKSI dalam OOP.
 * Setiap sesi mentoring (Online, Offline, Video) harus mengimplementasikan
 * kontrak penjadwalan ini agar sistem dapat mengelola jadwal secara seragam
 * tanpa mengetahui detail implementasi spesifik masing-masing jenis sesi.
 *
 * Contoh penggunaan polimorfisme:
 *   Schedulable sesi = new SesiOnline();
 *   sesi.jadwalkanSesi(waktu);  // Perilaku spesifik SesiOnline
 */
public interface Schedulable {

    /**
     * Menjadwalkan sesi pada waktu tertentu.
     *
     * @param waktuMulai waktu mulai sesi yang dijadwalkan
     * @param durasiMenit durasi sesi dalam satuan menit
     * @return true jika penjadwalan berhasil, false jika gagal
     */
    boolean jadwalkanSesi(LocalDateTime waktuMulai, int durasiMenit);

    /**
     * Mengubah jadwal sesi yang sudah ada ke waktu baru.
     *
     * @param waktuBaru waktu mulai baru untuk sesi
     * @return true jika perubahan jadwal berhasil
     */
    boolean ubahJadwal(LocalDateTime waktuBaru);

    /**
     * Membatalkan sesi yang sudah dijadwalkan.
     * Pembatalan harus menyertakan alasan untuk keperluan audit trail.
     *
     * @param alasan alasan pembatalan sesi
     * @return true jika pembatalan berhasil
     */
    boolean batalkanJadwal(String alasan);

    /**
     * Mengecek apakah sesi masih bisa dijadwalkan ulang.
     * Sesi yang sudah selesai atau dibatalkan tidak dapat dijadwalkan ulang.
     *
     * @return true jika sesi masih dapat dijadwalkan ulang
     */
    boolean dapatDijadwalkanUlang();
}
