package com.habittracker.repository;

import com.habittracker.model.Badge;
import com.habittracker.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findByHabit(Habit habit);

    boolean existsByHabitAndType(Habit habit, String type);
}
