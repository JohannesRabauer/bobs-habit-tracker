package com.habittracker.service;

import com.habittracker.dto.HabitStats;
import com.habittracker.model.Badge;
import com.habittracker.model.Habit;
import com.habittracker.model.HabitLog;
import com.habittracker.repository.BadgeRepository;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final BadgeRepository badgeRepository;

    public HabitService(HabitRepository habitRepository,
                        HabitLogRepository habitLogRepository,
                        BadgeRepository badgeRepository) {
        this.habitRepository = habitRepository;
        this.habitLogRepository = habitLogRepository;
        this.badgeRepository = badgeRepository;
    }

    // ── Habit CRUD ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Habit> findAllActive() {
        return habitRepository.findByActiveTrueOrderByName();
    }

    @Transactional(readOnly = true)
    public Habit findById(Long id) {
        return habitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found: " + id));
    }

    public Habit save(Habit habit) {
        return habitRepository.save(habit);
    }

    public void delete(Long id) {
        Habit habit = findById(id);
        habit.setActive(false);
        habitRepository.save(habit);
    }

    // ── Daily tracking ────────────────────────────────────────────────────────

    public void toggleLog(Long habitId, LocalDate date) {
        Habit habit = findById(habitId);
        Optional<HabitLog> existing = habitLogRepository.findByHabitAndLogDate(habit, date);
        if (existing.isPresent()) {
            HabitLog log = existing.get();
            log.setCompleted(!log.isCompleted());
            habitLogRepository.save(log);
        } else {
            HabitLog log = new HabitLog();
            log.setHabit(habit);
            log.setLogDate(date);
            log.setCompleted(true);
            habitLogRepository.save(log);
        }
        checkAndAwardBadges(habit);
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> getTodayStatus(List<Habit> habits, LocalDate date) {
        Map<Long, Boolean> status = new HashMap<>();
        for (Habit habit : habits) {
            boolean completed = habitLogRepository.findByHabitAndLogDate(habit, date)
                    .map(HabitLog::isCompleted)
                    .orElse(false);
            status.put(habit.getId(), completed);
        }
        return status;
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public HabitStats getStats(Habit habit) {
        List<HabitLog> completedLogs = habitLogRepository.findByHabitAndCompletedTrue(habit);
        Set<LocalDate> completedSet = completedLogs.stream()
                .map(HabitLog::getLogDate)
                .collect(Collectors.toSet());

        int currentStreak = calculateCurrentStreak(completedSet);
        int longestStreak = calculateLongestStreak(completedSet);
        int totalCompleted = completedLogs.size();

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(29);
        List<HabitLog> last30Logs = habitLogRepository.findByHabitAndLogDateBetween(habit, thirtyDaysAgo, today);
        long completedInLast30 = last30Logs.stream().filter(HabitLog::isCompleted).count();
        double completionRate = last30Logs.isEmpty() ? 0.0 : (completedInLast30 * 100.0 / 30);

        List<LocalDate> completedDates = completedLogs.stream()
                .map(HabitLog::getLogDate)
                .sorted()
                .collect(Collectors.toList());

        // How many days since habit was created up to today
        long daysSinceCreation = habit.getCreatedAt() != null
                ? habit.getCreatedAt().until(today).toTotalMonths() * 30L + habit.getCreatedAt().until(today).getDays()
                : 0;
        int totalDays = (int) Math.max(daysSinceCreation, totalCompleted);

        List<Badge> badges = badgeRepository.findByHabit(habit);

        return new HabitStats(currentStreak, longestStreak, totalCompleted, totalDays,
                completionRate, completedDates, badges);
    }

    // ── Streak helpers ────────────────────────────────────────────────────────

    private int calculateCurrentStreak(Set<LocalDate> completedDates) {
        LocalDate cursor = LocalDate.now();
        int streak = 0;
        while (completedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int calculateLongestStreak(Set<LocalDate> completedDates) {
        if (completedDates.isEmpty()) return 0;
        List<LocalDate> sorted = completedDates.stream().sorted().collect(Collectors.toList());
        int longest = 1;
        int current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }
        return longest;
    }

    // ── Badge award logic ─────────────────────────────────────────────────────

    /** Public entry point used by DataInitializer after seeding logs. */
    public void evaluateBadges(Long habitId) {
        checkAndAwardBadges(findById(habitId));
    }

    private void checkAndAwardBadges(Habit habit) {
        List<HabitLog> completedLogs = habitLogRepository.findByHabitAndCompletedTrue(habit);
        Set<LocalDate> completedSet = completedLogs.stream()
                .map(HabitLog::getLogDate)
                .collect(Collectors.toSet());

        int streak = calculateCurrentStreak(completedSet);
        awardStreakBadge(habit, streak, 7, "STREAK_7");
        awardStreakBadge(habit, streak, 30, "STREAK_30");
        awardStreakBadge(habit, streak, 100, "STREAK_100");

        LocalDate today = LocalDate.now();
        List<HabitLog> last30Logs = habitLogRepository.findByHabitAndLogDateBetween(
                habit, today.minusDays(29), today);
        long completedIn30 = last30Logs.stream().filter(HabitLog::isCompleted).count();
        double rate = completedIn30 * 100.0 / 30;
        awardRateBadge(habit, rate, 50, "RATE_50");
        awardRateBadge(habit, rate, 80, "RATE_80");
        awardRateBadge(habit, rate, 100, "RATE_100");
    }

    private void awardStreakBadge(Habit habit, int streak, int threshold, String type) {
        if (streak >= threshold && !badgeRepository.existsByHabitAndType(habit, type)) {
            Badge badge = new Badge();
            badge.setHabit(habit);
            badge.setType(type);
            badge.setEarnedAt(LocalDate.now());
            badgeRepository.save(badge);
        }
    }

    private void awardRateBadge(Habit habit, double rate, double threshold, String type) {
        if (rate >= threshold && !badgeRepository.existsByHabitAndType(habit, type)) {
            Badge badge = new Badge();
            badge.setHabit(habit);
            badge.setType(type);
            badge.setEarnedAt(LocalDate.now());
            badgeRepository.save(badge);
        }
    }
}
