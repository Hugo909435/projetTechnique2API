package app.service;

import app.exception.ForbiddenException;
import app.model.MonthlyReport;
import app.model.Role;
import app.model.User;
import app.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class ReportPermissionService {

    private final StudentProfileRepository studentProfileRepository;

    public ReportPermissionService(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    /** Vérifie que le demandeur peut lire ce rapport. */
    public void assertCanRead(MonthlyReport report, User requester) {
        switch (requester.getRole()) {
            case ADMIN -> {}
            case STUDENT -> {
                if (!report.getStudent().getId().equals(requester.getId()))
                    throw new ForbiddenException("Vous ne pouvez consulter que vos propres rapports");
            }
            case TRAINER -> {
                if (!studentProfileRepository.existsByStudentIdAndTrainerId(
                        report.getStudent().getId(), requester.getId()))
                    throw new ForbiddenException("Ce rapport n'appartient pas à l'un de vos étudiants");
            }
            case TUTOR -> {
                if (!studentProfileRepository.existsByStudentIdAndTutorId(
                        report.getStudent().getId(), requester.getId()))
                    throw new ForbiddenException("Ce rapport n'appartient pas à l'un de vos étudiants");
            }
        }
    }

    /** Vérifie que le demandeur peut modifier ce rapport. */
    public void assertCanEdit(MonthlyReport report, User requester) {
        assertCanRead(report, requester);

        if (requester.getRole() == Role.ADMIN) return;

        if (requester.getRole() != Role.STUDENT)
            throw new ForbiddenException("Seul l'étudiant peut modifier son rapport");

        if (!report.getStatus().isEditable() && !report.getStatus().canBeResetByStudentEdit())
            throw new ForbiddenException(
                "Ce rapport n'est plus modifiable (statut : " + report.getStatus() + ")");
    }

    /** Indique si l'utilisateur donné est autorisé à lire les rapports d'un étudiant. */
    public void assertCanReadStudentReports(Long studentId, User requester) {
        switch (requester.getRole()) {
            case ADMIN -> {}
            case STUDENT -> {
                if (!requester.getId().equals(studentId))
                    throw new ForbiddenException("Vous ne pouvez consulter que vos propres rapports");
            }
            case TRAINER -> {
                if (!studentProfileRepository.existsByStudentIdAndTrainerId(studentId, requester.getId()))
                    throw new ForbiddenException("Cet étudiant ne fait pas partie de vos étudiants");
            }
            case TUTOR -> {
                if (!studentProfileRepository.existsByStudentIdAndTutorId(studentId, requester.getId()))
                    throw new ForbiddenException("Cet étudiant ne fait pas partie de vos étudiants");
            }
        }
    }
}
