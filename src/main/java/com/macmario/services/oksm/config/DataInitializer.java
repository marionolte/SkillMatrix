package com.macmario.services.oksm.config;

import com.macmario.services.oksm.model.*;
import com.macmario.services.oksm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepo;
    @Autowired private SkillCategoryRepository catRepo;
    @Autowired private SkillRepository skillRepo;
    @Autowired private SubSkillRepository subSkillRepo;
    @Autowired private EngineerSkillRepository esRepo;
    @Autowired private EngineerSubSkillRepository essRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private ServiceTeamRepository teamRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;

        // ── Users ──────────────────────────────────────────────
        User admin = new User("admin", passwordEncoder.encode("admin123"),
            "admin@company.com", "System Admin", User.Role.ADMIN);
        userRepo.save(admin);

        User manager1 = new User("manager", passwordEncoder.encode("manager123"),
            "manager@company.com", "Anna Weber", User.Role.MANAGER);
        manager1.setDepartment("Engineering"); manager1.setTeam("Platform");
        userRepo.save(manager1);

        User manager2 = new User("m.braun", passwordEncoder.encode("manager123"),
            "m.braun@company.com", "Michael Braun", User.Role.MANAGER);
        manager2.setDepartment("Engineering"); manager2.setTeam("Integration");
        userRepo.save(manager2);

        User eng1 = new User("max.mueller", passwordEncoder.encode("pass123"),
            "max.mueller@company.com", "Max Müller", User.Role.ENGINEER);
        eng1.setDepartment("Engineering"); eng1.setTeam("Backend");
        userRepo.save(eng1);

        User eng2 = new User("sara.schmidt", passwordEncoder.encode("pass123"),
            "sara.schmidt@company.com", "Sara Schmidt", User.Role.ENGINEER);
        eng2.setDepartment("Engineering"); eng2.setTeam("Frontend");
        userRepo.save(eng2);

        User eng3 = new User("tom.klein", passwordEncoder.encode("pass123"),
            "tom.klein@company.com", "Tom Klein", User.Role.ENGINEER);
        eng3.setDepartment("Engineering"); eng3.setTeam("Backend");
        userRepo.save(eng3);

        User eng4 = new User("lisa.bauer", passwordEncoder.encode("pass123"),
            "lisa.bauer@company.com", "Lisa Bauer", User.Role.ENGINEER);
        eng4.setDepartment("Engineering"); eng4.setTeam("DevOps");
        userRepo.save(eng4);

        User consultant1 = new User("j.extern", passwordEncoder.encode("pass123"),
            "j.extern@consulting.com", "Jan Extern", User.Role.ENGINEER);
        consultant1.setConsultant(true); consultant1.setTeam("External");
        userRepo.save(consultant1);

        // ── Categories ─────────────────────────────────────────
        SkillCategory backend  = catRepo.save(new SkillCategory("Backend",  "Server-side Technologies", "#4F46E5", "⚙️"));
        SkillCategory frontend = catRepo.save(new SkillCategory("Frontend", "UI/UX Technologies",       "#0EA5E9", "🎨"));
        SkillCategory devops   = catRepo.save(new SkillCategory("DevOps",   "Cloud & Infrastructure",   "#10B981", "☁️"));
        SkillCategory data     = catRepo.save(new SkillCategory("Data",     "Data & Analytics",         "#F59E0B", "📊"));
        SkillCategory security = catRepo.save(new SkillCategory("Security", "Security & Compliance",    "#EF4444", "🔒"));

        // ── Skills + SubSkills ──────────────────────────────────
        Skill java   = skillRepo.save(new Skill("Java",      "Java 17+, Spring Boot", backend));
        Skill spring = skillRepo.save(new Skill("Spring Boot","Spring Framework",     backend));
        Skill python = skillRepo.save(new Skill("Python",    "Python 3.x",            backend));
        Skill sql    = skillRepo.save(new Skill("SQL",       "Relational databases",  backend));
        Skill node   = skillRepo.save(new Skill("Node.js",   "JS backend",            backend));

        Skill react   = skillRepo.save(new Skill("React",      "React 18+",        frontend));
        Skill angular = skillRepo.save(new Skill("Angular",    "Angular framework",frontend));
        Skill ts      = skillRepo.save(new Skill("TypeScript", "Typed JavaScript", frontend));
        Skill css     = skillRepo.save(new Skill("CSS/Tailwind","Styling",         frontend));

        Skill docker = skillRepo.save(new Skill("Docker",     "Containerization",        devops));
        Skill k8s    = skillRepo.save(new Skill("Kubernetes", "Container orchestration", devops));
        Skill cicd   = skillRepo.save(new Skill("CI/CD",      "Pipelines & automation",  devops));
        Skill aws    = skillRepo.save(new Skill("AWS",        "Amazon Web Services",     devops));

        Skill kafka  = skillRepo.save(new Skill("Kafka",      "Event streaming", data));
        Skill spark  = skillRepo.save(new Skill("Apache Spark","Big data",       data));
        Skill ml     = skillRepo.save(new Skill("ML/AI",      "Machine Learning",data));

        Skill owasp  = skillRepo.save(new Skill("OWASP",     "Security best practices", security));
        Skill oauth  = skillRepo.save(new Skill("OAuth/OIDC","Auth standards",          security));

        // SubSkills for Docker
        SubSkill dkrInstall  = subSkillRepo.save(new SubSkill(docker, "Install",     "Docker installation & setup",     0));
        SubSkill dkrAdmin    = subSkillRepo.save(new SubSkill(docker, "Administrate","Day-to-day Docker ops",           1));
        SubSkill dkrMigrate  = subSkillRepo.save(new SubSkill(docker, "Migrate",     "Migrate workloads to Docker",     2));
        SubSkill dkrUpgrade  = subSkillRepo.save(new SubSkill(docker, "Upgrade",     "Docker version upgrades",         3));

        // SubSkills for Kubernetes
        SubSkill k8sInstall  = subSkillRepo.save(new SubSkill(k8s, "Install",     "Cluster installation",   0));
        SubSkill k8sAdmin    = subSkillRepo.save(new SubSkill(k8s, "Administrate","Cluster administration",  1));
        SubSkill k8sMigrate  = subSkillRepo.save(new SubSkill(k8s, "Migrate",     "Workload migration",      2));
        SubSkill k8sUpgrade  = subSkillRepo.save(new SubSkill(k8s, "Upgrade",     "Cluster upgrades",        3));

        // SubSkills for Java
        SubSkill javaInstall = subSkillRepo.save(new SubSkill(java, "Install",     "JDK installation",   0));
        SubSkill javaUpgrade = subSkillRepo.save(new SubSkill(java, "Upgrade",     "JDK migration",      1));
        SubSkill javaBuild   = subSkillRepo.save(new SubSkill(java, "Build",       "Maven/Gradle builds",2));

        // SubSkills for SQL
        SubSkill sqlInstall  = subSkillRepo.save(new SubSkill(sql, "Install",     "DB installation",  0));
        SubSkill sqlAdmin    = subSkillRepo.save(new SubSkill(sql, "Administrate","DB administration", 1));
        SubSkill sqlMigrate  = subSkillRepo.save(new SubSkill(sql, "Migrate",     "Schema/data migration",2));

        // ── Engineer Skills ─────────────────────────────────────
        es(eng1, java,   EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng1, spring, EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng1, sql,    EngineerSkill.SkillLevel.EXPERTE);
        es(eng1, docker, EngineerSkill.SkillLevel.EXPERTE);
        es(eng1, python, EngineerSkill.SkillLevel.ANWENDER);
        es(eng1, kafka,  EngineerSkill.SkillLevel.GRUNDWISSEN);
        es(eng1, owasp,  EngineerSkill.SkillLevel.ANWENDER);

        es(eng2, react,   EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng2, ts,      EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng2, css,     EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng2, angular, EngineerSkill.SkillLevel.EXPERTE);
        es(eng2, node,    EngineerSkill.SkillLevel.EXPERTE);
        es(eng2, java,    EngineerSkill.SkillLevel.ANWENDER);

        es(eng3, java,   EngineerSkill.SkillLevel.EXPERTE);
        es(eng3, spring, EngineerSkill.SkillLevel.ANWENDER);
        es(eng3, react,  EngineerSkill.SkillLevel.EXPERTE);
        es(eng3, ts,     EngineerSkill.SkillLevel.EXPERTE);
        es(eng3, sql,    EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng3, docker, EngineerSkill.SkillLevel.ANWENDER);
        es(eng3, oauth,  EngineerSkill.SkillLevel.GRUNDWISSEN);

        es(eng4, docker, EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng4, k8s,    EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng4, cicd,   EngineerSkill.SkillLevel.SPEZIALIST);
        es(eng4, aws,    EngineerSkill.SkillLevel.EXPERTE);
        es(eng4, python, EngineerSkill.SkillLevel.EXPERTE);
        es(eng4, kafka,  EngineerSkill.SkillLevel.ANWENDER);
        es(eng4, owasp,  EngineerSkill.SkillLevel.EXPERTE);

        es(consultant1, java,   EngineerSkill.SkillLevel.SPEZIALIST);
        es(consultant1, spring, EngineerSkill.SkillLevel.EXPERTE);
        es(consultant1, aws,    EngineerSkill.SkillLevel.SPEZIALIST);
        es(consultant1, docker, EngineerSkill.SkillLevel.EXPERTE);

        // ── Engineer SubSkills ──────────────────────────────────
        ess(eng4, dkrInstall,  EngineerSkill.SkillLevel.SPEZIALIST);
        ess(eng4, dkrAdmin,    EngineerSkill.SkillLevel.SPEZIALIST);
        ess(eng4, dkrMigrate,  EngineerSkill.SkillLevel.EXPERTE);
        ess(eng4, dkrUpgrade,  EngineerSkill.SkillLevel.EXPERTE);
        ess(eng4, k8sInstall,  EngineerSkill.SkillLevel.SPEZIALIST);
        ess(eng4, k8sAdmin,    EngineerSkill.SkillLevel.SPEZIALIST);
        ess(eng4, k8sMigrate,  EngineerSkill.SkillLevel.ANWENDER);
        ess(eng4, k8sUpgrade,  EngineerSkill.SkillLevel.EXPERTE);

        ess(eng3, dkrInstall,  EngineerSkill.SkillLevel.EXPERTE);
        ess(eng3, dkrAdmin,    EngineerSkill.SkillLevel.ANWENDER);
        ess(eng1, javaInstall, EngineerSkill.SkillLevel.SPEZIALIST);
        ess(eng1, javaUpgrade, EngineerSkill.SkillLevel.EXPERTE);
        ess(eng1, javaBuild,   EngineerSkill.SkillLevel.SPEZIALIST);
        ess(eng1, sqlInstall,  EngineerSkill.SkillLevel.EXPERTE);
        ess(eng1, sqlAdmin,    EngineerSkill.SkillLevel.EXPERTE);
        ess(eng1, sqlMigrate,  EngineerSkill.SkillLevel.ANWENDER);
        ess(eng3, sqlAdmin,    EngineerSkill.SkillLevel.SPEZIALIST);
        ess(eng3, sqlMigrate,  EngineerSkill.SkillLevel.EXPERTE);

        // ── Service Teams ───────────────────────────────────────
        ServiceTeam teamPlatform = new ServiceTeam("Platform Team",   "Core platform services",      "#4F46E5");
        teamPlatform.setManager(manager1);
        teamPlatform.setMembers(Set.of(eng1, eng3));
        teamRepo.save(teamPlatform);

        ServiceTeam teamFrontend = new ServiceTeam("Frontend Squad",  "UI/UX development team",      "#0EA5E9");
        teamFrontend.setManager(manager1);
        teamFrontend.setMembers(Set.of(eng2));
        teamRepo.save(teamFrontend);

        ServiceTeam teamOps = new ServiceTeam("Ops & DevOps",         "Infrastructure & operations", "#10B981");
        teamOps.setManager(manager2);
        teamOps.setMembers(Set.of(eng4, consultant1));
        teamRepo.save(teamOps);

        ServiceTeam teamConsulting = new ServiceTeam("Consultants",   "External consultants pool",   "#F59E0B");
        teamConsulting.setMembers(Set.of(consultant1));
        teamRepo.save(teamConsulting);

        // ── Products ────────────────────────────────────────────
        Product p1 = new Product("Customer Portal",   "Main customer-facing webapp", "Anna Weber");
        p1.setRequiredSkills(Set.of(java, spring, react, ts, sql, oauth));
        productRepo.save(p1);

        Product p2 = new Product("Data Pipeline",     "Real-time data processing",   "Anna Weber");
        p2.setRequiredSkills(Set.of(kafka, spark, python, docker, k8s));
        productRepo.save(p2);

        Product p3 = new Product("DevOps Platform",  "Internal CI/CD tool",          "Michael Braun");
        p3.setRequiredSkills(Set.of(docker, k8s, cicd, aws, python));
        productRepo.save(p3);

        Product p4 = new Product("Mobile Backend API","REST API for mobile clients", "Michael Braun");
        p4.setRequiredSkills(Set.of(java, spring, sql, oauth, owasp));
        productRepo.save(p4);

        System.out.println("✅ Demo data seeded. Logins: admin/admin123  manager/manager123  max.mueller/pass123");
    }

    private void es(User u, Skill s, EngineerSkill.SkillLevel l) {
        esRepo.save(new EngineerSkill(u, s, l));
    }

    private void ess(User u, SubSkill ss, EngineerSkill.SkillLevel l) {
        essRepo.save(new EngineerSubSkill(u, ss, l));
    }
}
