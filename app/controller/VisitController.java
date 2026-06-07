package app.controller;

import app.dto.CreateTrainerSlotRequest;
import app.dto.ProposeSlotRequest;
import app.dto.TrainerSlotResponseDto;
import app.dto.TutorVisitStatusDto;
import app.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    /** Formateur : créer un créneau libre */
    @PostMapping("/api/trainer-slots")
    public ResponseEntity<TrainerSlotResponseDto> createSlot(
            @Valid @RequestBody CreateTrainerSlotRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(visitService.createSlot(request, auth.getName()));
    }

    /** Formateur : tous ses créneaux — Tuteur : ses créneaux proposés/confirmés */
    @GetMapping("/api/trainer-slots")
    public ResponseEntity<List<TrainerSlotResponseDto>> getSlots(Authentication auth) {
        return ResponseEntity.ok(visitService.getSlots(auth.getName()));
    }

    /** Formateur : liste des tuteurs avec statut de visite */
    @GetMapping("/api/trainer-slots/tutors")
    public ResponseEntity<List<TutorVisitStatusDto>> getTutors(Authentication auth) {
        return ResponseEntity.ok(visitService.getTutors(auth.getName()));
    }

    /** Formateur : proposer un créneau à un tuteur */
    @PutMapping("/api/trainer-slots/{id}/propose")
    public ResponseEntity<TrainerSlotResponseDto> proposeSlot(
            @PathVariable Long id,
            @Valid @RequestBody ProposeSlotRequest request,
            Authentication auth) {
        return ResponseEntity.ok(visitService.proposeSlot(id, request, auth.getName()));
    }

    /** Formateur : annuler une proposition (créneau → FREE) */
    @DeleteMapping("/api/trainer-slots/{id}/propose")
    public ResponseEntity<TrainerSlotResponseDto> cancelProposal(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(visitService.cancelProposal(id, auth.getName()));
    }

    /** Formateur : supprimer un créneau libre */
    @DeleteMapping("/api/trainer-slots/{id}")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable Long id, Authentication auth) {
        visitService.deleteSlot(id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    /** Tuteur : confirmer un créneau */
    @PutMapping("/api/trainer-slots/{id}/confirm")
    public ResponseEntity<TrainerSlotResponseDto> confirmSlot(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(visitService.confirmSlot(id, auth.getName()));
    }
}
