package com.example.backend_service.todo;

import jakarta.persistence.*;

/**
 * Database entity for a student's to-do list.
 * Saves all tasks as a single JSON block.
 */
@Entity
@Table(name = "student_todo_state")
public class StudentTodoState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false, unique = true)
    private Long studentId;

    @Lob
    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}