# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SkillMatrix (OKSM)** is an Online Skill Management platform built with Spring Boot and Thymeleaf. It enables organizations to:
- Track engineer/consultant skills and proficiency levels (Expert, Ready, Associate, Planned)
- Manage skill hierarchies (Skills → SubSkills, e.g., Docker has Install/Admin/Migrate/Upgrade subskills)
- Analyze skill gaps against product requirements
- Organize engineers into service teams
- Export skill matrices to Excel

The application uses **role-based access control** (ENGINEER, MANAGER, ADMIN) with Spring Security, a **H2 in-file database**, and **Thymeleaf** for server-rendered HTML.

## Build & Run Commands

### Build
```bash
# Full Maven build (creates WAR file at target/oksm.war)
mvn clean package

# Skip tests during build
mvn clean package -DskipTests

# Just compile without packaging
mvn clean compile
```

### Run
```bash
# Development: Run as a Spring Boot application directly
mvn spring-boot:run

# After building, can deploy oksm.war to a Tomcat server
# Or run the JAR (if packaging is changed to jar instead of war)
java -jar target/oksm.war
```

The application starts on **http://localhost:8080** with H2 database auto-initialized. Default credentials from `DataInitializer`:
- Admin: `admin` / `admin123`
- Manager: `manager` / `manager123`  
- Engineer: `max.mueller` / `pass123`

### Test
No test files currently exist in the codebase. The data initializer (`DataInitializer.java`) seeds sample data on first run.

## Architecture & Key Components

### Layered Architecture

**1. Controllers** (`src/main/java/.../controller/`)
- **DashboardController**: Routes to role-specific dashboards (Engineer vs. Manager/Admin views)
- **EngineerController**: Engineer profile/skill management
- **ManagerController**: Manager/Admin views (skill matrix, gap analysis, Excel export/import)
- **AdminController**: User/skill management (create, update, toggle active status)
- **AuthController**: Login form (form-based Spring Security)
- **ProductController**, **ServiceTeamController**: Team and product CRUD operations
- **ImportController**: Excel file upload for bulk skill data

**2. Services** (`src/main/java/.../service/`)
- **SkillService**: Core business logic for skills, categories, engineer-skill associations, and gap analysis (calculates coverage against required skills)
- **ExcelExportService**: Generates skill matrix as colored Excel workbook (XSSF)
- **ExcelImportService**: Parses uploaded Excel files to bulk-update engineer skills
- **EngineerExcelService**: Specialized Excel operations for engineer data
- **UserService**: User CRUD and authentication helpers
- **ProductService**, **ServiceTeamService**: Entity management
- **UserDetailsServiceImpl**: Spring Security integration (loads users for authentication)

**3. Repositories** (`src/main/java/.../repository/`)
- JPA repositories extending `CrudRepository` for data access:
  - `UserRepository`: Custom query `findActiveEngineers()`, `findByUsername()`
  - `SkillRepository`: `findAllActiveWithCategory()`, `findByCategoryId()`
  - `EngineerSkillRepository`: Core pivot table (User↔Skill with SkillLevel); custom queries for bulk lookups
  - `EngineerSubSkillRepository`: SubSkill proficiency tracking
  - `SkillCategoryRepository`, `ProductRepository`, `ServiceTeamRepository`, `SubSkillRepository`

**4. Models** (`src/main/java/.../model/`)
- **User**: Represents engineers, managers, admins; has `consultant` flag for external consultants; maintains relationships to EngineerSkill
- **Skill**: Technology/skill name; belongs to a SkillCategory; has ordered SubSkills; many-to-many with Products (required skills)
- **SkillCategory**: Groups skills (Backend, Frontend, DevOps, Data, Security); each has a color and emoji icon
- **SubSkill**: Granular task within a Skill (e.g., Docker has "Install", "Administrate", "Migrate", "Upgrade")
- **EngineerSkill**: Join entity mapping User→Skill with a `SkillLevel` enum (EXPERT/READY/ASSOCIATE/GEPLANT) and optional notes
- **EngineerSubSkill**: Similar join for SubSkill proficiency
- **ServiceTeam**: Named team with manager and set of member users; used for team-level organization
- **Product**: Named product/project with description and a set of requiredSkills

**5. Configuration** (`src/main/java/.../config/`)
- **SecurityConfig**: Spring Security configuration (role-based URL patterns, form login, session management, H2 console access for ADMIN)
- **DataInitializer**: Runs on startup if database is empty; seeds 5 demo users, 5 skill categories, 20+ skills, sample engineer-skill assignments, and 4 products

**6. Views & Styling** (`src/main/resources/`)
- **templates/layout.html**: Master Thymeleaf fragment with sidebar navigation (role-aware)
- **templates/{engineer,manager,admin,auth,profile,teams}/*.html**: Role-specific page templates
- **static/css/main.css**: Single stylesheet with Tailwind-like utility classes and custom component styles
- **static/js/main.js**: Minimal client-side logic (likely form interactions, modals)

### Data Model Relationships
```
User (1) ──has many──> (M) EngineerSkill ──belongs to──> (1) Skill
  ├─ many-to-many ──> ServiceTeam (members)
  └─ one-to-many ──> EngineerSubSkill ──belongs to──> (1) SubSkill

Skill (1) ──belongs to──> (1) SkillCategory
  ├─ many-to-many ──> Product (requiredSkills)
  └─ one-to-many ──> SubSkill

ServiceTeam (1) ──has one──> User (manager)
Product (1) ──has many──> Skill (requiredSkills)
```

### Key Business Logic
- **Skill Levels**: SkillLevel enum with numeric order (1=Geplant, 2=Associate, 3=Ready, 4=Expert) for gap analysis comparisons
- **Gap Analysis**: `SkillService.analyzeGaps()` compares engineers' proficiency against a set of required skills; returns coverage percentage and gap list
- **Skill Matrix**: Tabular view of all active engineers vs. all active skills; Excel export for reporting

## Configuration & Database

- **Database**: H2 in-file at `./data/skillmatrix` (auto-created on first run)
- **Credentials**: `sa` / `skillmatrix2024` (hardcoded; development-only)
- **H2 Console**: Available at `/h2-console` (ADMIN-only) for debugging
- **Hibernate**: `spring.jpa.hibernate.ddl-auto=update` (auto-creates/updates schema)
- **JPA**: Jakarta Persistence API (new namespace as of Spring Boot 3.2.x)
- **ORM Annotations**: Lombok `@Data` for getters/setters; `@NoArgsConstructor` for JPA

## Key Entry Points

1. **Main Application Class**: `SkillMatrixApplication.java` — extends `SpringBootServletInitializer` for WAR deployment
2. **Start Page**: `/login` (redirects to `/dashboard` after authentication)
3. **Role-Based Routing**: `DashboardController` dispatches to engineer or manager dashboard based on `User.getRole()`

## Important Implementation Details

- **Excel Export**: Uses Apache POI (XSSF) to generate colored, formatted Excel workbooks with headers and skill level styling
- **Excel Import**: Custom parser to extract engineer names and skill proficiencies from uploaded files
- **Security**: Form-based login with BCrypt password encoding; session limit of 3 concurrent sessions per user
- **CSRF Protection**: Enabled (forms use hidden tokens); H2 console access requires CSRF exemption
- **Transactional**: Services marked `@Transactional` for consistent state during multi-entity operations

## Development Notes

- **No Tests**: The codebase currently has no unit or integration tests.
- **Demo Data**: `DataInitializer` seeds sample data on every startup if the database is empty.
- **Frontend**: Minimal JavaScript; most functionality is server-side Thymeleaf rendering.
