package app.repository;

import app.model.VisitSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitSlotRepository extends JpaRepository<VisitSlot, Long> {

    @Query("SELECT COUNT(s) > 0 FROM VisitSlot s WHERE s.campaign.id = :campaignId AND s.bookedBy.id = :tutorId")
    boolean tutorAlreadyBooked(@Param("campaignId") Long campaignId, @Param("tutorId") Long tutorId);
}
