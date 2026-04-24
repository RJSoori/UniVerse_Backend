package com.example.backend_service.gpacalculator.service;

import com.example.backend_service.gpacalculator.model.GradeEnum;
import com.example.backend_service.gpacalculator.model.GradeScaleMode;
import com.example.backend_service.gpacalculator.model.GPASemester;
import com.example.backend_service.gpacalculator.model.GPASettings;
import com.example.backend_service.gpacalculator.model.GPASubject;
import com.example.backend_service.gpacalculator.repository.GPASemesterRepository;
import com.example.backend_service.gpacalculator.repository.GPASettingsRepository;
import com.example.backend_service.gpacalculator.repository.GPASubjectRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Random;

/**
 * Service providing GPA calculation logic.
 * Implements SGPA, CGPA, degree classification, and prediction algorithms.
 * All calculations must maintain parity with frontend implementations.
 */
@Service
public class GPACalculatorService {

    private final GPASemesterRepository semesterRepository;
    private final GPASubjectRepository subjectRepository;
    private final GPASettingsRepository settingsRepository;
    private final Random random = new Random();

    public GPACalculatorService(
            GPASemesterRepository semesterRepository,
            GPASubjectRepository subjectRepository,
            GPASettingsRepository settingsRepository) {
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.settingsRepository = settingsRepository;
    }

    /**
     * Get or create default GPA settings for a student.
     */
    public GPASettings getOrCreateSettings(String studentId) {
        return settingsRepository.findByStudentId(studentId)
                .orElse(new GPASettings(studentId));
    }

    /**
     * Convert a grade to its numeric point value based on the GPA scale.
     * 
     * @param grade    the grade enum
     * @param gpaScale the GPA scale mode (4.0 or 4.2)
     * @return the numeric point value
     */
    public double getGradePoint(GradeEnum grade, GradeScaleMode gpaScale) {
        if (gpaScale == GradeScaleMode.EXTENDED_4_2) {
            return switch (grade) {
                case PLUS_A -> 4.2;
                case A -> 4.0;
                case MINUS_A -> 3.7;
                case PLUS_B -> 3.3;
                case B -> 3.0;
                case MINUS_B -> 2.7;
                case PLUS_C -> 2.3;
                case C -> 2.0;
                case MINUS_C -> 1.7;
                case PLUS_D -> 1.0;
                case D -> 1.0;
                case E, F -> 0.0;
            };
        } else {
            // Standard 4.0 scale
            return switch (grade) {
                case PLUS_A, A -> 4.0;
                case MINUS_A -> 3.7;
                case PLUS_B -> 3.3;
                case B -> 3.0;
                case MINUS_B -> 2.7;
                case PLUS_C -> 2.3;
                case C -> 2.0;
                case MINUS_C -> 1.7;
                case PLUS_D, D -> 1.0;
                case E, F -> 0.0;
            };
        }
    }

    /**
     * Calculate SGPA (Semester GPA) for a given semester.
     * SGPA = sum(grade_point * credits) / sum(credits) for subjects where
     * isGpa=true
     * 
     * @param semester the semester entity
     * @param settings the GPA settings (for scale mode)
     * @return the SGPA value
     */
    public double calculateSGPA(GPASemester semester, GPASettings settings) {
        List<GPASubject> subjects = semester.getSubjects();
        if (subjects.isEmpty()) {
            return 0.0;
        }

        double totalWeightedPoints = 0.0;
        double totalCredits = 0.0;

        for (GPASubject subject : subjects) {
            if (subject.getIsGpa()) {
                double gradePoint = getGradePoint(subject.getGrade(), settings.getGpaScale());
                double credits = subject.getCredits();
                totalWeightedPoints += gradePoint * credits;
                totalCredits += credits;
            }
        }

        if (totalCredits == 0.0) {
            return 0.0;
        }

        return Math.round((totalWeightedPoints / totalCredits) * 1000.0) / 1000.0;
    }

    /**
     * Calculate CGPA (Cumulative GPA) for a student across all semesters.
     * CGPA = sum(all SGPA * semester credits) / sum(all semester credits)
     * 
     * @param studentId the student ID
     * @param settings  the GPA settings (for scale mode)
     * @return the CGPA value
     */
    public double calculateCGPA(String studentId, GPASettings settings) {
        List<GPASemester> semesters = semesterRepository.findByStudentId(studentId);
        if (semesters.isEmpty()) {
            return 0.0;
        }

        double totalWeightedPoints = 0.0;
        double totalCredits = 0.0;

        for (GPASemester semester : semesters) {
            double sgpa = calculateSGPA(semester, settings);
            double semesterCredits = semester.getSubjects().stream()
                    .filter(s -> s.getIsGpa())
                    .mapToDouble(GPASubject::getCredits)
                    .sum();
            totalWeightedPoints += sgpa * semesterCredits;
            totalCredits += semesterCredits;
        }

        if (totalCredits == 0.0) {
            return 0.0;
        }

        return Math.round((totalWeightedPoints / totalCredits) * 1000.0) / 1000.0;
    }

    /**
     * Classify a GPA into a degree class based on thresholds.
     * 
     * @param gpa      the GPA value
     * @param settings the GPA settings containing classification thresholds
     * @return a string describing the degree class
     */
    public String classifyDegreeClass(double gpa, GPASettings settings) {
        if (gpa >= settings.getFirstClassThreshold()) {
            return "First Class";
        } else if (gpa >= settings.getSecondUpperThreshold()) {
            return "Second Upper";
        } else if (gpa >= settings.getSecondLowerThreshold()) {
            return "Second Lower";
        } else if (gpa >= settings.getGeneralThreshold()) {
            return "General Degree";
        } else {
            return "Academic Warning";
        }
    }

