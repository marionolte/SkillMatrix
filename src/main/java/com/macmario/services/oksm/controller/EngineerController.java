package com.macmario.services.oksm.controller;

import com.macmario.services.oksm.model.*;
import com.macmario.services.oksm.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/engineer")
public class EngineerController {

    @Autowired private UserService userService;
    @Autowired private SkillService skillService;
    @Autowired private ServiceTeamService teamService;
    @Autowired private EngineerExcelService engineerExcelService;

    @GetMapping("/skills")
    public String mySkills(Authentication auth, Model model) {
        User user = userService.getCurrentUser(auth.getName());
        var mySkills = skillService.getEngineerSkills(user.getId());

        // Pre-load active subskills per skill to avoid lazy-load in template
        java.util.Map<Long, java.util.List<com.macmario.services.oksm.model.SubSkill>> subSkillMap =
            new java.util.LinkedHashMap<>();
        for (var es : mySkills) {
            Long sid = es.getSkill().getId();
            subSkillMap.put(sid, skillService.getSubSkillsForSkill(sid)
                .stream().filter(ss -> ss.isActive()).toList());
        }

        model.addAttribute("user", user);
        model.addAttribute("mySkills",    mySkills);
        model.addAttribute("mySubSkills", skillService.getEngineerSubSkills(user.getId()));
        model.addAttribute("allSkills",   skillService.getAllActiveSkills());
        model.addAttribute("categories",  skillService.getAllCategories());
        model.addAttribute("levels",      EngineerSkill.SkillLevel.values());
        model.addAttribute("myTeams",     teamService.getTeamsForUser(user.getId()));
        model.addAttribute("subSkillMap", subSkillMap);
        return "engineer/skills";
    }

    // ── Top-level skill ──────────────────────────────────────
    @PostMapping("/skills/save")
    public String saveSkill(@RequestParam Long skillId,
                            @RequestParam EngineerSkill.SkillLevel level,
                            @RequestParam(required = false) String notes,
                            Authentication auth, RedirectAttributes ra) {
        User user = userService.getCurrentUser(auth.getName());
        skillService.saveOrUpdateEngineerSkill(user.getId(), skillId, level, notes);
        ra.addFlashAttribute("success", "Skill gespeichert.");
        return "redirect:/engineer/skills";
    }

    @PostMapping("/skills/remove")
    public String removeSkill(@RequestParam Long skillId,
                              Authentication auth, RedirectAttributes ra) {
        User user = userService.getCurrentUser(auth.getName());
        skillService.removeEngineerSkill(user.getId(), skillId);
        ra.addFlashAttribute("success", "Skill entfernt.");
        return "redirect:/engineer/skills";
    }

    // ── SubSkill ─────────────────────────────────────────────
    @PostMapping("/subskills/save")
    public String saveSubSkill(@RequestParam Long subSkillId,
                               @RequestParam EngineerSkill.SkillLevel level,
                               @RequestParam(required = false) String notes,
                               Authentication auth, RedirectAttributes ra) {
        User user = userService.getCurrentUser(auth.getName());
        skillService.saveOrUpdateEngineerSubSkill(user.getId(), subSkillId, level, notes);
        ra.addFlashAttribute("success", "SubSkill gespeichert.");
        return "redirect:/engineer/skills";
    }

    @PostMapping("/subskills/remove")
    public String removeSubSkill(@RequestParam Long subSkillId,
                                 Authentication auth, RedirectAttributes ra) {
        User user = userService.getCurrentUser(auth.getName());
        skillService.removeEngineerSubSkill(user.getId(), subSkillId);
        ra.addFlashAttribute("success", "SubSkill entfernt.");
        return "redirect:/engineer/skills";
    }

    // ── Excel Export ─────────────────────────────────────────
    @GetMapping("/skills/export")
    public void exportMySkills(Authentication auth, HttpServletResponse response) throws Exception {
        User user = userService.getCurrentUser(auth.getName());
        byte[] data = engineerExcelService.exportMySkills(user.getId());
        String filename = "skills_" + user.getUsername() + "_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    // ── Excel Import ─────────────────────────────────────────
    @PostMapping("/skills/import")
    public String importMySkills(@RequestParam("file") MultipartFile file,
                                 Authentication auth, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Bitte eine Excel-Datei auswählen.");
            return "redirect:/engineer/skills";
        }
        User user = userService.getCurrentUser(auth.getName());
        try {
            var result = engineerExcelService.importMySkills(user.getId(), file);
            String msg = String.format(
                "Import abgeschlossen: %d Zeilen, %d neu, %d aktualisiert.",
                result.rows(), result.created(), result.updated());
            ra.addFlashAttribute("success", msg);
            if (!result.errors().isEmpty()) {
                ra.addFlashAttribute("importErrors", result.errors());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Import fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/engineer/skills";
    }
}

