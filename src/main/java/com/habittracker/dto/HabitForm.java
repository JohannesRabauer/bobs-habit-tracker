package com.habittracker.dto;

import jakarta.validation.constraints.NotBlank;

public class HabitForm {

    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    private String color = "#4CAF50";

    private String icon = "⭐";

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
