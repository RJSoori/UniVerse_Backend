package com.example.backend_service.gpacalculator.web;

import com.example.backend_service.gpacalculator.model.GradeEnum;
import com.example.backend_service.gpacalculator.model.GPASemester;
import com.example.backend_service.gpacalculator.model.GPASettings;
import com.example.backend_service.gpacalculator.model.GPASubject;
import com.example.backend_service.gpacalculator.repository.GPASemesterRepository;
import com.example.backend_service.gpacalculator.repository.GPASettingsRepository;
import com.example.backend_service.gpacalculator.repository.GPASubjectRepository;
import com.example.backend_service.gpacalculator.service.GPACalculatorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for GPA Calculator endpoints.
 * Provides CRUD operations for semesters/subjects and analytics calculations.
 */
@RestController
@RequestMapping("/api/gpa")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" })
public class GPACalculatorController {

    private final GPACalculatorService gpaCalculatorService;
    private final GPASemesterRepository semesterRepository;
    private final GPASubjectRepository subjectRepository;
    private final GPASettingsRepository settingsRepository;

    public GPACalculatorController(
            GPACalculatorService gpaCalculatorService,
            GPASemesterRepository semesterRepository,
            GPASubjectRepository subjectRepository,
            GPASettingsRepository settingsRepository) {
        this.gpaCalculatorService = gpaCalculatorService;
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.settingsRepository = settingsRepository;
    }

    // ============================================================================
    // Settings Endpoints
    // ============================================================================

    @GetMapping("/settings/{studentId}")
    public ResponseEntity<GPASettings> getSettings(@PathVariable String studentId) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(studentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings/{studentId}")
    public ResponseEntity<GPASettings> updateSettings(
            @PathVariable String studentId,
            @RequestBody GPASettings updatedSettings) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(studentId);
        if (settings.getId() == null) {
            settings.setStudentId(studentId);
        }
        if (updatedSettings.getGpaScale() != null) {
            settings.setGpaScale(updatedSettings.getGpaScale());
        }
        if (updatedSettings.getGradingMode() != null) {
            settings.setGradingMode(updatedSettings.getGradingMode());
        }
        if (updatedSettings.getFirstClassThreshold() != null) {
            settings.setFirstClassThreshold(updatedSettings.getFirstClassThreshold());
        }
        if (updatedSettings.getSecondUpperThreshold() != null) {
            settings.setSecondUpperThreshold(updatedSettings.getSecondUpperThreshold());
        }
        if (updatedSettings.getSecondLowerThreshold() != null) {
            settings.setSecondLowerThreshold(updatedSettings.getSecondLowerThreshold());
        }
        if (updatedSettings.getGeneralThreshold() != null) {
            settings.setGeneralThreshold(updatedSettings.getGeneralThreshold());
        }
        settings = settingsRepository.save(settings);
        return ResponseEntity.ok(settings);
    }

    // ============================================================================
    // Semester CRUD Endpoints
    // ============================================================================

