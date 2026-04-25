package com.example.backend_service.gpacalculator.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "gpa_subjects")
public class GPASubject extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Double credits;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GradeEnum grade;

    @Column(nullable = false)
    private Boolean isGpa = true;

    @Column(nullable = false, length = 36)
    private String studentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "semester_id", nullable = false, foreignKey = @ForeignKey(name = "fk_subject_semester"))
    private GPASemester semester;

    public GPASubject() {
    }

    public GPASubject(String name, Double credits, GradeEnum grade, Boolean isGpa, String studentId,
            GPASemester semester) {
        this.name = name;
        this.credits = credits;
        this.grade = grade;
        this.isGpa = isGpa;
        this.studentId = studentId;
        this.semester = semester;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getCredits() {
        return credits;
    }

    public void setCredits(Double credits) {
        this.credits = credits;
    }

    public GradeEnum getGrade() {
        return grade;
    }

    public void setGrade(GradeEnum grade) {
        this.grade = grade;
    }

    public Boolean getIsGpa() {
        return isGpa;
    }

    public void setIsGpa(Boolean isGpa) {
        this.isGpa = isGpa;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public GPASemester getSemester() {
        return semester;
    }

    public void setSemester(GPASemester semester) {
        this.semester = semester;
    }
}
