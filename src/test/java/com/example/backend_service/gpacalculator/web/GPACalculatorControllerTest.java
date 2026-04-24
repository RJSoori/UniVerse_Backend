package com.example.backend_service.gpacalculator.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class GPACalculatorControllerTest {

    @Autowired
    private GPACalculatorController gpaCalculatorController;

    @Test
    public void testContextLoads() {
        assertNotNull(gpaCalculatorController);
    }
}
