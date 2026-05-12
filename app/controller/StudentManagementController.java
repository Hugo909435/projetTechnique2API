package app.controller;

import app.dto.StudentResponseDto;
import app.dto.UpdateStudentRequest;
import app.dto.UpdateTutorRequest;
import app.dto.UserResponseDto;
import app.service.StudentManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentManagementController {

    private final StudentManagementService studentManagementService;

    public StudentManagementController(StudentManagementService studentManagementService) {
        this.studentManagementService = studentManagementService;
    }

    /** Retourne la liste des étudiants visibles selon le rôle du demandeur. */
    @GetMapping
    public ResponseEntity<List<StudentResponseDto>> getStudents(Authentication auth) {
        return ResponseEntity.ok(studentManagementService.getStudents(auth.getName()));
    }

    /** Retourne le profil complet d'un étudiant. */
    @GetMapping("/{studentId}")
    public ResponseEntity<StudentResponseDto> getStudent(
            @PathVariable Long studentId, Authentication auth) {
        return ResponseEntity.ok(studentManagementService.getStudent(studentId, auth.getName()));
    }

    /** TRAINER (ses étudiants) ou ADMIN : met à jour le profil étudiant. */
    @PutMapping("/{studentId}")
    public ResponseEntity<StudentResponseDto> updateStudent(
            @PathVariable Long studentId,
            @RequestBody UpdateStudentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(studentManagementService.updateStudent(studentId, request, auth.getName()));
    }

    /** TRAINER (son étudiant) ou ADMIN : met à jour le compte du tuteur associé. */
    @PutMapping("/{studentId}/tutor")
    public ResponseEntity<UserResponseDto> updateTutor(
            @PathVariable Long studentId,
            @RequestBody UpdateTutorRequest request,
            Authentication auth) {
        return ResponseEntity.ok(studentManagementService.updateTutor(studentId, request, auth.getName()));
    }

    /** ADMIN seulement : assigne un formateur à un étudiant. */
    @PutMapping("/{studentId}/assign-trainer/{trainerId}")
    public ResponseEntity<StudentResponseDto> assignTrainer(
            @PathVariable Long studentId,
            @PathVariable Long trainerId,
            Authentication auth) {
        return ResponseEntity.ok(studentManagementService.assignTrainer(studentId, trainerId, auth.getName()));
    }

    /** ADMIN seulement : assigne un tuteur à un étudiant. */
    @PutMapping("/{studentId}/assign-tutor/{tutorId}")
    public ResponseEntity<StudentResponseDto> assignTutor(
            @PathVariable Long studentId,
            @PathVariable Long tutorId,
            Authentication auth) {
        return ResponseEntity.ok(studentManagementService.assignTutor(studentId, tutorId, auth.getName()));
    }
}
