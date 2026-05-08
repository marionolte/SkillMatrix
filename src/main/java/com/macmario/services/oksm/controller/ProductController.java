package com.macmario.services.oksm.controller;

import com.macmario.services.oksm.model.Product;
import com.macmario.services.oksm.service.ProductService;
import com.macmario.services.oksm.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired private ProductService productService;
    @Autowired private SkillService skillService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.getAllActiveProducts());
        return "manager/products";
    }

    @GetMapping("/manage/new")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String newForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("allSkills", skillService.getAllActiveSkills());
        model.addAttribute("categories", skillService.getAllCategories());
        return "manager/product-form";
    }

    @GetMapping("/manage/edit/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String editForm(@PathVariable Long id, Model model) {
        var product = productService.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
        model.addAttribute("product", product);
        model.addAttribute("allSkills", skillService.getAllActiveSkills());
        model.addAttribute("categories", skillService.getAllCategories());
        return "manager/product-form";
    }

    @PostMapping("/manage/save")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String save(@ModelAttribute Product product,
                       @RequestParam(required = false) List<Long> skillIds,
                       RedirectAttributes ra) {
        productService.saveWithSkills(product, skillIds != null ? skillIds : List.of());
        ra.addFlashAttribute("success", "Produkt gespeichert.");
        return "redirect:/products";
    }

    @PostMapping("/manage/toggle/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        productService.toggleActive(id);
        ra.addFlashAttribute("success", "Produkt-Status geändert.");
        return "redirect:/products";
    }
}
