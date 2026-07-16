package com.habittracker.controller;

import com.habittracker.dto.HabitForm;
import com.habittracker.dto.HabitStats;
import com.habittracker.model.Habit;
import com.habittracker.service.HabitService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Controller
@RequestMapping("/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @GetMapping
    public String list(Model model) {
        List<Habit> habits = habitService.findAllActive();
        // Build a map of currentStreak per habit for the table
        java.util.Map<Long, Integer> streaks = new java.util.HashMap<>();
        LocalDate today = LocalDate.now();
        for (Habit h : habits) {
            HabitStats stats = habitService.getStats(h);
            streaks.put(h.getId(), stats.currentStreak());
        }
        model.addAttribute("habits", habits);
        model.addAttribute("streaks", streaks);
        return "habits/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new HabitForm());
        model.addAttribute("editing", false);
        return "habits/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") HabitForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("editing", false);
            return "habits/form";
        }
        Habit habit = toEntity(form, new Habit());
        habitService.save(habit);
        flash.addFlashAttribute("successMessage", "Habit \"" + habit.getName() + "\" created.");
        return "redirect:/habits";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Habit habit = habitService.findById(id);
        model.addAttribute("form", toForm(habit));
        model.addAttribute("editing", true);
        return "habits/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") HabitForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("editing", true);
            return "habits/form";
        }
        Habit habit = habitService.findById(id);
        toEntity(form, habit);
        habitService.save(habit);
        flash.addFlashAttribute("successMessage", "Habit \"" + habit.getName() + "\" updated.");
        return "redirect:/habits";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        Habit habit = habitService.findById(id);
        String name = habit.getName();
        habitService.delete(id);
        flash.addFlashAttribute("successMessage", "Habit \"" + name + "\" deleted.");
        return "redirect:/habits";
    }

    @GetMapping("/{id}/stats")
    public String stats(@PathVariable Long id, Model model) {
        Habit habit = habitService.findById(id);
        HabitStats stats = habitService.getStats(habit);

        // Build last-30-days labels and data for Chart.js
        LocalDate today = LocalDate.now();
        List<String> chartLabels = new java.util.ArrayList<>();
        List<Integer> chartData = new java.util.ArrayList<>();
        java.util.Set<LocalDate> completed = new java.util.HashSet<>(stats.completedDates());
        for (int i = 29; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            chartLabels.add(d.toString());
            chartData.add(completed.contains(d) ? 1 : 0);
        }

        model.addAttribute("habit", habit);
        model.addAttribute("stats", stats);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartData", chartData);
        return "habits/stats";
    }

    @GetMapping("/{id}/calendar")
    public String calendar(@PathVariable Long id,
                           @RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Integer month,
                           Model model) {
        Habit habit = habitService.findById(id);
        HabitStats stats = habitService.getStats(habit);

        LocalDate today = LocalDate.now();
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        java.util.Set<LocalDate> completedSet = new java.util.HashSet<>(stats.completedDates());

        // Build calendar grid: list of weeks (each week = 7 LocalDate cells, null = padding)
        LocalDate firstDay = ym.atDay(1);
        int startDow = firstDay.getDayOfWeek().getValue() % 7; // 0=Sun … 6=Sat
        List<List<LocalDate>> weeks = new java.util.ArrayList<>();
        List<LocalDate> week = new java.util.ArrayList<>();
        for (int i = 0; i < startDow; i++) week.add(null);
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            week.add(ym.atDay(d));
            if (week.size() == 7) { weeks.add(week); week = new java.util.ArrayList<>(); }
        }
        while (week.size() < 7) week.add(null);
        if (!week.stream().allMatch(java.util.Objects::isNull)) weeks.add(week);

        model.addAttribute("habit", habit);
        model.addAttribute("yearMonth", ym);
        model.addAttribute("weeks", weeks);
        model.addAttribute("completedSet", completedSet);
        model.addAttribute("today", today);
        model.addAttribute("prevMonth", ym.minusMonths(1));
        model.addAttribute("nextMonth", ym.plusMonths(1));
        return "habits/calendar";
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private Habit toEntity(HabitForm form, Habit habit) {
        habit.setName(form.getName());
        habit.setDescription(form.getDescription());
        habit.setColor(form.getColor() != null ? form.getColor() : "#4CAF50");
        habit.setIcon(form.getIcon() != null ? form.getIcon() : "⭐");
        return habit;
    }

    private HabitForm toForm(Habit habit) {
        HabitForm form = new HabitForm();
        form.setId(habit.getId());
        form.setName(habit.getName());
        form.setDescription(habit.getDescription());
        form.setColor(habit.getColor());
        form.setIcon(habit.getIcon());
        return form;
    }
}
