package com.habittracker.controller;

import com.habittracker.model.Habit;
import com.habittracker.service.HabitService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final HabitService habitService;

    public HomeController(HabitService habitService) {
        this.habitService = habitService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        List<Habit> habits = habitService.findAllActive();
        Map<Long, Boolean> todayStatus = habitService.getTodayStatus(habits, today);

        model.addAttribute("habits", habits);
        model.addAttribute("todayStatus", todayStatus);
        model.addAttribute("today", today);
        model.addAttribute("completedCount",
                todayStatus.values().stream().filter(Boolean::booleanValue).count());
        return "dashboard";
    }
}
