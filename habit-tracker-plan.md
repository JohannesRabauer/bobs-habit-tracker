# Habit Tracker — Implementation Plan

## Top-Level Overview

Build a fully functional, single-user habit tracker as a Spring Boot 3.3 / Java 21 / Thymeleaf web application.  
Persistence: H2 in-file mode via Spring Data JPA + Hibernate.  
UI: Bootstrap 5 (CDN) + Chart.js (CDN) for responsive layout and statistics charts.  
No authentication — single-user, runs locally.

Features: habit CRUD, daily tracking dashboard, streak calculation, per-habit statistics, month calendar view, and auto-awarded achievement badges.

---

## Sub-Tasks

---

### Sub-Task 1 — Project Scaffold & Build Configuration

**Status:** `[ ] pending`

**Intent**  
Create the Maven project structure, `pom.xml`, `application.properties`, and the Spring Boot entry point. This is the foundation every other sub-task depends on.

**Expected Outcomes**
- `pom.xml` compiles with `mvn package` (Spring Boot fat JAR produced)
- Application starts and serves an empty root URL without errors
- H2 file database is configured and the H2 console is reachable at `/h2-console`

**Todo List**
1. Create `pom.xml` with:
   - `spring-boot-starter-web`
   - `spring-boot-starter-thymeleaf`
   - `spring-boot-starter-data-jpa`
   - `com.h2database:h2` (runtime scope)
   - `spring-boot-starter-validation`
   - Java 21 compiler settings
2. Create `src/main/java/com/habittracker/HabitTrackerApplication.java` — `@SpringBootApplication` main class
3. Create `src/main/resources/application.properties`:
   - `spring.datasource.url=jdbc:h2:file:./data/habitdb`
   - `spring.jpa.hibernate.ddl-auto=update`
   - H2 console enabled
   - Thymeleaf cache disabled (dev convenience)
4. Create placeholder `src/main/resources/templates/index.html` (basic HTML, returns 200)
5. Create `src/main/java/com/habittracker/controller/HomeController.java` mapping `GET /` to `index`

**Relevant Context**
- Package root: `com.habittracker`
- All subsequent sub-tasks add code inside this package tree

---

### Sub-Task 2 — Domain Model & Database Schema

**Status:** `[ ] pending`

**Intent**  
Define the JPA entities that model the core domain. Hibernate will generate the schema from these entities on startup (`ddl-auto=update`).

**Expected Outcomes**
- Three `@Entity` classes are compilable and mapped correctly
- Hibernate auto-creates the tables on first startup
- Basic repository interfaces exist for all three entities

**Todo List**
1. Create `com.habittracker.model.Habit`:
   - Fields: `id` (Long, PK), `name` (String, not null), `description` (String), `color` (String, default `#4CAF50`), `icon` (String, e.g. emoji or Bootstrap icon class, default `⭐`), `createdAt` (LocalDate), `active` (boolean, default true)
   - `@OneToMany(mappedBy="habit", cascade=ALL, orphanRemoval=true)` to `HabitLog` and `Badge`
2. Create `com.habittracker.model.HabitLog`:
   - Fields: `id` (Long, PK), `habit` (`@ManyToOne`), `logDate` (LocalDate), `completed` (boolean)
   - Unique constraint on `(habit_id, log_date)`
3. Create `com.habittracker.model.Badge`:
   - Fields: `id` (Long, PK), `habit` (`@ManyToOne`), `type` (String — e.g. `STREAK_7`, `STREAK_30`, `STREAK_100`, `RATE_50`, `RATE_80`, `RATE_100`), `earnedAt` (LocalDate)
   - Unique constraint on `(habit_id, type)` so a badge is awarded only once per habit
4. Create `com.habittracker.repository.HabitRepository` extending `JpaRepository<Habit, Long>`
5. Create `com.habittracker.repository.HabitLogRepository` extending `JpaRepository<HabitLog, Long>`:
   - `Optional<HabitLog> findByHabitAndLogDate(Habit, LocalDate)`
   - `List<HabitLog> findByHabitAndLogDateBetween(Habit, LocalDate, LocalDate)`
   - `List<HabitLog> findByHabitAndCompletedTrue(Habit)`
6. Create `com.habittracker.repository.BadgeRepository` extending `JpaRepository<Badge, Long>`:
   - `List<Badge> findByHabit(Habit)`
   - `boolean existsByHabitAndType(Habit, String)`

**Relevant Context**
- `HabitLog` unique constraint prevents double-logging on the same day
- `Badge` unique constraint ensures idempotent badge award logic

---

### Sub-Task 3 — Service Layer

**Status:** `[ ] pending`

**Intent**  
Encapsulate all business logic in a service layer so controllers stay thin. This includes streak calculation, statistics computation, and badge award logic.

