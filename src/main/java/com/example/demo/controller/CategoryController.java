package com.example.demo.controller;

import com.example.demo.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Category Controller for handling category-related web requests
 */
@Controller
@RequiredArgsConstructor
public class CategoryController {

    private final CatalogService catalogService;

    /**
     * Get categories page - redirect to products categories
     * @param model Spring Model
     * @return Categories page
     */
    @GetMapping("/categories")
    public String categories(Model model) {
        // Set page metadata
        model.addAttribute("pageTitle", "Danh mục sản phẩm");
        model.addAttribute("pageDescription", "Khám phá các danh mục hoa tươi đa dạng tại StarShop");
        model.addAttribute("categories", catalogService.findAll());

        return "products/categories";
    }
}
