package com.gozero.dashboard.web;

import com.gozero.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Hidden
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("view", service.build());
        return "index";
    }

    @PostMapping("/refresh")
    public String refresh(RedirectAttributes redirect) {
        redirect.addFlashAttribute("refreshSteps", service.refreshPipeline());
        return "redirect:/";
    }
}
