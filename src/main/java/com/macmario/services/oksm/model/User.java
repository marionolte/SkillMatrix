package com.macmario.services.oksm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** true = external consultant/freelancer */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean consultant = false;

    private String department;
    private String team;
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "engineer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<EngineerSkill> engineerSkills = new HashSet<>();

    public enum Role {
        ENGINEER, MANAGER, ADMIN
    }

    public User(String username, String password, String email, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }
}
