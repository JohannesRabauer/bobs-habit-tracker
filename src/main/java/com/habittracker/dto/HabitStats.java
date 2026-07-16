package com.habittracker.dto;

import com.habittracker.model.Badge;
import java.time.LocalDate;
import java.util.List;

public record HabitStats(
        int currentStreak,
        int longestStreak,
        int totalCompleted,
        int totalDays,
        double completionRate,
        List<LocalDate> completedDates,
        List<Badge> badges
) {}
