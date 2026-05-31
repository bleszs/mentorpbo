package com.mentorpbo.dto;

/**
 * DTO untuk pendaftaran Pengawas Akademik (Guru/Dosen).
 * Digunakan untuk menangkap data dari form registrasi pengawas.
 */
public class PengawasRegistrationDTO {
    
    private String namaLengkap;
    private String gelarAkademik;
    private String email;
    private String namaInstitusi;
    private String nidnNip;
    private String departemen;
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

    public boolean isSetujuSyaratKetentuan() {
        return setujuSyaratKetentuan;
    }

    public void setSetujuSyaratKetentuan(boolean setujuSyaratKetentuan) {
        this.setujuSyaratKetentuan = setujuSyaratKetentuan;
    }
}
