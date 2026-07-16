package com.habittracker.controller;

import com.habittracker.service.HabitService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;

@Controller
@RequestMapping("/track")
public class TrackingController {

    private final HabitService habitService;

    public TrackingController(HabitService habitService) {
        this.habitService = habitService;
    }

    @PostMapping("/{habitId}")
    public String toggle(@PathVariable Long habitId) {
        habitService.toggleLog(habitId, LocalDate.now());
        return "redirect:/";
    }
}
