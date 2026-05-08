package com.macmario.services.oksm.repository;

import com.macmario.services.oksm.model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByCategoryId(Long categoryId);
    List<Skill> findByActiveTrue();

    @Query("SELECT s FROM Skill s LEFT JOIN FETCH s.category WHERE s.active = true ORDER BY s.category.name, s.name")
    List<Skill> findAllActiveWithCategory();
}
