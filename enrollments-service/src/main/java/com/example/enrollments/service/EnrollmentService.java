package com.example.enrollments.service;

import com.example.enrollments.dto.EnrollmentDTO;
import com.example.enrollments.dto.StudentDTO;
import com.example.enrollments.feign.StudentClient;
import com.example.enrollments.model.Enrollment;
import com.example.enrollments.repo.EnrollmentRepository;
import feign.FeignException;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository repo;
    private final StudentClient studentClient;

    public EnrollmentService(EnrollmentRepository repo, StudentClient studentClient) {
        this.repo = repo;
        this.studentClient = studentClient;
    }

    public List<Enrollment> all() {
            return repo.findAll();
    }

    public Enrollment byId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Enrollment with ID " + id + " not found"));
    }

    @CircuitBreaker(name = "studentsCB", fallbackMethod = "createFallback")
    @Retry(name = "studentsRetry")
    public Enrollment create(EnrollmentDTO dto) {
        log.info("Creating enrollment for studentId={}, courseCode={}, semester={}",
                dto.studentId(), dto.courseCode(), dto.semester());

        try {
            StudentDTO s = studentClient.getStudent(dto.studentId());

            if (s == null) {
                log.warn("Students service returned null for studentId={}", dto.studentId());
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Students service invalid response");
            }

            Enrollment enrollment = Enrollment.builder()
                    .studentId(dto.studentId())
                    .courseCode(dto.courseCode())
                    .semester(dto.semester())
                    .build();

            return repo.save(enrollment);

        } catch (FeignException.NotFound nf) {
            log.warn("Student with ID {} not found (404)", dto.studentId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student with ID " + dto.studentId() + " not found");

        } catch (FeignException ex) {
            log.error("Feign exception fetching student {}: {}", dto.studentId(), ex.getMessage());
            throw ex;
        }
    }

    private Enrollment createFallback(EnrollmentDTO dto, Throwable ex) {
        log.error("Create enrollment fallback triggered for studentId={} due to: {}",
                dto.studentId(), ex.toString());
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Students service unavailable (Circuit Breaker) – cannot create enrollment now"
        );
    }


    public Enrollment update(Long id, EnrollmentDTO dto) {
        Enrollment existing = byId(id);

        if (!existing.getStudentId().equals(dto.studentId())) {
            try {
                StudentDTO student = fetchStudentWithResilience(dto.studentId());
                if (student == null) {
                    throw new NoSuchElementException("Student with ID " + dto.studentId() + " not found");
                }
            } catch (NoSuchElementException nse) {
                throw nse;
            }
        }

        existing.setStudentId(dto.studentId());
        existing.setCourseCode(dto.courseCode());
        existing.setSemester(dto.semester());

        return repo.save(existing);
    }

    public void delete(Long id) {
        Enrollment enrollment = byId(id);
        repo.delete(enrollment);
    }

    public EnrollmentDetails details(Long id) {
        Enrollment enrollment = byId(id);
        try {
            StudentDTO student = fetchStudentWithResilience(enrollment.getStudentId());
            return new EnrollmentDetails(enrollment, student);
        } catch (NoSuchElementException nse) {
            throw nse;
        }
    }

    @CircuitBreaker(name = "studentsCB", fallbackMethod = "fetchStudentFallback")
    @Retry(name = "studentsRetry")
    public StudentDTO fetchStudentWithResilience(Long studentId) {
        try {
            return studentClient.getStudent(studentId);
        } catch (FeignException.NotFound nf) {
            throw new NoSuchElementException("Student with ID " + studentId + " not found");
        } catch (RetryableException rex) {
            log.error("StudentService retryable exception for id={}: {}", studentId, rex.getMessage());
            throw new IllegalStateException("StudentService unreachable");
        } catch (FeignException.ServiceUnavailable su) {
            log.error("StudentService returned 503 for id={}: {}", studentId, su.getMessage());
            throw new IllegalStateException("StudentService unavailable (503)");
        } catch (FeignException fe) {
            log.error("Unexpected FeignException while fetching student {}: {}", studentId, fe.getMessage());
            throw new IllegalStateException("Failed to contact StudentService");
        }
    }

    private StudentDTO fetchStudentFallback(Long studentId, Throwable ex) {
        log.error("fetchStudentFallback: Student service unavailable for id={} cause={}", studentId, ex.toString());
        throw new IllegalStateException("Students service unavailable (Circuit Breaker)");
    }

    /** Combined response for details() */
    public record EnrollmentDetails(Enrollment enrollment, StudentDTO student) {}
}
