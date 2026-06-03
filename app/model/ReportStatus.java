package app.model;

public enum ReportStatus {
    DRAFT,
    STUDENT_VALIDATED,
    AUTO_VALIDATED,
    TRAINER_VALIDATED,
    TUTOR_VALIDATED,
    COMPLETED;

    public boolean isEditable() {
        return this == DRAFT;
    }

    /** L'étudiant peut modifier le rapport et cela réinitialise toutes les validations. */
    public boolean canBeResetByStudentEdit() {
        return this == STUDENT_VALIDATED
                || this == AUTO_VALIDATED
                || this == TRAINER_VALIDATED
                || this == TUTOR_VALIDATED
                || this == COMPLETED;
    }
}
