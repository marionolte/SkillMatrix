package com.macmario.services.oksm.repository;

import com.macmario.services.oksm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByRole(User.Role role);
    List<User> findByRoleAndActive(User.Role role, boolean active);

    @Query("SELECT u FROM User u WHERE u.role = 'ENGINEER' AND u.active = true ORDER BY u.fullName")
    List<User> findActiveEngineers();
}
