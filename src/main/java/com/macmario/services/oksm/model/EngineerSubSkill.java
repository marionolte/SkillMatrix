package com.macmario.services.oksm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "engineer_sub_skills",
    uniqueConstraints = @UniqueConstraint(columnNames = {"engineer_id", "sub_skill_id"}))
@Data
@NoArgsConstructor
public class EngineerSubSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineer_id", nullable = false)
    private User engineer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sub_skill_id", nullable = false)
    private SubSkill subSkill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EngineerSkill.SkillLevel level;

    private String notes;
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() { updatedAt = LocalDateTime.now(); }

    public EngineerSubSkill(User engineer, SubSkill subSkill, EngineerSkill.SkillLevel level) {
        this.engineer = engineer;
        this.subSkill = subSkill;
        this.level = level;
    }
}
