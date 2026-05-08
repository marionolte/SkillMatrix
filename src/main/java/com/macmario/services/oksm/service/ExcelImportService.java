package com.macmario.services.oksm.service;

import com.macmario.services.oksm.model.*;
import com.macmario.services.oksm.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

/**
 * Imports skills, subskills and engineer skill-levels from an Excel file.
 *
 * Expected sheet layout (sheet name "Import"):
 *
 * Row 1 (header): Category | Skill | SubSkill | Description | Engineer1 | Engineer2 | ...
 * Row 2+:         Backend  | Java  | Install  | Desc        | EXPERT    | READY     | ...
 *
 * If SubSkill is empty, the row sets the top-level EngineerSkill level.
 * If SubSkill has a value, the row sets the EngineerSubSkill level.
 *
 * Level values accepted (case-insensitive): EXPERT, READY, ASSOCIATE, GEPLANT (or blank = skip)
 */
@Service
@Transactional
public class ExcelImportService {

    @Autowired private SkillCategoryRepository categoryRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private SubSkillRepository subSkillRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EngineerSkillRepository engineerSkillRepository;
    @Autowired private EngineerSubSkillRepository engineerSubSkillRepository;

    public record ImportResult(int rows, int created, int updated, List<String> errors) {}

    public ImportResult importFromExcel(MultipartFile file) throws Exception {
        List<String> errors = new ArrayList<>();
        int rows = 0, created = 0, updated = 0;

        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheet("Import");
            if (sheet == null) sheet = wb.getSheetAt(0);

            // ── Parse header row to find engineer columns ────────────────
            Row header = sheet.getRow(0);
            if (header == null) throw new RuntimeException("Leere Datei");

            // Columns 0..3 fixed: Category, Skill, SubSkill, Description
            // Columns 4+ are engineer usernames
            Map<Integer, User> colToEngineer = new LinkedHashMap<>();
            for (int col = 4; col <= header.getLastCellNum(); col++) {
                Cell c = header.getCell(col);
                if (c == null) continue;
                String username = str(c).trim();
                if (username.isBlank()) continue;
                final int finalCol = col;
                userRepository.findByUsername(username)
                    .ifPresentOrElse(
                        u -> colToEngineer.put(finalCol, u),
                        () -> errors.add("Benutzer nicht gefunden: " + username)
                    );
            }

            if (colToEngineer.isEmpty()) {
                errors.add("Keine Engineer-Spalten gefunden (ab Spalte E).");
                return new ImportResult(0, 0, 0, errors);
            }

            // ── Process data rows ────────────────────────────────────────
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String catName    = str(row.getCell(0)).trim();
                String skillName  = str(row.getCell(1)).trim();
                String subName    = str(row.getCell(2)).trim();
                String desc       = str(row.getCell(3)).trim();

                if (skillName.isBlank()) continue;
                rows++;

                // Upsert category
                SkillCategory category = null;
                if (!catName.isBlank()) {
                    String finalCatName = catName;
                    category = categoryRepository.findAllByOrderByNameAsc().stream()
                        .filter(c -> c.getName().equalsIgnoreCase(finalCatName))
                        .findFirst()
                        .orElseGet(() -> categoryRepository.save(
                            new SkillCategory(finalCatName, "", "#6366F1", "🔧")));
                }

                // Upsert skill
                SkillCategory finalCategory = category;
                String finalSkillName = skillName;
                Skill skill = skillRepository.findAllActiveWithCategory().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(finalSkillName))
                    .findFirst()
                    .orElseGet(() -> {
                        Skill ns = new Skill(finalSkillName, desc, finalCategory);
                        return skillRepository.save(ns);
                    });

                SubSkill subSkill = null;
                if (!subName.isBlank()) {
                    String finalSubName = subName;
                    Skill finalSkill = skill;
                    subSkill = subSkillRepository.findBySkillIdAndName(skill.getId(), subName)
                        .orElseGet(() -> {
                            int order = (int) subSkillRepository
                                .findBySkillIdOrderByDisplayOrderAsc(finalSkill.getId()).stream().count();
                            return subSkillRepository.save(new SubSkill(finalSkill, finalSubName, desc, order));
                        });
                }

