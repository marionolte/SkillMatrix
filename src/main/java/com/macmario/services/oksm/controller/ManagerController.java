package com.macmario.services.oksm.controller;

import com.macmario.services.oksm.model.*;
import com.macmario.services.oksm.service.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/manager")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ManagerController {

    @Autowired private UserService userService;
    @Autowired private SkillService skillService;
    @Autowired private ProductService productService;
    @Autowired private ExcelExportService excelExportService;

    @GetMapping("/matrix")
    public String skillMatrix(Model model) {
        var matrixData = skillService.buildSkillMatrix();
        model.addAttribute("engineers", matrixData.get("engineers"));
        model.addAttribute("skills", matrixData.get("skills"));
        model.addAttribute("lookup", matrixData.get("lookup"));
        model.addAttribute("levels", EngineerSkill.SkillLevel.values());
        model.addAttribute("categories", skillService.getAllCategories());
        return "manager/matrix";
    }

    @GetMapping("/gaps")
    public String gapAnalysis(@RequestParam(required = false) List<Long> skillIds,
                              @RequestParam(required = false, defaultValue = "EXPERTE") String minLevel,
                              Model model) {
        var allSkills = skillService.getAllActiveSkills();
        var categories = skillService.getAllCategories();
        model.addAttribute("allSkills", allSkills);
        model.addAttribute("categories", categories);
        model.addAttribute("levels", EngineerSkill.SkillLevel.values());
        model.addAttribute("selectedMinLevel", minLevel);
        model.addAttribute("selectedSkillIds", skillIds != null ? skillIds : List.of());

        if (skillIds != null && !skillIds.isEmpty()) {
            EngineerSkill.SkillLevel level = EngineerSkill.SkillLevel.valueOf(minLevel);
            var gapResult = skillService.analyzeGaps(skillIds, level);
            model.addAttribute("gapData", gapResult.get("engineers"));
            model.addAttribute("requiredSkillIds", gapResult.get("requiredSkillIds"));

            // Get skill names for selected IDs
            var selectedSkills = allSkills.stream()
                .filter(s -> skillIds.contains(s.getId()))
                .toList();
            model.addAttribute("selectedSkills", selectedSkills);
        }
        return "manager/gaps";
    }

    @GetMapping("/engineers/{id}")
    public String engineerDetail(@PathVariable Long id, Model model) {
        var engineer = userService.findById(id)
            .orElseThrow(() -> new RuntimeException("Engineer not found"));
        var skills = skillService.getEngineerSkills(id);
        model.addAttribute("engineer", engineer);
        model.addAttribute("engineerSkills", skills);
        model.addAttribute("levels", EngineerSkill.SkillLevel.values());
        return "manager/engineer-detail";
    }

    @GetMapping("/export/excel")
    public void exportExcel(HttpServletResponse response) throws Exception {
        byte[] data = excelExportService.exportSkillMatrix();
        String filename = "skill-matrix_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }
}
