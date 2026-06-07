package app.repository;

import app.model.VisitCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitCampaignRepository extends JpaRepository<VisitCampaign, Long> {

    @Query("SELECT DISTINCT vc FROM VisitCampaign vc LEFT JOIN FETCH vc.slots WHERE vc.trainer.id = :trainerId ORDER BY vc.createdAt DESC")
    List<VisitCampaign> findByTrainerIdWithSlots(@Param("trainerId") Long trainerId);

    @Query("SELECT DISTINCT vc FROM VisitCampaign vc LEFT JOIN FETCH vc.slots WHERE vc.trainer.id IN :trainerIds ORDER BY vc.createdAt DESC")
    List<VisitCampaign> findByTrainerIdsWithSlots(@Param("trainerIds") List<Long> trainerIds);

    @Query("SELECT DISTINCT vc FROM VisitCampaign vc LEFT JOIN FETCH vc.slots WHERE vc.id = :id")
    Optional<VisitCampaign> findByIdWithSlots(@Param("id") Long id);
}