    /**
     * Calculate the required SGPA to achieve a target CGPA.
     * Assumes the target SGPA is needed for the next semester.
     * requiredSgpa = (targetCgpa * totalCurrentCredits + targetSgpa *
     * nextSemesterCredits) / (totalCurrentCredits + nextSemesterCredits)
     * Solving for nextSemesterSgpa (target SGPA):
     * targetSgpa = (targetCgpa * (totalCurrentCredits + nextSemesterCredits) -
     * currentCgpa * totalCurrentCredits) / nextSemesterCredits
     * 
     * @param studentId           the student ID
     * @param targetCgpa          the desired CGPA
     * @param nextSemesterCredits estimated credits in the next semester
     * @param settings            the GPA settings
     * @return the required SGPA for next semester
     */
    public double calculateRequiredSGPA(String studentId, double targetCgpa, double nextSemesterCredits,
            GPASettings settings) {
        List<GPASemester> semesters = semesterRepository.findByStudentId(studentId);

        double totalCurrentCredits = 0.0;
        double currentWeightedPoints = 0.0;

        for (GPASemester semester : semesters) {
            double sgpa = calculateSGPA(semester, settings);
            double semesterCredits = semester.getSubjects().stream()
                    .filter(s -> s.getIsGpa())
                    .mapToDouble(GPASubject::getCredits)
                    .sum();
            currentWeightedPoints += sgpa * semesterCredits;
            totalCurrentCredits += semesterCredits;
        }

        if (totalCurrentCredits == 0.0) {
            return targetCgpa;
        }

        double currentCgpa = currentWeightedPoints / totalCurrentCredits;
        double totalFutureCredits = totalCurrentCredits + nextSemesterCredits;
        double requiredSgpa = (targetCgpa * totalFutureCredits - currentCgpa * totalCurrentCredits)
                / nextSemesterCredits;

        // Clamp to valid range (0 to max scale)
        double maxScale = settings.getGpaScale() == GradeScaleMode.EXTENDED_4_2 ? 4.2 : 4.0;
        return Math.max(0.0, Math.min(maxScale, requiredSgpa));
    }

    /**
     * Predict the probability of achieving a target degree class using Monte Carlo
     * simulation.
     * Simulates random grade distributions and counts how many achieve the target.
     * 
     * @param studentId            the student ID
     * @param targetDegreeClass    the target degree classification threshold
     * @param nextSemesterSubjects list of subject credits to simulate
     * @param settings             the GPA settings
     * @param simulations          number of Monte Carlo iterations (default 1000)
     * @return probability as a percentage (0-100)
     */
    public double predictDegreeClassProbability(
            String studentId,
            String targetDegreeClass,
            List<Double> nextSemesterSubjects,
            GPASettings settings,
            int simulations) {

        List<GPASemester> semesters = semesterRepository.findByStudentId(studentId);

        double totalCurrentCredits = 0.0;
        double currentWeightedPoints = 0.0;

        for (GPASemester semester : semesters) {
            double sgpa = calculateSGPA(semester, settings);
            double semesterCredits = semester.getSubjects().stream()
                    .filter(s -> s.getIsGpa())
                    .mapToDouble(GPASubject::getCredits)
                    .sum();
            currentWeightedPoints += sgpa * semesterCredits;
            totalCurrentCredits += semesterCredits;
        }

        // Get the threshold for the target degree class
        double threshold = getThresholdForDegreeClass(targetDegreeClass, settings);

        int successCount = 0;
        double totalNextCredits = nextSemesterSubjects.stream().mapToDouble(Double::doubleValue).sum();

        for (int i = 0; i < simulations; i++) {
            // Simulate random grades for next semester
            double simulatedWeightedPoints = 0.0;
            for (double credits : nextSemesterSubjects) {
                GradeEnum randomGrade = getRandomGrade();
                double gradePoint = getGradePoint(randomGrade, settings.getGpaScale());
                simulatedWeightedPoints += gradePoint * credits;
            }

            double projectedCgpa = (currentWeightedPoints + simulatedWeightedPoints)
                    / (totalCurrentCredits + totalNextCredits);
            if (projectedCgpa >= threshold) {
                successCount++;
            }
        }

        return Math.round((successCount / (double) simulations) * 100.0);
    }

    /**
     * Get a random grade for Monte Carlo simulation.
     * Weighted towards higher grades (more likely to get A than F).
     */
    private GradeEnum getRandomGrade() {
        GradeEnum[] allGrades = GradeEnum.values();
        // Weighted random: favor higher grades
        double rand = random.nextDouble() * 100;
        if (rand < 30)
            return GradeEnum.PLUS_A;
        if (rand < 50)
            return GradeEnum.A;
        if (rand < 65)
            return GradeEnum.MINUS_A;
        if (rand < 75)
            return GradeEnum.PLUS_B;
        if (rand < 82)
            return GradeEnum.B;
        if (rand < 87)
            return GradeEnum.MINUS_B;
        if (rand < 91)
            return GradeEnum.PLUS_C;
        if (rand < 94)
            return GradeEnum.C;
        if (rand < 96)
            return GradeEnum.MINUS_C;
        return allGrades[random.nextInt(allGrades.length - 3)];
    }

    /**
     * Get the GPA threshold for a given degree class.
     */
    private double getThresholdForDegreeClass(String degreeClass, GPASettings settings) {
        return switch (degreeClass) {
            case "First Class" -> settings.getFirstClassThreshold();
            case "Second Upper" -> settings.getSecondUpperThreshold();
            case "Second Lower" -> settings.getSecondLowerThreshold();
            case "General Degree" -> settings.getGeneralThreshold();
            default -> 0.0;
        };
    }
}
