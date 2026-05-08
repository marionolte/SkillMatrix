package com.macmario.services.oksm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sub_skills",
    uniqueConstraints = @UniqueConstraint(columnNames = {"skill_id", "name"}))
@Data
@NoArgsConstructor
public class SubSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @NotBlank
    @Column(nullable = false)
    private String name;          // e.g. "Install", "Administrate", "Migrate", "Upgrade"

    private String description;
    private int displayOrder = 0;
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    public SubSkill(Skill skill, String name, String description, int displayOrder) {
        this.skill = skill;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