                // Set levels for each engineer column
                for (Map.Entry<Integer, User> e : colToEngineer.entrySet()) {
                    Cell cell = row.getCell(e.getKey());
                    if (cell == null) continue;
                    String levelStr = str(cell).trim().toUpperCase();
                    if (levelStr.isBlank() || levelStr.equals("-") || levelStr.equals("—")) continue;

                    EngineerSkill.SkillLevel parsedLevel;
                    try {
                        parsedLevel = EngineerSkill.SkillLevel.valueOf(levelStr);
                    } catch (IllegalArgumentException ex) {
                        errors.add("Zeile " + (r + 1) + ": Unbekanntes Level '" + levelStr + "'");
                        continue;
                    }
                    // Now effectively final — safe to use in lambdas
                    final EngineerSkill.SkillLevel level = parsedLevel;

                    User engineer = e.getValue();
                    if (subSkill != null) {
                        // Sub-skill level
                        final SubSkill finalSubSkill = subSkill;
                        Long essId = finalSubSkill.getId();
                        boolean exists = engineerSubSkillRepository
                            .findByEngineerIdAndSubSkillId(engineer.getId(), essId).isPresent();
                        engineerSubSkillRepository.findByEngineerIdAndSubSkillId(engineer.getId(), essId)
                            .ifPresentOrElse(
                                ess -> { ess.setLevel(level); engineerSubSkillRepository.save(ess); },
                                () -> engineerSubSkillRepository.save(
                                    new EngineerSubSkill(engineer, finalSubSkill, level))
                            );
                        if (exists) updated++; else created++;
                    } else {
                        // Top-level skill
                        final Skill finalSkill2 = skill;
                        boolean exists = engineerSkillRepository
                            .findByEngineerIdAndSkillId(engineer.getId(), finalSkill2.getId()).isPresent();
                        engineerSkillRepository.findByEngineerIdAndSkillId(engineer.getId(), finalSkill2.getId())
                            .ifPresentOrElse(
                                es -> { es.setLevel(level); engineerSkillRepository.save(es); },
                                () -> engineerSkillRepository.save(
                                    new EngineerSkill(engineer, finalSkill2, level))
                            );
                        if (exists) updated++; else created++;
                    }
                }
            }
        }
        return new ImportResult(rows, created, updated, errors);
    }

    public byte[] generateImportTemplate(List<User> engineers) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Import");
            sheet.setDefaultColumnWidth(16);

            var headerStyle = wb.createCellStyle();
            var font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            var exampleStyle = wb.createCellStyle();
            exampleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            exampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            var hrow = sheet.createRow(0);
            String[] fixed = {"Category", "Skill", "SubSkill", "Description"};
            for (int i = 0; i < fixed.length; i++) {
                var c = hrow.createCell(i);
                c.setCellValue(fixed[i]);
                c.setCellStyle(headerStyle);
            }
            int col = 4;
            for (User eng : engineers) {
                var c = hrow.createCell(col++);
                c.setCellValue(eng.getUsername());
                c.setCellStyle(headerStyle);
            }

            // Example rows
            String[][] examples = {
                {"Backend", "Java", "",          "Java 17+",          "EXPERT", "READY",  "ASSOCIATE", ""},
                {"Backend", "Java", "Install",   "Java Installation", "EXPERT", "READY",  "",          "GEPLANT"},
                {"Backend", "Java", "Upgrade",   "Java Upgrades",     "READY",  "",       "ASSOCIATE", ""},
                {"DevOps",  "Docker", "",        "Container",         "",       "EXPERT", "READY",     "ASSOCIATE"},
                {"DevOps",  "Docker", "Install", "Docker setup",      "",       "EXPERT", "READY",     ""},
            };

            int r = 1;
            for (String[] ex : examples) {
                var row = sheet.createRow(r++);
                for (int c2 = 0; c2 < ex.length && c2 < (4 + engineers.size()); c2++) {
                    var cell = row.createCell(c2);
                    cell.setCellValue(ex[c2]);
                    cell.setCellStyle(exampleStyle);
                }
            }

            // Info sheet
            var info = wb.createSheet("Anleitung");
            info.setDefaultColumnWidth(60);
            String[] lines = {
                "EXCEL IMPORT ANLEITUNG",
                "",
                "Tabellenblatt 'Import' ausfüllen:",
                "Spalte A: Category (wird angelegt falls nicht vorhanden)",
                "Spalte B: Skill-Name (wird angelegt falls nicht vorhanden)",
                "Spalte C: SubSkill-Name (optional - leer lassen für Haupt-Skill)",
                "Spalte D: Beschreibung (optional)",
                "Spalten E+: Benutzername des Engineers als Überschrift, Level als Wert",
                "",
                "Gültige Level-Werte:",
                "  EXPERT    - vollständige Expertise",
                "  READY     - produktiv einsetzbar",
                "  ASSOCIATE - Grundkenntnisse",
                "  GEPLANT   - geplante Weiterbildung",
                "  (leer)    - kein Eintrag / überspringen",
            };
            for (int i = 0; i < lines.length; i++) {
                var row = info.createRow(i);
                row.createCell(0).setCellValue(lines[i]);
            }

            var out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private String str(Cell c) {
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING  -> c.getStringCellValue();
            case NUMERIC -> String.valueOf((long) c.getNumericCellValue());
            case BOOLEAN -> String.valueOf(c.getBooleanCellValue());
            default -> "";
        };
    }
}
