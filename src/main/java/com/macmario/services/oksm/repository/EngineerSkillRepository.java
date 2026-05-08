package com.macmario.services.oksm.repository;

import com.macmario.services.oksm.model.EngineerSkill;
import com.macmario.services.oksm.model.User;
import com.macmario.services.oksm.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EngineerSkillRepository extends JpaRepository<EngineerSkill, Long> {
    List<EngineerSkill> findByEngineerId(Long engineerId);
    Optional<EngineerSkill> findByEngineerAndSkill(User engineer, Skill skill);
    Optional<EngineerSkill> findByEngineerIdAndSkillId(Long engineerId, Long skillId);

    @Query("SELECT es FROM EngineerSkill es JOIN FETCH es.skill s JOIN FETCH s.category WHERE es.engineer.id = :engineerId ORDER BY s.category.name, s.name")
    List<EngineerSkill> findByEngineerIdWithDetails(@Param("engineerId") Long engineerId);

    @Query("SELECT es FROM EngineerSkill es JOIN FETCH es.engineer WHERE es.skill.id = :skillId")
    List<EngineerSkill> findBySkillId(@Param("skillId") Long skillId);

    void deleteByEngineerAndSkill(User engineer, Skill skill);
}
