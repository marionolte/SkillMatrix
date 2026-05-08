package com.macmario.services.oksm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill_categories")
@Data
@NoArgsConstructor
public class SkillCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String name;

    private String description;
    private String color = "#4F46E5";
    private String icon = "🔧";

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Skill> skills = new ArrayList<>();

    public SkillCategory(String name, String description, String color, String icon) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.icon = icon;
    }
}
