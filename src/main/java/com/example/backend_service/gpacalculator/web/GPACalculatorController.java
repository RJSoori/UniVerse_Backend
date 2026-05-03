package com.example.backend_service.gpacalculator.web;

import com.example.backend_service.common.exception.NotFoundException;
import com.example.backend_service.gpacalculator.model.GPASemester;
import com.example.backend_service.gpacalculator.model.GPASettings;
import com.example.backend_service.gpacalculator.model.GPASubject;
import com.example.backend_service.gpacalculator.model.GradeEnum;
import com.example.backend_service.gpacalculator.repository.GPASemesterRepository;
import com.example.backend_service.gpacalculator.repository.GPASettingsRepository;
import com.example.backend_service.gpacalculator.repository.GPASubjectRepository;
import com.example.backend_service.gpacalculator.service.GPACalculatorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

/**
 * REST Controller for GPA Calculator endpoints.
 * Provides CRUD operations for semesters/subjects and analytics calculations.
 * studentId is always derived from the JWT principal — never from path/request params.
 */
@RestController
@RequestMapping("/api/gpa")
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

    @GetMapping("/settings")
    public ResponseEntity<GPASettings> getSettings(@AuthenticationPrincipal Long authStudentId) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(authStudentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    public ResponseEntity<GPASettings> updateSettings(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody GPASettings updatedSettings) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(authStudentId);
        if (settings.getId() == null) {
            settings.setStudentId(authStudentId);
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
    public ResponseEntity<GPASemester> createSemester(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody GPASemester semester) {
        semester.setId(null); // reject client-supplied id — prevents JPA merge overwrite
        semester.setStudentId(authStudentId);
        GPASemester saved = semesterRepository.save(semester);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/semesters")
    public ResponseEntity<List<GPASemester>> getSemestersByStudent(@AuthenticationPrincipal Long authStudentId) {
        List<GPASemester> semesters = semesterRepository.findByStudentIdOrderByYearDescSemesterDesc(authStudentId);
        return ResponseEntity.ok(semesters);
    }

    @GetMapping("/semesters/{semesterId}")
    public ResponseEntity<GPASemester> getSemesterDetails(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable String semesterId) {
        GPASemester semester = semesterRepository.findById(semesterId)
                .filter(s -> s.getStudentId().equals(authStudentId))
                .orElseThrow(NotFoundException::new);
        return ResponseEntity.ok(semester);
    }

    @PutMapping("/semesters/{semesterId}")
    public ResponseEntity<GPASemester> updateSemester(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable String semesterId,
            @RequestBody GPASemester updatedSemester) {
        GPASemester semester = semesterRepository.findById(semesterId)
                .filter(s -> s.getStudentId().equals(authStudentId))
                .orElseThrow(NotFoundException::new);
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
    public ResponseEntity<Void> deleteSemester(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable String semesterId) {
        semesterRepository.findById(semesterId)
                .filter(s -> s.getStudentId().equals(authStudentId))
                .orElseThrow(NotFoundException::new);
        semesterRepository.deleteById(semesterId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // Subject CRUD Endpoints
    // ============================================================================

    @PostMapping("/subjects")
    public ResponseEntity<GPASubject> createSubject(
            @AuthenticationPrincipal Long authStudentId,
            @RequestBody SubjectRequest request) {
        String semesterId = request.semester() == null ? null : request.semester().id();
        GPASemester semester = semesterRepository.findById(semesterId)
                .filter(s -> s.getStudentId().equals(authStudentId))
                .orElseThrow(NotFoundException::new);
        GPASubject subject = new GPASubject();
        subject.setName(request.name());
        subject.setCredits(request.credits());
        subject.setGrade(request.grade());
        subject.setIsGpa(request.isGpa() == null ? Boolean.TRUE : request.isGpa());
        subject.setStudentId(authStudentId);
        subject.setSemester(semester);
        GPASubject saved = subjectRepository.save(subject);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/semesters/{semesterId}/subjects")
    public ResponseEntity<List<GPASubject>> getSubjectsBySemester(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable String semesterId) {
        semesterRepository.findById(semesterId)
                .filter(s -> s.getStudentId().equals(authStudentId))
                .orElseThrow(NotFoundException::new);
        List<GPASubject> subjects = subjectRepository.findBySemesterId(semesterId);
        return ResponseEntity.ok(subjects);
    }

    @PutMapping("/subjects/{subjectId}")
    public ResponseEntity<GPASubject> updateSubject(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable String subjectId,
            @RequestBody GPASubject updatedSubject) {
        GPASubject subject = subjectRepository.findById(subjectId)
                .filter(s -> s.getSemester().getStudentId().equals(authStudentId))
                .orElseThrow(NotFoundException::new);
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
    public ResponseEntity<Void> deleteSubject(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable String subjectId) {
        subjectRepository.findById(subjectId)
                .filter(s -> s.getSemester().getStudentId().equals(authStudentId))
                .orElseThrow(NotFoundException::new);
        subjectRepository.deleteById(subjectId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================================
    // Analytics Endpoints
    // ============================================================================

    @GetMapping("/analytics/sgpa/{semesterId}")
    public ResponseEntity<Map<String, Double>> calculateSGPA(
            @AuthenticationPrincipal Long authStudentId,
            @PathVariable String semesterId) {
        GPASemester semester = semesterRepository.findById(semesterId)
                .filter(s -> s.getStudentId().equals(authStudentId))
                .orElseThrow(NotFoundException::new);
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(authStudentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double sgpa = gpaCalculatorService.calculateSGPA(semester, settings);
        Map<String, Double> response = new HashMap<>();
        response.put("sgpa", sgpa);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/cgpa")
    public ResponseEntity<Map<String, Double>> calculateCGPA(@AuthenticationPrincipal Long authStudentId) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(authStudentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double cgpa = gpaCalculatorService.calculateCGPA(authStudentId, settings);
        String degreeClass = gpaCalculatorService.classifyDegreeClass(cgpa, settings);
        Map<String, Double> response = new HashMap<>();
        response.put("cgpa", cgpa);
        response.put("degreeClass", (double) degreeClass.hashCode()); // Placeholder for string
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/degree-class")
    public ResponseEntity<Map<String, String>> getDegreeClass(@AuthenticationPrincipal Long authStudentId) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(authStudentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double cgpa = gpaCalculatorService.calculateCGPA(authStudentId, settings);
        String degreeClass = gpaCalculatorService.classifyDegreeClass(cgpa, settings);
        Map<String, String> response = new HashMap<>();
        response.put("degreeClass", degreeClass);
        response.put("cgpa", String.format("%.3f", cgpa));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analytics/required-sgpa")
    public ResponseEntity<Map<String, Double>> calculateRequiredSGPA(
            @AuthenticationPrincipal Long authStudentId,
            @RequestParam double targetCgpa,
            @RequestParam(defaultValue = "18.0") double nextSemesterCredits) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(authStudentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double requiredSgpa = gpaCalculatorService.calculateRequiredSGPA(authStudentId, targetCgpa, nextSemesterCredits,
                settings);
        Map<String, Double> response = new HashMap<>();
        response.put("requiredSgpa", requiredSgpa);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/analytics/prediction")
    public ResponseEntity<Map<String, Object>> predictDegreeClass(
            @AuthenticationPrincipal Long authStudentId,
            @RequestParam String targetDegreeClass,
            @RequestParam(defaultValue = "1000") int simulations,
            @RequestBody List<Double> nextSemesterSubjects) {
        GPASettings settings = gpaCalculatorService.getOrCreateSettings(authStudentId);
        if (settings.getId() == null) {
            settings = settingsRepository.save(settings);
        }
        double probability = gpaCalculatorService.predictDegreeClassProbability(
                authStudentId, targetDegreeClass, nextSemesterSubjects, settings, simulations);
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

    private record SubjectRequest(
            String name,
            Double credits,
            GradeEnum grade,
            Boolean isGpa,
            SemesterRef semester) {
    }

    private record SemesterRef(String id) {
    }
}
