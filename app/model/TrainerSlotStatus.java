package app.model;

public enum TrainerSlotStatus {
    FREE,      // créé par le formateur, pas encore proposé
    PROPOSED,  // proposé à un tuteur, en attente de confirmation
    BOOKED,    // confirmé par le tuteur
    CANCELLED  // annulé automatiquement quand le tuteur a choisi un autre créneau
}
