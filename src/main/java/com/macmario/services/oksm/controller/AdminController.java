package com.macmario.services.oksm.controller;

import com.macmario.services.oksm.model.*;
import com.macmario.services.oksm.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private UserService userService;
    @Autowired private SkillService skillService;

    // ── Users ──────────────────────────────────────────────────
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("newUser", new User());
        model.addAttribute("roles", User.Role.values());
        return "admin/users";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute User user,
                           @RequestParam(required = false, defaultValue = "false") boolean consultant,
                           RedirectAttributes ra) {
        try {
            user.setConsultant(consultant);
            userService.createUser(user);
            ra.addFlashAttribute("success", "Benutzer angelegt: " + user.getUsername());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/toggle/{id}")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleActive(id);
        ra.addFlashAttribute("success", "Benutzer-Status geändert.");
        return "redirect:/admin/users";
    }

    // ── Skills ─────────────────────────────────────────────────
    @GetMapping("/skills")
    public String skills(Model model) {
        model.addAttribute("skills", skillService.getAllActiveSkills());
        model.addAttribute("categories", skillService.getAllCategories());
        model.addAttribute("newSkill", new Skill());
        model.addAttribute("newCategory", new SkillCategory());
        return "admin/skills";
    }

    @PostMapping("/skills/save")
    public String saveSkill(@ModelAttribute Skill skill,
                            @RequestParam Long categoryId,
                            RedirectAttributes ra) {
        skillService.findCategoryById(categoryId).ifPresent(skill::setCategory);
        skillService.saveSkill(skill);
        ra.addFlashAttribute("success", "Skill gespeichert.");
        return "redirect:/admin/skills";
    }

    @PostMapping("/skills/move")
    public String moveSkill(@RequestParam Long skillId,
                            @RequestParam Long categoryId,
                            RedirectAttributes ra) {
        try {
            skillService.moveSkillToCategory(skillId, categoryId);
            ra.addFlashAttribute("success", "Skill erfolgreich verschoben.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Fehler beim Verschieben: " + e.getMessage());
        }
        return "redirect:/admin/skills";
    }

    @PostMapping("/skills/toggle/{id}")
    public String toggleSkill(@PathVariable Long id, RedirectAttributes ra) {
        skillService.toggleSkillActive(id);
        ra.addFlashAttribute("success", "Skill-Status geändert.");
        return "redirect:/admin/skills";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute SkillCategory category, RedirectAttributes ra) {
        skillService.saveCategory(category);
        ra.addFlashAttribute("success", "Kategorie gespeichert.");
        return "redirect:/admin/skills";
    }

    // ── SubSkills ──────────────────────────────────────────────
    @PostMapping("/subskills/save")
    public String saveSubSkill(@RequestParam Long skillId,
                               @RequestParam String name,
                               @RequestParam(required = false) String description,
                               @RequestParam(defaultValue = "0") int displayOrder,
                               RedirectAttributes ra) {
        skillService.createSubSkill(skillId, name, description, displayOrder);
        ra.addFlashAttribute("success", "SubSkill gespeichert.");
        return "redirect:/admin/skills";
    }

    @PostMapping("/subskills/toggle/{id}")
    public String toggleSubSkill(@PathVariable Long id, RedirectAttributes ra) {
        skillService.toggleSubSkillActive(id);
        ra.addFlashAttribute("success", "SubSkill-Status geändert.");
        return "redirect:/admin/skills";
    }

    @PostMapping("/subskills/delete/{id}")
    public String deleteSubSkill(@PathVariable Long id, RedirectAttributes ra) {
        skillService.deleteSubSkill(id);
        ra.addFlashAttribute("success", "SubSkill gelöscht.");
        return "redirect:/admin/skills";
    }
}
