package com.example.backend_service.gpacalculator.service;

import com.example.backend_service.gpacalculator.model.GradeEnum;
import com.example.backend_service.gpacalculator.model.GradeScaleMode;
import com.example.backend_service.gpacalculator.model.GPASemester;
import com.example.backend_service.gpacalculator.model.GPASettings;
import com.example.backend_service.gpacalculator.model.GPASubject;
import com.example.backend_service.gpacalculator.repository.GPASemesterRepository;
import com.example.backend_service.gpacalculator.repository.GPASettingsRepository;
import com.example.backend_service.gpacalculator.repository.GPASubjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class GPACalculatorServiceTest {

    @Autowired
    private GPACalculatorService gpaCalculatorService;

    @Autowired
    private GPASemesterRepository semesterRepository;

    @Autowired
    private GPASubjectRepository subjectRepository;

    @Autowired
    private GPASettingsRepository settingsRepository;

    @Test
    public void testContextLoads() {
        assertNotNull(gpaCalculatorService);
        assertNotNull(semesterRepository);
    }

    @Test
    public void testGetGradePoint_Standard_4_0() {
        assertEquals(4.0, gpaCalculatorService.getGradePoint(GradeEnum.PLUS_A, GradeScaleMode.STANDARD_4_0));
        assertEquals(4.0, gpaCalculatorService.getGradePoint(GradeEnum.A, GradeScaleMode.STANDARD_4_0));
        assertEquals(3.7, gpaCalculatorService.getGradePoint(GradeEnum.MINUS_A, GradeScaleMode.STANDARD_4_0));
        assertEquals(0.0, gpaCalculatorService.getGradePoint(GradeEnum.F, GradeScaleMode.STANDARD_4_0));
    }

    @Test
    public void testGetGradePoint_Extended_4_2() {
        assertEquals(4.2, gpaCalculatorService.getGradePoint(GradeEnum.PLUS_A, GradeScaleMode.EXTENDED_4_2));
        assertEquals(4.0, gpaCalculatorService.getGradePoint(GradeEnum.A, GradeScaleMode.EXTENDED_4_2));
    }

    @Test
    public void testClassifyDegreeClass() {
        GPASettings settings = new GPASettings(1L);
        assertEquals("First Class", gpaCalculatorService.classifyDegreeClass(3.7, settings));
        assertEquals("Second Upper", gpaCalculatorService.classifyDegreeClass(3.3, settings));
        assertEquals("Second Lower", gpaCalculatorService.classifyDegreeClass(3.0, settings));
        assertEquals("General Degree", gpaCalculatorService.classifyDegreeClass(2.0, settings));
    }

    @Test
    public void testGetOrCreateSettings() {
        Long testStudentId = System.currentTimeMillis();
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(testStudentId);
        assertEquals(testStudentId, settings.getStudentId());
        assertEquals(GradeScaleMode.STANDARD_4_0, settings.getGpaScale());
    }
}
