package com.habittracker;

import com.habittracker.model.Habit;
import com.habittracker.model.HabitLog;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import com.habittracker.service.HabitService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitService habitService;

    public DataInitializer(HabitRepository habitRepository,
                           HabitLogRepository habitLogRepository,
                           HabitService habitService) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.habitService = habitService;
    }

    @Override
    public void run(String... args) {
        if (habitRepository.count() > 0) return; // idempotent

        Habit exercise = createHabit("Morning Exercise", "30 minutes of physical activity", "#ef4444", "🏃");
        Habit reading  = createHabit("Read 30 Minutes",  "Daily reading habit",             "#3b82f6", "📚");
        Habit water    = createHabit("Drink 8 Glasses",  "Stay hydrated throughout the day", "#06b6d4", "💧");

        // exercise: completed every day for past 14 days → triggers STREAK_7 badge
        boolean[] exercisePattern = { true, true, true, true, true, true, true,
                                      true, true, true, true, true, true, true };
        // reading: alternating days → partial rate
        boolean[] readingPattern  = { true, false, true, false, true, true, true,
                                      false, true, false, true, true, false, true  };
        // water: most days, a few gaps
        boolean[] waterPattern    = { true, true, true, false, true, true, true,
                                      true, false, true, true, true, true, false };

        LocalDate today = LocalDate.now();
        seedLogs(exercise, today, exercisePattern);
        seedLogs(reading,  today, readingPattern);
        seedLogs(water,    today, waterPattern);

        // Trigger badge evaluation for seeded data
        habitService.evaluateBadges(exercise.getId());
        habitService.evaluateBadges(reading.getId());
        habitService.evaluateBadges(water.getId());
    }

    private Habit createHabit(String name, String description, String color, String icon) {
        Habit h = new Habit();
        h.setName(name);
        h.setDescription(description);
        h.setColor(color);
        h.setIcon(icon);
        return habitService.save(h);
    }

    private void seedLogs(Habit habit, LocalDate today, boolean[] pattern) {
        for (int i = 0; i < pattern.length; i++) {
            LocalDate date = today.minusDays(pattern.length - 1 - i);
            HabitLog log = new HabitLog();
            log.setHabit(habit);
            log.setLogDate(date);
            log.setCompleted(pattern[i]);
            habitLogRepository.save(log);
        }
    }
}
