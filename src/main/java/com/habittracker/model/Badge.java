package com.habittracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "badge",
       uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "type"}))
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private LocalDate earnedAt;

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Habit getHabit() { return habit; }
    public void setHabit(Habit habit) { this.habit = habit; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getEarnedAt() { return earnedAt; }
    public void setEarnedAt(LocalDate earnedAt) { this.earnedAt = earnedAt; }
}
