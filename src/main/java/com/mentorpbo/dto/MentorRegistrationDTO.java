package com.mentorpbo.dto;

import java.io.Serializable;

public class MentorRegistrationDTO implements Serializable {

    private String namaLengkap;
    private String nimNisn;
    private String tingkatPendidikan;
    private String institusi;
    private String email;
    private String kataSandi;
    private String ipk;
    private String portofolioUrl;

    // Step 2: Profil Akademik
    private String kelas;
    private String semester;
    private String programStudi;
    private String fakultas;
    private String keahlian;
    private String topikKeahlian;

    // Step 3: Motivasi
    private String motivasi;

    public MentorRegistrationDTO() {
    }

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

    public String getIpk() { return ipk; }
    public void setIpk(String ipk) { this.ipk = ipk; }

    public String getPortofolioUrl() { return portofolioUrl; }
    public void setPortofolioUrl(String portofolioUrl) { this.portofolioUrl = portofolioUrl; }

    public String getKelas() { return kelas; }
    public void setKelas(String kelas) { this.kelas = kelas; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getProgramStudi() { return programStudi; }
    public void setProgramStudi(String programStudi) { this.programStudi = programStudi; }

    public String getFakultas() { return fakultas; }
    public void setFakultas(String fakultas) { this.fakultas = fakultas; }

    public String getKeahlian() { return keahlian; }
    public void setKeahlian(String keahlian) { this.keahlian = keahlian; }

    public String getTopikKeahlian() { return topikKeahlian; }
    public void setTopikKeahlian(String topikKeahlian) { this.topikKeahlian = topikKeahlian; }

    public String getMotivasi() { return motivasi; }
    public void setMotivasi(String motivasi) { this.motivasi = motivasi; }

    public boolean isSiswa() {
        return tingkatPendidikan != null && tingkatPendidikan.startsWith("SMA");
    }
}
