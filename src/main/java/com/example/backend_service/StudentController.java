package com.example.backend_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    @PostMapping("/register")
    public String registerStudent(@RequestBody Student student) {
        studentRepository.save(student);
        return "Registration Successful!";
    }

    @GetMapping("/all")
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @PostMapping("/login")
    public String loginStudent(@RequestBody Student loginData) {
        // We check the database for the user
        return studentRepository.findAll().stream()
            .filter(s -> s.getUsername().equals(loginData.getUsername()))
            .findFirst()
            .map(student -> {
                // If the user exists, check the password
                if (student.getPassword().equals(loginData.getPassword())) {
                    return "Login Successful!";
                } else {
                    return "Invalid Password";
                }
            })
            .orElse("User Not Found");
    }
}