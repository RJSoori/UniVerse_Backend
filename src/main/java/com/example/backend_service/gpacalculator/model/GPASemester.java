package com.example.backend_service.gpacalculator.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gpa_semesters")
public class GPASemester extends BaseEntity {

    @Column(name = "semester_year", nullable = false, length = 20)
    private String year;

    @Column(nullable = false, length = 20)
    private String semester;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<GPASubject> subjects = new ArrayList<>();

    public GPASemester() {
    }

    public GPASemester(String year, String semester, Long studentId) {
        this.year = year;
        this.semester = semester;
        this.studentId = studentId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public List<GPASubject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<GPASubject> subjects) {
        this.subjects = subjects;
    }

    public void addSubject(GPASubject subject) {
        subjects.add(subject);
        subject.setSemester(this);
    }

    public void removeSubject(GPASubject subject) {
        subjects.remove(subject);
        subject.setSemester(null);
    }
}
