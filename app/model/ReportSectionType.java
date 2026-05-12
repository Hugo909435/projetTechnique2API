package app.model;

/** Ordre de définition = ordre d'affichage dans le rapport. */
public enum ReportSectionType {
    ACTIVITIES("Activités réalisées"),
    SKILLS("Compétences développées"),
    DIFFICULTIES("Difficultés rencontrées"),
    SOLUTIONS("Solutions apportées"),
    OBJECTIVES("Objectifs pour le mois suivant"),
    FREE_COMMENT("Commentaire libre de l'étudiant");

    private final String label;

    ReportSectionType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
