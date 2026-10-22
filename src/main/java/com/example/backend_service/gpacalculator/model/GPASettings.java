package com.example.backend_service.gpacalculator.model;

import com.example.backend_service.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "gpa_settings")
public class GPASettings extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GradeScaleMode gpaScale = GradeScaleMode.STANDARD_4_0;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GradeScaleMode gradingMode = GradeScaleMode.STANDARD_4_0;

    @Column(nullable = false)
    private Double firstClassThreshold = 3.7;

    @Column(nullable = false)
    private Double secondUpperThreshold = 3.3;

    @Column(nullable = false)
    private Double secondLowerThreshold = 3.0;

    @Column(nullable = false)
    private Double generalThreshold = 2.0;

    public GPASettings() {
    }

    public GPASettings(Long studentId) {
        this.studentId = studentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public GradeScaleMode getGpaScale() {
        return gpaScale;
    }

    public void setGpaScale(GradeScaleMode gpaScale) {
        this.gpaScale = gpaScale;
    }

    public GradeScaleMode getGradingMode() {
        return gradingMode;
    }

    public void setGradingMode(GradeScaleMode gradingMode) {
        this.gradingMode = gradingMode;
    }

    public Double getFirstClassThreshold() {
        return firstClassThreshold;
    }

    public void setFirstClassThreshold(Double firstClassThreshold) {
        this.firstClassThreshold = firstClassThreshold;
    }

    public Double getSecondUpperThreshold() {
        return secondUpperThreshold;
    }

    public void setSecondUpperThreshold(Double secondUpperThreshold) {
        this.secondUpperThreshold = secondUpperThreshold;
    }

    public Double getSecondLowerThreshold() {
        return secondLowerThreshold;
    }

    public void setSecondLowerThreshold(Double secondLowerThreshold) {
        this.secondLowerThreshold = secondLowerThreshold;
    }

    public Double getGeneralThreshold() {
        return generalThreshold;
    }

    public void setGeneralThreshold(Double generalThreshold) {
        this.generalThreshold = generalThreshold;
    }
}
