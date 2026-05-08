package com.macmario.services.oksm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "service_teams")
@Data
@NoArgsConstructor
public class ServiceTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String name;

    private String description;
    private String color = "#4F46E5";
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    /** Members of this team (engineers) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "service_team_members",
        joinColumns = @JoinColumn(name = "team_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();

    public ServiceTeam(String name, String description, String color) {
        this.name = name;
        this.description = description;
        this.color = color;
    }
}
