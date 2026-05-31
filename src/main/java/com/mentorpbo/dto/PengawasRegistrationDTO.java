package com.mentorpbo.dto;

/**
 * DTO untuk pendaftaran Pengawas Akademik (Guru/Dosen).
 * Digunakan untuk menangkap data dari form registrasi pengawas.
 */
public class PengawasRegistrationDTO {

    private String namaLengkap;
    private String gelarAkademik;
    private String email;
    private String kataSandi;
    private String namaInstitusi;
    private String nidnNip;
    private String departemen;
    private String jabatan;
    private String minatRiset;
    private String pengalaman;
    private String tipeInstitusi; // "SEKOLAH" atau "KAMPUS"
    private boolean setujuSyaratKetentuan;

    // Constructors
    public PengawasRegistrationDTO() {
    }

    public PengawasRegistrationDTO(String namaLengkap, String gelarAkademik, String email, 
                                   String namaInstitusi, String nidnNip, String departemen) {
        this.namaLengkap = namaLengkap;
        this.gelarAkademik = gelarAkademik;
        this.email = email;
        this.namaInstitusi = namaInstitusi;
        this.nidnNip = nidnNip;
        this.departemen = departemen;
    }

    // Getters and Setters
    public String getNamaLengkap() {
        return namaLengkap;
    }

    public void setNamaLengkap(String namaLengkap) {
        this.namaLengkap = namaLengkap;
    }

    public String getGelarAkademik() {
        return gelarAkademik;
    }

    public void setGelarAkademik(String gelarAkademik) {
        this.gelarAkademik = gelarAkademik;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNamaInstitusi() {
        return namaInstitusi;
    }

    public void setNamaInstitusi(String namaInstitusi) {
        this.namaInstitusi = namaInstitusi;
    }

    public String getNidnNip() {
        return nidnNip;
    }

    public void setNidnNip(String nidnNip) {
        this.nidnNip = nidnNip;
    }

    public String getDepartemen() {
        return departemen;
    }

    public void setDepartemen(String departemen) {
        this.departemen = departemen;
    }

    public String getKataSandi() { return kataSandi; }
    public void setKataSandi(String kataSandi) { this.kataSandi = kataSandi; }

    public String getJabatan() { return jabatan; }
    public void setJabatan(String jabatan) { this.jabatan = jabatan; }

    public String getMinatRiset() { return minatRiset; }
    public void setMinatRiset(String minatRiset) { this.minatRiset = minatRiset; }

    public String getPengalaman() { return pengalaman; }
    public void setPengalaman(String pengalaman) { this.pengalaman = pengalaman; }

    public String getTipeInstitusi() { return tipeInstitusi; }
    public void setTipeInstitusi(String tipeInstitusi) { this.tipeInstitusi = tipeInstitusi; }

    public boolean isSetujuSyaratKetentuan() { return setujuSyaratKetentuan; }
    public void setSetujuSyaratKetentuan(boolean setujuSyaratKetentuan) { this.setujuSyaratKetentuan = setujuSyaratKetentuan; }
}