**Expected Outcomes**
- `HabitService` provides all CRUD and query operations used by controllers
- Streak and statistics values are computed correctly
- Badges are auto-awarded whenever a log is saved

**Todo List**
1. Create `com.habittracker.service.HabitService` (`@Service`, `@Transactional`):

   **Habit CRUD**
   - `List<Habit> findAllActive()` — returns active habits ordered by name
   - `Habit findById(Long)` — throws if not found
   - `Habit save(Habit)` — create or update
   - `void delete(Long)` — soft-delete: sets `active=false`

   **Daily tracking**
   - `void toggleLog(Long habitId, LocalDate date)` — creates or toggles `HabitLog.completed`; after toggle, calls badge-check
   - `Map<Long, Boolean> getTodayStatus(List<Habit>, LocalDate)` — returns completion status for each habit for given date

   **Streak calculation** (private helper used by `getStats`)
   - Walk `HabitLog` records sorted descending by date; count consecutive completed days up to today → `currentStreak`
   - Walk all completed logs ascending → find the longest run → `longestStreak`

   **Statistics DTO**
   - Create `com.habittracker.dto.HabitStats` (record or class):
     - `int currentStreak`, `int longestStreak`
     - `int totalCompleted`, `int totalDays`
     - `double completionRate` (last 30 days)
     - `List<LocalDate> completedDates` (for calendar)
     - `List<Badge> badges`
   - `HabitStats getStats(Habit)` — populates all fields

   **Badge award logic** (called after every `toggleLog`)
   - Check if current streak hits 7 / 30 / 100; award `STREAK_7` / `STREAK_30` / `STREAK_100` if not already awarded
   - Check 30-day completion rate; award `RATE_50` / `RATE_80` / `RATE_100` thresholds if not already awarded
   - Use `BadgeRepository.existsByHabitAndType` to avoid duplicates

2. No external libraries for streak logic — pure Java date arithmetic

**Relevant Context**
- `HabitLogRepository.findByHabitAndCompletedTrue` provides the full log for streak computation
- Badge award is idempotent via the unique DB constraint (double-save is safe)

---

### Sub-Task 4 — Controllers & Form Beans

**Status:** `[ ] pending`

**Intent**  
Wire HTTP routes to service calls and template model attributes. Keep controllers thin — delegate everything to `HabitService`.

**Expected Outcomes**
- All pages are reachable via a browser with correct data
- Form submissions create/edit/delete habits and toggle daily logs
- Redirect-after-POST pattern is followed throughout to prevent double-submit

**Todo List**
1. Create `com.habittracker.controller.HomeController`:
   - `GET /` → load today's habits + completion status + all badges → render `dashboard`
2. Create `com.habittracker.controller.HabitController`:
   - `GET /habits` → list all active habits → render `habits/list`
   - `GET /habits/new` → empty form → render `habits/form`
   - `POST /habits` → validate + save → redirect `/habits`
   - `GET /habits/{id}/edit` → load habit + form → render `habits/form`
   - `POST /habits/{id}` → validate + update → redirect `/habits`
   - `POST /habits/{id}/delete` → soft-delete → redirect `/habits`
   - `GET /habits/{id}/stats` → load habit + HabitStats → render `habits/stats`
   - `GET /habits/{id}/calendar` → load habit + stats → render `habits/calendar`
3. Create `com.habittracker.controller.TrackingController`:
   - `POST /track/{habitId}` → `toggleLog(habitId, today)` → redirect `/`
4. Create `com.habittracker.dto.HabitForm` (used as `@ModelAttribute`):
   - Fields: `name` (`@NotBlank`), `description`, `color`, `icon`
   - Bidirectionally maps to/from `Habit` entity

**Relevant Context**
- All POST handlers use `RedirectAttributes` for flash messages shown in templates
- `@Valid` + `BindingResult` on form submissions display inline errors without losing form state

---

### Sub-Task 5 — Thymeleaf Templates & Layout

**Status:** `[ ] pending`

**Intent**  
Build all HTML pages using Thymeleaf + Bootstrap 5. A shared layout fragment (navbar + footer) is used by every page to avoid duplication.

**Expected Outcomes**
- All pages render correctly in a browser
- Responsive on mobile (Bootstrap grid)
- Consistent navigation with active-link highlighting
- Flash messages (success/error) shown in a dismissable Bootstrap alert

**Todo List**
1. Create `src/main/resources/templates/fragments/layout.html`:
   - Bootstrap 5 CSS/JS via CDN
   - Chart.js via CDN (for stats page)
   - Navbar with links: Dashboard, Habits
   - `th:fragment="layout(title, content)"` wrapping pattern OR use Thymeleaf Layout Dialect — use the **simple fragment approach** (no extra dependency): each template includes header/footer fragments and defines its own `<main>` block

