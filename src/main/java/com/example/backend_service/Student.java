package com.example.backend_service;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;      // Maps to formData.name
    private String degree;    // Maps to formData.degree
    private String email;     // Maps to formData.email
    private String username;  // Maps to formData.username
    private String password;  // Maps to formData.password

    // 1. Default Constructor (Required by JPA)
    public Student() {}

    // 2. Full Parameterized Constructor
    public Student(String name, String degree, String email, String username, String password) {
        this.name = name;
        this.degree = degree;
        this.email = email;
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}