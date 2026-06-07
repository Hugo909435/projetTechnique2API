package app.model;

public enum VisitType {
    PRESENTIEL,
    VISIO;

    public String label() {
        return switch (this) {
            case PRESENTIEL -> "Présentiel";
            case VISIO -> "Visio";
        };
    }
}
