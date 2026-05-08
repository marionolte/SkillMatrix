package com.macmario.services.oksm.controller;

import com.macmario.services.oksm.model.User;
import com.macmario.services.oksm.repository.UserRepository;
import com.macmario.services.oksm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping
    public String profile(Authentication auth, Model model) {
        User user = userService.getCurrentUser(auth.getName());
        model.addAttribute("user", user);
        return "profile/edit";
    }

    @PostMapping("/save")
    public String save(@RequestParam String fullName,
                       @RequestParam(required = false) String email,
                       @RequestParam(required = false) String team,
                       @RequestParam(required = false) String department,
                       Authentication auth,
                       RedirectAttributes ra) {
        User user = userService.getCurrentUser(auth.getName());
        user.setFullName(fullName);
        user.setEmail(email);
        user.setTeam(team);
        user.setDepartment(department);
        userRepository.save(user);
        ra.addFlashAttribute("success", "Profil erfolgreich gespeichert.");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        User user = userService.getCurrentUser(auth.getName());

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("pwError", "Aktuelles Passwort ist falsch.");
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("pwError", "Neue Passwörter stimmen nicht überein.");
            return "redirect:/profile";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("pwError", "Passwort muss mindestens 6 Zeichen lang sein.");
            return "redirect:/profile";
        }

        userService.changePassword(user.getId(), newPassword);
        ra.addFlashAttribute("pwSuccess", "Passwort erfolgreich geändert.");
        return "redirect:/profile";
    }
}
