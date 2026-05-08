package com.macmario.services.oksm.repository;

import com.macmario.services.oksm.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.requiredSkills WHERE p.active = true ORDER BY p.name")
    List<Product> findAllActiveWithSkills();
}
