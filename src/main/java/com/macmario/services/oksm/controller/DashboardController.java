package com.macmario.services.oksm.controller;

import com.macmario.services.oksm.model.User;
import com.macmario.services.oksm.service.SkillService;
import com.macmario.services.oksm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @Autowired private UserService userService;
    @Autowired private SkillService skillService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = userService.getCurrentUser(auth.getName());
        model.addAttribute("user", user);

        if (user.getRole() == User.Role.ENGINEER) {
            var mySkills = skillService.getEngineerSkills(user.getId());
            model.addAttribute("mySkills", mySkills);
            return "engineer/dashboard";
        } else {
            // Manager / Admin
            var matrixData = skillService.buildSkillMatrix();
            model.addAttribute("engineers", matrixData.get("engineers"));
            model.addAttribute("skills", matrixData.get("skills"));
            model.addAttribute("lookup", matrixData.get("lookup"));
            model.addAttribute("allEngineers", userService.getAllEngineers());
            return "manager/dashboard";
        }
    }
}
