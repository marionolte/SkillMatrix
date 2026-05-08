package com.macmario.services.oksm.controller;

import com.macmario.services.oksm.model.ServiceTeam;
import com.macmario.services.oksm.model.User;
import com.macmario.services.oksm.service.ServiceTeamService;
import com.macmario.services.oksm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/teams")
public class ServiceTeamController {

    @Autowired private ServiceTeamService teamService;
    @Autowired private UserService userService;

    @GetMapping
    public String list(Authentication auth, Model model) {
        User current = userService.getCurrentUser(auth.getName());
        boolean isManagerOrAdmin = current.getRole() == User.Role.MANAGER
                                || current.getRole() == User.Role.ADMIN;
        if (isManagerOrAdmin) {
            model.addAttribute("teams", teamService.getAllActive());
        } else {
            model.addAttribute("teams", teamService.getTeamsForUser(current.getId()));
        }
        model.addAttribute("isManagerOrAdmin", isManagerOrAdmin);
        return "teams/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        ServiceTeam team = teamService.findById(id)
            .orElseThrow(() -> new RuntimeException("Team not found"));
        User current = userService.getCurrentUser(auth.getName());
        List<User> allEngineers = userService.getAllEngineers();
        List<User> allManagers  = userService.getAllByRole(User.Role.MANAGER);

        model.addAttribute("team", team);
        model.addAttribute("allEngineers", allEngineers);
        model.addAttribute("allManagers", allManagers);
        model.addAttribute("isManagerOrAdmin",
            current.getRole() == User.Role.MANAGER || current.getRole() == User.Role.ADMIN);
        return "teams/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("team", new ServiceTeam());
        model.addAttribute("allManagers", userService.getAllByRole(User.Role.MANAGER));
        return "teams/form";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        ServiceTeam team = teamService.findById(id)
            .orElseThrow(() -> new RuntimeException("Team not found"));
        model.addAttribute("team", team);
        model.addAttribute("allManagers", userService.getAllByRole(User.Role.MANAGER));
        return "teams/form";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String save(@RequestParam(required = false) Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) String color,
                       @RequestParam(required = false) Long managerId,
                       RedirectAttributes ra) {
        if (id != null) {
            teamService.updateTeam(id, name, description, color, managerId);
            ra.addFlashAttribute("success", "Team aktualisiert.");
        } else {
            teamService.createTeam(name, description, color, managerId);
            ra.addFlashAttribute("success", "Team angelegt.");
        }
        return "redirect:/teams";
    }

    @PostMapping("/{id}/members/add")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String addMember(@PathVariable Long id,
                            @RequestParam Long userId,
                            RedirectAttributes ra) {
        teamService.addMember(id, userId);
        ra.addFlashAttribute("success", "Mitglied hinzugefügt.");
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/members/remove")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String removeMember(@PathVariable Long id,
                               @RequestParam Long userId,
                               RedirectAttributes ra) {
        teamService.removeMember(id, userId);
        ra.addFlashAttribute("success", "Mitglied entfernt.");
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/members/set")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String setMembers(@PathVariable Long id,
                             @RequestParam(required = false) List<Long> userIds,
                             RedirectAttributes ra) {
        teamService.setMembers(id, userIds != null ? userIds : List.of());
        ra.addFlashAttribute("success", "Mitglieder aktualisiert.");
        return "redirect:/teams/" + id;
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        teamService.toggleActive(id);
        ra.addFlashAttribute("success", "Team-Status geändert.");
        return "redirect:/teams";
    }
}
