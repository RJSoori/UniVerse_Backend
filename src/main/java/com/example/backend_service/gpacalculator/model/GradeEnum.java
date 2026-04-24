package com.example.backend_service.gpacalculator.model;

/**
 * Enum representing valid letter grades in the GPA system.
 * These grades are used for subject grades and are mapped to point values.
 */
public enum GradeEnum {
    PLUS_A("A+"),
    A("A"),
    MINUS_A("A-"),
    PLUS_B("B+"),
    B("B"),
    MINUS_B("B-"),
    PLUS_C("C+"),
    C("C"),
    MINUS_C("C-"),
    PLUS_D("D+"),
    D("D"),
    E("E"),
    F("F");

    private final String displayValue;

    GradeEnum(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    /**
     * Convert a string grade (e.g., "A+") to the corresponding enum value.
     * 
     * @param grade the grade string
     * @return the GradeEnum value, or throws IllegalArgumentException if not found
     */
    public static GradeEnum fromDisplayValue(String grade) {
        for (GradeEnum g : GradeEnum.values()) {
            if (g.displayValue.equalsIgnoreCase(grade)) {
                return g;
            }
        }
        throw new IllegalArgumentException("Invalid grade: " + grade);
    }
}
