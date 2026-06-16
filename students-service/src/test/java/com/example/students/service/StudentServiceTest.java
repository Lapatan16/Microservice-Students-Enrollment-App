package com.example.students.service;

import com.example.students.dto.StudentDTO;
import com.example.students.model.Student;
import com.example.students.repo.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository repo;

    @InjectMocks
    private StudentService service;

    @Test
    void shouldCreateStudentWhenUnique() {
        Student s = new Student();
        s.setFullName("Ana Petrovic");
        s.setEmail("ana@example.com");
        s.setIndexNumber("2025-001");

        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(repo.findByIndexNumber("2025-001")).thenReturn(Optional.empty());
        when(repo.save(s)).thenReturn(s);

        Student saved = service.create(s);

        assertThat(saved.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void shouldThrowWhenEmailExists() {
        Student s = new Student();
        s.setEmail("ana@example.com");
        s.setIndexNumber("2025-001");

        when(repo.findByEmail("ana@example.com")).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.create(s))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void shouldUpdateStudent() {
        Student existing = new Student();
        existing.setId(1L);
        existing.setFullName("Old Name");
        existing.setEmail("old@example.com");
        existing.setIndexNumber("2024-001");

        StudentDTO dto = new StudentDTO(1L, "New Name", "new@example.com", "2025-123");

        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(existing)).thenReturn(existing);

        Student updated = service.update(1L, dto);

        assertThat(updated.getFullName()).isEqualTo("New Name");
        assertThat(updated.getEmail()).isEqualTo("new@example.com");
        assertThat(updated.getIndexNumber()).isEqualTo("2025-123");
    }
}
