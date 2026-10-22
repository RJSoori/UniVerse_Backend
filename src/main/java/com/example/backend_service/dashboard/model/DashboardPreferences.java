package com.example.backend_service.dashboard.model;

import com.example.backend_service.common.model.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "dashboard_preferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id"})
})
public class DashboardPreferences extends BaseEntity {

    @JsonIgnore
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Size(max = 50000)
    @Lob
    @Column(name = "layout_json", columnDefinition = "TEXT")
    private String layoutJson;

    public DashboardPreferences() {
    }

    public DashboardPreferences(String layoutJson) {
        this.layoutJson = layoutJson;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getLayoutJson() {
        return layoutJson;
    }

    public void setLayoutJson(String layoutJson) {
        this.layoutJson = layoutJson;
    }
}
