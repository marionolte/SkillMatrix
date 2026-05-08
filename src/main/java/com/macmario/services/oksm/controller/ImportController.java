package com.macmario.services.oksm.controller;

import com.macmario.services.oksm.service.ExcelImportService;
import com.macmario.services.oksm.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/import")
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ImportController {

    @Autowired private ExcelImportService importService;
    @Autowired private UserService userService;

    @GetMapping
    public String importPage(Model model) {
        model.addAttribute("engineers", userService.getAllEngineers());
        return "manager/import";
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        byte[] data = importService.generateImportTemplate(userService.getAllEngineers());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment; filename=\"skillmatrix-import-template.xlsx\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file,
                         RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Bitte eine Excel-Datei auswählen.");
            return "redirect:/import";
        }
        try {
            var result = importService.importFromExcel(file);
            String msg = String.format(
                "Import abgeschlossen: %d Zeilen verarbeitet, %d neu angelegt, %d aktualisiert.",
                result.rows(), result.created(), result.updated());
            ra.addFlashAttribute("success", msg);
            if (!result.errors().isEmpty()) {
                ra.addFlashAttribute("importErrors", result.errors());
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Import fehlgeschlagen: " + e.getMessage());
        }
        return "redirect:/import";
    }
}
