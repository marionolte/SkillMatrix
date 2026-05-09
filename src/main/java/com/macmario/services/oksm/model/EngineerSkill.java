package com.macmario.services.oksm.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "engineer_skills",
    uniqueConstraints = @UniqueConstraint(columnNames = {"engineer_id", "skill_id"}))
@Data
@NoArgsConstructor
public class EngineerSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineer_id", nullable = false)
    private User engineer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel level;

    private String notes;

    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public EngineerSkill(User engineer, Skill skill, SkillLevel level) {
        this.engineer = engineer;
        this.skill = skill;
        this.level = level;
    }

    public enum SkillLevel {
        SPEZIALIST("Spezialist",       "#7C3AED", "⭐⭐⭐⭐⭐⭐", 6),
        EXPERTE("Experte",             "#059669", "⭐⭐⭐⭐⭐",  5),
        FORTGESCHRITTEN("Fortgeschritten", "#2563EB", "⭐⭐⭐⭐",  4),
        ANWENDER("Anwender",           "#D97706", "⭐⭐⭐",     3),
        GRUNDWISSEN("Grundwissen",     "#EA580C", "⭐⭐",      2),
        NOP("NOP",                     "#6B7280", "⭐",        1);

        private final String label;
        private final String color;
        private final String stars;
        private final int order;

        SkillLevel(String label, String color, String stars, int order) {
            this.label = label;
            this.color = color;
            this.stars = stars;
            this.order = order;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
        public String getStars() { return stars; }
        public int getOrder() { return order; }
    }
}
