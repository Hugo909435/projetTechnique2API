package app.model;

public enum ReportStatus {
    DRAFT,
    STUDENT_VALIDATED,
    AUTO_VALIDATED,
    TRAINER_VALIDATED,
    TUTOR_VALIDATED,
    COMPLETED,
    REOPENED;

    public boolean isEditable() {
        return this == DRAFT || this == REOPENED;
    }

    public boolean canBeReopened() {
        return this == STUDENT_VALIDATED
                || this == AUTO_VALIDATED
                || this == TRAINER_VALIDATED;
    }
}
