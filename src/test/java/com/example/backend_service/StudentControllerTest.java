package com.example.backend_service;

import com.example.backend_service.auth.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentController controller;

    @Test
    void getAllStudents_mapsStudentsToUserDtos() {
        Student student = new Student();
        student.setId(1L);
        student.setUsername("sam");
        student.setName("Sam");
        student.setEmail("sam@example.com");
        student.setDegree("IT");
        student.setRole(Role.STUDENT);

        when(studentRepository.findAll()).thenReturn(List.of(student));

        List<UserDto> result = controller.getAllStudents();

        assertThat(result).containsExactly(UserDto.from(student));
    }
}