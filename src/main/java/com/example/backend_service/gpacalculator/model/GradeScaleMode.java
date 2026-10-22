package com.example.backend_service.gpacalculator.model;

/**
 * Enum representing the GPA scale mode used for grade-to-point conversion.
 * Different institutions use different grading scales.
 */
public enum GradeScaleMode {
    STANDARD_4_0("standard"), // Standard 4.0 GPA scale (most common)
    EXTENDED_4_2("extended"); // Extended 4.2 GPA scale (some universities)

    private final String displayValue;

    GradeScaleMode(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public static GradeScaleMode fromDisplayValue(String mode) {
        for (GradeScaleMode m : GradeScaleMode.values()) {
            if (m.displayValue.equalsIgnoreCase(mode)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Invalid grade scale mode: " + mode);
    }
}