    @PostMapping("/semesters")
    public ResponseEntity<GPASemester> createSemester(@RequestBody GPASemester semester) {
        GPASemester saved = semesterRepository.save(semester);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/semesters/{studentId}")
    public ResponseEntity<List<GPASemester>> getSemestersByStudent(@PathVariable String studentId) {
        List<GPASemester> semesters = semesterRepository.findByStudentIdOrderByYearDescSemesterDesc(studentId);
        return ResponseEntity.ok(semesters);
    }

    @GetMapping("/semesters/{semesterId}/details")
    public ResponseEntity<GPASemester> getSemesterDetails(@PathVariable String semesterId) {
        Optional<GPASemester> semester = semesterRepository.findById(semesterId);
        return semester.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/semesters/{semesterId}")
    public ResponseEntity<GPASemester> updateSemester(
            @PathVariable String semesterId,
            @RequestBody GPASemester updatedSemester) {
        Optional<GPASemester> existing = semesterRepository.findById(semesterId);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        GPASemester semester = existing.get();
        if (updatedSemester.getYear() != null) {
            semester.setYear(updatedSemester.getYear());
        }
        if (updatedSemester.getSemester() != null) {
            semester.setSemester(updatedSemester.getSemester());
        }
        semesterRepository.save(semester);
        return ResponseEntity.ok(semester);
    }

    @DeleteMapping("/semesters/{semesterId}")
    public ResponseEntity<Void> deleteSemester(@PathVariable String semesterId) {
        semesterRepository.deleteById(semesterId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // Subject CRUD Endpoints
    // ============================================================================

    @PostMapping("/subjects")
    public ResponseEntity<GPASubject> createSubject(@RequestBody GPASubject subject) {
        GPASubject saved = subjectRepository.save(subject);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/semesters/{semesterId}/subjects")
    public ResponseEntity<List<GPASubject>> getSubjectsBySemester(@PathVariable String semesterId) {
        List<GPASubject> subjects = subjectRepository.findBySemesterId(semesterId);
        return ResponseEntity.ok(subjects);
    }

    @PutMapping("/subjects/{subjectId}")
    public ResponseEntity<GPASubject> updateSubject(
            @PathVariable String subjectId,
            @RequestBody GPASubject updatedSubject) {
        Optional<GPASubject> existing = subjectRepository.findById(subjectId);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        GPASubject subject = existing.get();
        if (updatedSubject.getName() != null) {
            subject.setName(updatedSubject.getName());
        }
        if (updatedSubject.getCredits() != null) {
            subject.setCredits(updatedSubject.getCredits());
        }
        if (updatedSubject.getGrade() != null) {
            subject.setGrade(updatedSubject.getGrade());
        }
        if (updatedSubject.getIsGpa() != null) {
            subject.setIsGpa(updatedSubject.getIsGpa());
        }
        subject = subjectRepository.save(subject);
        return ResponseEntity.ok(subject);
    }

    @DeleteMapping("/subjects/{subjectId}")
    public ResponseEntity<Void> deleteSubject(@PathVariable String subjectId) {
        subjectRepository.deleteById(subjectId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // Analytics Endpoints
    // ============================================================================

    @GetMapping("/analytics/sgpa/{semesterId}")
    public ResponseEntity<Map<String, Double>> calculateSGPA(@PathVariable String semesterId) {
        Optional<GPASemester> semester = semesterRepository.findById(semesterId);
        if (semester.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String studentId = semester.get().getStudentId();
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(studentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double sgpa = gpaCalculatorService.calculateSGPA(semester.get(), settings);
        Map<String, Double> response = new HashMap<>();
        response.put("sgpa", sgpa);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/cgpa/{studentId}")
    public ResponseEntity<Map<String, Double>> calculateCGPA(@PathVariable String studentId) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(studentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double cgpa = gpaCalculatorService.calculateCGPA(studentId, settings);
        String degreeClass = gpaCalculatorService.classifyDegreeClass(cgpa, settings);
        Map<String, Double> response = new HashMap<>();
        response.put("cgpa", cgpa);
        response.put("degreeClass", (double) degreeClass.hashCode()); // Placeholder for string
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/degree-class/{studentId}")
    public ResponseEntity<Map<String, String>> getDegreeClass(@PathVariable String studentId) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(studentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double cgpa = gpaCalculatorService.calculateCGPA(studentId, settings);
        String degreeClass = gpaCalculatorService.classifyDegreeClass(cgpa, settings);
        Map<String, String> response = new HashMap<>();
        response.put("degreeClass", degreeClass);
        response.put("cgpa", String.format("%.3f", cgpa));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analytics/required-sgpa")
    public ResponseEntity<Map<String, Double>> calculateRequiredSGPA(
            @RequestParam String studentId,
            @RequestParam double targetCgpa,
            @RequestParam(defaultValue = "18.0") double nextSemesterCredits) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(studentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double requiredSgpa = gpaCalculatorService.calculateRequiredSGPA(studentId, targetCgpa, nextSemesterCredits,
                settings);
        Map<String, Double> response = new HashMap<>();
        response.put("requiredSgpa", requiredSgpa);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analytics/prediction")
    public ResponseEntity<Map<String, Object>> predictDegreeClass(
            @RequestParam String studentId,
            @RequestParam String targetDegreeClass,
            @RequestParam(defaultValue = "1000") int simulations,
            @RequestBody List<Double> nextSemesterSubjects) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(studentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double probability = gpaCalculatorService.predictDegreeClassProbability(
                studentId, targetDegreeClass, nextSemesterSubjects, settings, simulations);
        Map<String, Object> response = new HashMap<>();
        response.put("probability", probability);
        response.put("targetDegreeClass", targetDegreeClass);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "GPA Calculator API");
        return ResponseEntity.ok(response);
    }
}
