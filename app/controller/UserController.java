package app.controller;

import app.dto.CreateUserRequest;
import app.dto.UpdateStudentRequest;
import app.dto.UserResponseDto;
import app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** ADMIN : tous les utilisateurs. TRAINER : ses étudiants + leurs tuteurs. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<List<UserResponseDto>> getUsers(Authentication auth) {
        return ResponseEntity.ok(userService.getUsers(auth.getName()));
    }

    /** Tout utilisateur authentifié peut lire un profil (contrôle d'accès dans le service). */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(userService.getUser(id, auth.getName()));
    }

    /** ADMIN : tout champ. TRAINER : ses étudiants/tuteurs. Utilisateur : son propre profil. */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateStudentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(userService.updateUser(id, request, auth.getName()));
    }

    /** ADMIN seulement. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDto> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(request, auth.getName()));
    }

    /** ADMIN seulement. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        userService.deleteUser(id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
