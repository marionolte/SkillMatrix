package com.macmario.services.oksm.repository;

import com.macmario.services.oksm.model.ServiceTeam;
import com.macmario.services.oksm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceTeamRepository extends JpaRepository<ServiceTeam, Long> {

    List<ServiceTeam> findByActiveTrue();

    List<ServiceTeam> findByManager(User manager);

    @Query("SELECT t FROM ServiceTeam t LEFT JOIN FETCH t.members LEFT JOIN FETCH t.manager WHERE t.active = true ORDER BY t.name")
    List<ServiceTeam> findAllActiveWithDetails();

    @Query("SELECT t FROM ServiceTeam t JOIN t.members m WHERE m.id = :userId")
    List<ServiceTeam> findTeamsByMemberId(@Param("userId") Long userId);
}
