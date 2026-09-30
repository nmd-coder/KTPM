package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Blog Controller
 * Xử lý điều hướng đến trang blog tĩnh
 */
@Controller
@RequestMapping("/blog")
@Slf4j
@Tag(name = "📝 Blog", description = "Blog page navigation")
public class BlogController {

    @Operation(
        summary = "Blog page",
        description = "Hiển thị trang blog với các bài viết về hoa tươi và nghệ thuật cắm hoa"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Trang blog được tải thành công")
    })
    @GetMapping
    public String blog() {
        log.info("Blog page accessed");
        return "customer/blog";
    }
}
