package com.macmario.services.oksm.repository;

import com.macmario.services.oksm.model.EngineerSubSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EngineerSubSkillRepository extends JpaRepository<EngineerSubSkill, Long> {

    List<EngineerSubSkill> findByEngineerId(Long engineerId);

    Optional<EngineerSubSkill> findByEngineerIdAndSubSkillId(Long engineerId, Long subSkillId);

    @Query("""
        SELECT ess FROM EngineerSubSkill ess
        JOIN FETCH ess.subSkill ss
        JOIN FETCH ss.skill s
        WHERE ess.engineer.id = :engineerId
        ORDER BY s.name, ss.displayOrder, ss.name
        """)
    List<EngineerSubSkill> findByEngineerIdWithDetails(@Param("engineerId") Long engineerId);

    @Query("""
        SELECT ess FROM EngineerSubSkill ess
        WHERE ess.engineer.id = :engineerId
        AND ess.subSkill.skill.id = :skillId
        """)
    List<EngineerSubSkill> findByEngineerIdAndSkillId(
        @Param("engineerId") Long engineerId,
        @Param("skillId") Long skillId);

    void deleteByEngineerIdAndSubSkillId(Long engineerId, Long subSkillId);
}
