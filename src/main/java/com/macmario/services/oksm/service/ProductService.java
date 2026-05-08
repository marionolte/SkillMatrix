package com.macmario.services.oksm.service;

import com.macmario.services.oksm.model.Product;
import com.macmario.services.oksm.model.Skill;
import com.macmario.services.oksm.repository.ProductRepository;
import com.macmario.services.oksm.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private SkillRepository skillRepository;

    public List<Product> getAllActiveProducts() {
        return productRepository.findAllActiveWithSkills();
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public Product saveWithSkills(Product product, List<Long> skillIds) {
        Set<Skill> skills = skillIds.stream()
            .map(id -> skillRepository.findById(id).orElseThrow())
            .collect(Collectors.toSet());
        product.setRequiredSkills(skills);
        return productRepository.save(product);
    }

    public void toggleActive(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setActive(!product.isActive());
        productRepository.save(product);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }
}