2. Create `src/main/resources/templates/fragments/navbar.html` and `fragments/flash.html`

3. **Dashboard** (`templates/dashboard.html`):
   - Today's date heading
   - Card grid: one Bootstrap card per habit, showing name, icon, color stripe, current streak badge
   - Check button (`POST /track/{id}`) toggling completion; button state (checked/unchecked) reflects today's status
   - Summary row: total habits, completed today count

4. **Habit List** (`templates/habits/list.html`):
   - Table with name, icon, color swatch, created date, streak, actions (Edit / Stats / Delete)

5. **Habit Form** (`templates/habits/form.html`):
   - Bootstrap form: name, description, colour picker (`<input type="color">`), icon (text field)
   - Inline validation error display with `th:errors`

6. **Statistics** (`templates/habits/stats.html`):
   - Summary cards: current streak, longest streak, total completions, 30-day rate
   - Bar chart (Chart.js) showing daily completion for the last 30 days (labels = dates, data = 0/1)
   - Badge shelf: display earned badges as Bootstrap badge pills

7. **Calendar** (`templates/habits/calendar.html`):
   - Month grid (7-column table) for the current month
   - Each day cell coloured green (completed) / light-grey (missed) / white (future/no data)
   - Prev/Next month navigation via `GET /habits/{id}/calendar?year=&month=`

**Relevant Context**
- Thymeleaf `th:each`, `th:if`, `th:classappend` are the main directives needed
- Chart.js data is passed as JSON strings in `th:attr="data-labels=..."` or inline `<script>` blocks rendered by Thymeleaf
- Calendar month navigation requires `year` and `month` request params in `HabitController`

---

### Sub-Task 6 — Static Assets & Visual Polish

**Status:** `[ ] pending`

**Intent**  
Add a small custom CSS file and any JS needed beyond CDN libraries. Make the tracker visually appealing without over-engineering.

**Expected Outcomes**
- Custom colour variables and card hover effects
- Habit colour stripe visible on dashboard cards
- Badge icons look distinct for each badge type
- No broken styles or console errors on any page

**Todo List**
1. Create `src/main/resources/static/css/app.css`:
   - CSS custom properties for primary colour
   - `.habit-card` with left border coloured by habit's `color` field (use inline `style` in template)
   - `.streak-badge` pill style
   - `.calendar-day` cell sizing + `.completed` / `.missed` / `.future` colour classes
   - `.badge-shelf` flex row with badge pill styles per type
2. Create `src/main/resources/static/js/app.js`:
   - Minimal: auto-dismiss flash messages after 4 s
   - Confirm dialog before habit deletion
3. Update all templates to reference `/css/app.css` and `/js/app.js`
4. Add Bootstrap Icons CDN link in the layout for icon rendering (e.g. `bi-check-circle`, `bi-fire`)

**Relevant Context**
- Habit colour is stored as a hex string (`#4CAF50`) — use it in `style="border-left: 4px solid [[${habit.color}]]"` in Thymeleaf

---

### Sub-Task 7 — Sample Data & Integration Smoke Test

**Status:** `[ ] pending`

**Intent**  
Seed a small set of sample habits and logs so the app is immediately useful when first launched, and manually verify all pages work end-to-end.

**Expected Outcomes**
- On first startup three sample habits are present with some historical logs
- Dashboard shows completions and streaks correctly
- All pages navigate without 500 errors
- Calendar correctly highlights past completed days

**Todo List**
1. Create `com.habittracker.DataInitializer` (`@Component`, implements `CommandLineRunner`):
   - Only seeds if `HabitRepository.count() == 0` (idempotent)
   - Insert 3 habits: "Morning Exercise", "Read 30 minutes", "Drink 8 glasses of water"
   - Insert `HabitLog` entries for each habit for the past 14 days (varied completion to produce realistic streaks and stats)
   - Save via `HabitService.save` and direct `HabitLogRepository.save` calls
2. Start the app (`mvn spring-boot:run`) and manually navigate:
   - `/` — dashboard loads, check-off buttons work
   - `/habits` — list shows 3 habits
   - `/habits/new` — form submits, new habit appears
   - `/habits/{id}/stats` — chart renders, badges appear if earned
   - `/habits/{id}/calendar` — month grid coloured correctly, prev/next navigation works
3. Fix any issues found during smoke test

**Relevant Context**
- `DataInitializer` must run after the schema is created — `CommandLineRunner` order after Hibernate init is guaranteed by Spring Boot
- Sample logs should include a 7-day streak for at least one habit so the `STREAK_7` badge is awarded on startup

---

## Architecture Summary

```
com.habittracker/
├── HabitTrackerApplication.java
├── DataInitializer.java
├── controller/
│   ├── HomeController.java
│   ├── HabitController.java
│   └── TrackingController.java
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
    └── HabitService.java

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
