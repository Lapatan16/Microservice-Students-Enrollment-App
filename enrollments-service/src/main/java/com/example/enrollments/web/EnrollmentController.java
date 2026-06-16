package com.example.enrollments.web;

import com.example.enrollments.dto.EnrollmentDTO;
import com.example.enrollments.model.Enrollment;
import com.example.enrollments.service.EnrollmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentController.class);
    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Enrollment> all() {
        return service.all();
    }

    @GetMapping("/{id}")
    public Enrollment one(@PathVariable(name = "id") Long id) {
        return service.byId(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody EnrollmentDTO dto) {
        log.info("POST /enrollments -> studentId={}, courseCode={}, semester={}",
                dto.studentId(), dto.courseCode(), dto.semester());

        try {
            Enrollment created = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (NoSuchElementException ex) {
            log.warn("Student with ID {} not found when creating enrollment", dto.studentId());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid data when creating enrollment: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable(name = "id") Long id,
                                    @Valid @RequestBody EnrollmentDTO dto) {
        log.info("PUT /enrollments/{} -> updating enrollment", id);

        try {
            Enrollment updated = service.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        log.warn("DELETE /enrollments/{}", id);
        try {
            service.delete(id);
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/{id}/details")
    public EnrollmentService.EnrollmentDetails details(@PathVariable(name = "id") Long id) {
        return service.details(id);
    }
}
