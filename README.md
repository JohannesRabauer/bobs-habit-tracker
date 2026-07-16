# Bob's Habit Tracker 🔥

A clean, single-user habit tracker built with **Spring Boot 3.3**, **Thymeleaf**, and **Bootstrap 5**.
Track daily habits, watch streaks grow, and earn achievement badges — all running locally with zero external dependencies.

---

## Features

| Area | What you get |
|------|-------------|
| **Dashboard** | One card per habit with colour stripe, today's status, and a one-click toggle |
| **Habit CRUD** | Create, edit, and soft-delete habits with a name, description, colour, and icon |
| **Streaks** | Live current streak and all-time longest streak calculated from your log history |
| **Statistics** | 30-day completion rate, total completions, and a Chart.js bar chart for the last 30 days |
| **Calendar** | Month grid view with green/grey/white cells per day; prev/next month navigation |
| **Badges** | Six auto-awarded achievement badges: 7/30/100-day streaks and 50/80/100 % monthly rates |
| **Sample data** | Three starter habits seeded on first launch so the app is immediately useful |

---

## Tech Stack

- **Java 21** · **Spring Boot 3.3**
- **Spring Data JPA** · **Hibernate** (schema auto-created on startup)
- **H2** file-mode database — data survives restarts, no DB server needed
- **Thymeleaf** server-side templates
- **Bootstrap 5.3** + **Bootstrap Icons 1.11** via CDN
- **Chart.js 4.4** via CDN

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+

### Run

```bash
git clone https://github.com/JohannesRabauer/bobs-habit-tracker.git
cd bobs-habit-tracker
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) in your browser.

The H2 database file is created at `./data/habitdb.mv.db` on first run.
Three sample habits are seeded automatically if the database is empty.

### Build a fat JAR

```bash
mvn package -DskipTests
java -jar target/habit-tracker-0.0.1-SNAPSHOT.jar
```

---

## Project Structure

```
src/main/java/com/habittracker/
├── HabitTrackerApplication.java
├── DataInitializer.java          # seeds sample data on first launch
├── controller/
│   ├── HomeController.java       # GET /
│   ├── HabitController.java      # GET|POST /habits/**
│   ├── TrackingController.java   # POST /track/{id}
│   └── GlobalModelAdvice.java
├── dto/
│   ├── HabitForm.java
│   └── HabitStats.java
├── model/
│   ├── Habit.java
│   ├── HabitLog.java
│   └── Badge.java
├── repository/
│   ├── HabitRepository.java
│   ├── HabitLogRepository.java
│   └── BadgeRepository.java
└── service/
    └── HabitService.java         # all business logic, streak calc, badge awards

src/main/resources/
├── application.properties
├── static/
│   ├── css/app.css
│   └── js/app.js
└── templates/
    ├── dashboard.html
    ├── fragments/
    │   ├── navbar.html
    │   └── flash.html
    └── habits/
        ├── list.html
        ├── form.html
        ├── stats.html
        └── calendar.html
```

---

## Routes

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/` | Dashboard — today's habits |
| `POST` | `/track/{id}` | Toggle completion for today |
| `GET` | `/habits` | Habit list |
| `GET` | `/habits/new` | New habit form |
| `POST` | `/habits` | Create habit |
| `GET` | `/habits/{id}/edit` | Edit habit form |
| `POST` | `/habits/{id}` | Update habit |
| `POST` | `/habits/{id}/delete` | Soft-delete habit |
| `GET` | `/habits/{id}/stats` | Statistics + Chart.js bar chart |
| `GET` | `/habits/{id}/calendar` | Month calendar view |
| `GET` | `/h2-console` | H2 database console (dev) |

---

## Badges

| Badge | Criteria |
|-------|----------|
| 🔥 **7-Day Streak** | 7 consecutive completed days |
| 🏆 **30-Day Streak** | 30 consecutive completed days |
| 💎 **100-Day Streak** | 100 consecutive completed days |
| ⭐ **50 % Rate** | ≥ 50 % completion in the last 30 days |
| 🌟 **80 % Rate** | ≥ 80 % completion in the last 30 days |
| 🎯 **Perfect Month** | 100 % completion in the last 30 days |

Badges are awarded automatically and stored once per habit — re-earning them is idempotent.

---

## Configuration

All settings live in [`src/main/resources/application.properties`](src/main/resources/application.properties).

| Property | Default | Notes |
|----------|---------|-------|
| `spring.datasource.url` | `jdbc:h2:file:./data/habitdb` | Change path to move the DB file |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema is kept in sync automatically |
| `spring.h2.console.enabled` | `true` | Disable in production |
| `spring.thymeleaf.cache` | `false` | Set `true` for production |

---

## License

MIT
