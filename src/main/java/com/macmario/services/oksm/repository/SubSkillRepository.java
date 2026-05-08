package com.macmario.services.oksm.repository;

import com.macmario.services.oksm.model.SubSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubSkillRepository extends JpaRepository<SubSkill, Long> {

    List<SubSkill> findBySkillIdAndActiveTrue(Long skillId);

    List<SubSkill> findBySkillIdOrderByDisplayOrderAsc(Long skillId);

    Optional<SubSkill> findBySkillIdAndName(Long skillId, String name);

    @Query("SELECT ss FROM SubSkill ss JOIN FETCH ss.skill WHERE ss.active = true ORDER BY ss.skill.name, ss.displayOrder, ss.name")
    List<SubSkill> findAllActiveWithSkill();

    @Query("SELECT ss FROM SubSkill ss WHERE ss.skill.id IN :skillIds AND ss.active = true ORDER BY ss.displayOrder, ss.name")
    List<SubSkill> findBySkillIds(@Param("skillIds") List<Long> skillIds);
}
