package com.macmario.services.oksm.service;

import com.macmario.services.oksm.model.ServiceTeam;
import com.macmario.services.oksm.model.User;
import com.macmario.services.oksm.repository.ServiceTeamRepository;
import com.macmario.services.oksm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ServiceTeamService {

    @Autowired private ServiceTeamRepository teamRepository;
    @Autowired private UserRepository userRepository;

    public List<ServiceTeam> getAllActive() {
        return teamRepository.findAllActiveWithDetails();
    }

    public List<ServiceTeam> getAll() {
        return teamRepository.findAll();
    }

    public Optional<ServiceTeam> findById(Long id) {
        return teamRepository.findById(id);
    }

    public List<ServiceTeam> getTeamsForUser(Long userId) {
        return teamRepository.findTeamsByMemberId(userId);
    }

    public ServiceTeam save(ServiceTeam team) {
        return teamRepository.save(team);
    }

    public ServiceTeam createTeam(String name, String description, String color, Long managerId) {
        ServiceTeam team = new ServiceTeam(name, description, color);
        if (managerId != null) {
            userRepository.findById(managerId).ifPresent(team::setManager);
        }
        return teamRepository.save(team);
    }

    public ServiceTeam updateTeam(Long teamId, String name, String description,
                                   String color, Long managerId) {
        ServiceTeam team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));
        team.setName(name);
        team.setDescription(description);
        team.setColor(color != null ? color : "#4F46E5");
        if (managerId != null) {
            userRepository.findById(managerId).ifPresent(team::setManager);
        } else {
            team.setManager(null);
        }
        return teamRepository.save(team);
    }

    public void setMembers(Long teamId, List<Long> userIds) {
        ServiceTeam team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));
        Set<User> members = userIds.stream()
            .map(id -> userRepository.findById(id).orElseThrow())
            .collect(Collectors.toSet());
        team.setMembers(members);
        teamRepository.save(team);
    }

    public void addMember(Long teamId, Long userId) {
        ServiceTeam team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));
        userRepository.findById(userId).ifPresent(u -> team.getMembers().add(u));
        teamRepository.save(team);
    }

    public void removeMember(Long teamId, Long userId) {
        ServiceTeam team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));
        team.getMembers().removeIf(u -> u.getId().equals(userId));
        teamRepository.save(team);
    }

    public void toggleActive(Long teamId) {
        ServiceTeam team = teamRepository.findById(teamId)
            .orElseThrow(() -> new RuntimeException("Team not found"));
        team.setActive(!team.isActive());
        teamRepository.save(team);
    }
}
