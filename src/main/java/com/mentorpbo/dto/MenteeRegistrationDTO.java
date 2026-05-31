package com.mentorpbo.dto;

public class MenteeRegistrationDTO {

    private String namaLengkap;
    private String nimNisn;
    private String tingkatPendidikan;
    private String institusi;
    private String email;
    private String kataSandi;
    private String minatBelajar;

    public MenteeRegistrationDTO() {}

    public String getNamaLengkap() { return namaLengkap; }
    public void setNamaLengkap(String namaLengkap) { this.namaLengkap = namaLengkap; }

    public String getNimNisn() { return nimNisn; }
    public void setNimNisn(String nimNisn) { this.nimNisn = nimNisn; }

    public String getTingkatPendidikan() { return tingkatPendidikan; }
    public void setTingkatPendidikan(String tingkatPendidikan) { this.tingkatPendidikan = tingkatPendidikan; }

    public String getInstitusi() { return institusi; }
    public void setInstitusi(String institusi) { this.institusi = institusi; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getKataSandi() { return kataSandi; }
    public void setKataSandi(String kataSandi) { this.kataSandi = kataSandi; }

    public String getMinatBelajar() { return minatBelajar; }
    public void setMinatBelajar(String minatBelajar) { this.minatBelajar = minatBelajar; }

    public boolean isSiswa() {
        return tingkatPendidikan != null && tingkatPendidikan.startsWith("SMA");
    }
}
