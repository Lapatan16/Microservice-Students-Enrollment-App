package com.example.enrollments.dto;

import jakarta.validation.constraints.*;

public record EnrollmentDTO(

        Long id,

        @NotNull(message = "Student ID cannot be null")
        @Positive(message = "Student ID must be a positive number")
        Long studentId,

        @NotBlank(message = "Course code cannot be blank")
        @Pattern(
                regexp = "^[A-Z]{2,4}\\d{2,4}$",
                message = "Course code must be in format like 'DS101' or 'CS50' (2–4 uppercase letters followed by 2–4 digits)"
        )
        String courseCode,

        @NotBlank(message = "Semester cannot be blank")
        @Pattern(
                regexp = "^\\d{1}/\\d{4}$",
                message = "Semester must follow format like '1/2022'"
        )
        String semester
) {}

