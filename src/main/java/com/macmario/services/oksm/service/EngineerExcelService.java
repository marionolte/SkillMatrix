package com.macmario.services.oksm.service;

import com.macmario.services.oksm.model.*;
import com.macmario.services.oksm.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class EngineerExcelService {

    @Autowired private EngineerSkillRepository engineerSkillRepository;
    @Autowired private EngineerSubSkillRepository engineerSubSkillRepository;
    @Autowired private SubSkillRepository subSkillRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private UserRepository userRepository;

    // ── Export ───────────────────────────────────────────────────
    public byte[] exportMySkills(Long engineerId) throws Exception {
        User engineer = userRepository.findById(engineerId).orElseThrow();
        List<Skill> allSkills = skillRepository.findAllActiveWithCategory();
        List<EngineerSkill> mySkills = engineerSkillRepository.findByEngineerIdWithDetails(engineerId);
        List<EngineerSubSkill> mySubSkills = engineerSubSkillRepository.findByEngineerIdWithDetails(engineerId);

        Map<Long, EngineerSkill> skillLookup = new HashMap<>();
        for (EngineerSkill es : mySkills) {
            skillLookup.put(es.getSkill().getId(), es);
        }
        Map<Long, EngineerSubSkill> subSkillLookup = new HashMap<>();
        for (EngineerSubSkill ess : mySubSkills) {
            subSkillLookup.put(ess.getSubSkill().getId(), ess);
        }

        Map<String, List<Skill>> byCategory = new LinkedHashMap<>();
        for (Skill skill : allSkills) {
            String cat = skill.getCategory() != null ? skill.getCategory().getName() : "Sonstiges";
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(skill);
        }

        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            // ── Styles ────────────────────────────────────────────
            XSSFCellStyle titleStyle = titleStyle(wb);
            XSSFCellStyle headerStyle = headerStyle(wb);
            XSSFCellStyle catStyle = catStyle(wb);
            XSSFCellStyle expertStyle = levelStyle(wb, new byte[]{(byte)22,(byte)134,(byte)58});
            XSSFCellStyle readyStyle  = levelStyle(wb, new byte[]{(byte)37,(byte)99,(byte)235});
            XSSFCellStyle assocStyle  = levelStyle(wb, new byte[]{(byte)217,(byte)119,(byte)6});
            XSSFCellStyle geplantStyle= levelStyle(wb, new byte[]{(byte)100,(byte)116,(byte)139});
            XSSFCellStyle unknownStyle = unknownStyle(wb);
            XSSFCellStyle subStyle    = subSkillRowStyle(wb);
            XSSFCellStyle normalStyle = normalStyle(wb);

            // ── Sheet 1: My Skills ────────────────────────────────
            XSSFSheet sheet = wb.createSheet("Meine Skills");
            sheet.setColumnWidth(0, 6000);   // Type
            sheet.setColumnWidth(1, 7000);   // Category
            sheet.setColumnWidth(2, 8000);   // Skill
            sheet.setColumnWidth(3, 6000);   // SubSkill
            sheet.setColumnWidth(4, 4500);   // Level
            sheet.setColumnWidth(5, 10000);  // Notes
            sheet.setColumnWidth(6, 5000);   // Updated

            int row = 0;

            // Title
            Row titleRow = sheet.createRow(row++);
            titleRow.setHeightInPoints(34);
            Cell tc = titleRow.createCell(0);
            tc.setCellValue("Skill-Profil: " + engineer.getFullName()
                + "  |  Export: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            tc.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            row++; // blank

            // Column headers
            Row hdr = sheet.createRow(row++);
            hdr.setHeightInPoints(22);
            String[] cols = {"Typ", "Kategorie", "Skill", "SubSkill", "Level", "Notizen", "Geändert"};
            for (int i = 0; i < cols.length; i++) {
                Cell c = hdr.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            for (Map.Entry<String, List<Skill>> entry : byCategory.entrySet()) {
                // Category separator row
                Row catRow = sheet.createRow(row++);
                catRow.setHeightInPoints(18);
                Cell cc = catRow.createCell(0);
                cc.setCellValue(entry.getKey());
                cc.setCellStyle(catStyle);
                sheet.addMergedRegion(new CellRangeAddress(catRow.getRowNum(), catRow.getRowNum(), 0, 6));

                for (Skill skill : entry.getValue()) {
                    EngineerSkill es = skillLookup.get(skill.getId());

                    // Skill row
                    Row skillRow = sheet.createRow(row++);
                    skillRow.setHeightInPoints(18);
                    skillRow.createCell(0).setCellValue("Skill");
                    skillRow.getCell(0).setCellStyle(normalStyle);
                    Cell catCell = skillRow.createCell(1);
                    catCell.setCellValue(skill.getCategory() != null ? skill.getCategory().getName() : "");
                    catCell.setCellStyle(normalStyle);
                    Cell nameCell = skillRow.createCell(2);
                    nameCell.setCellValue(skill.getName());
                    nameCell.setCellStyle(normalStyle);
                    skillRow.createCell(3).setCellStyle(normalStyle);
                    Cell lvlCell = skillRow.createCell(4);
                    Cell notesCell = skillRow.createCell(5);
                    Cell updCell = skillRow.createCell(6);
                    if (es != null) {
                        lvlCell.setCellValue(es.getLevel().getLabel());
                        lvlCell.setCellStyle(levelCellStyle(wb, es.getLevel(), expertStyle, readyStyle, assocStyle, geplantStyle));
                        notesCell.setCellValue(es.getNotes() != null ? es.getNotes() : "");
                        updCell.setCellValue(es.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")));
                    } else {
                        lvlCell.setCellValue("UNKNOWN");
                        lvlCell.setCellStyle(unknownStyle);
                        notesCell.setCellValue("");
                        updCell.setCellValue("");
                    }
                    notesCell.setCellStyle(normalStyle);
                    updCell.setCellStyle(normalStyle);

                    // SubSkill rows
                    for (SubSkill subSkill : skill.getSubSkills()) {
                        if (!subSkill.isActive()) continue;
                        EngineerSubSkill ess = subSkillLookup.get(subSkill.getId());

                        Row subRow = sheet.createRow(row++);
                        subRow.setHeightInPoints(16);
                        Cell st = subRow.createCell(0); st.setCellValue("SubSkill"); st.setCellStyle(subStyle);
                        Cell sc = subRow.createCell(1); sc.setCellValue(""); sc.setCellStyle(subStyle);
                        Cell sn = subRow.createCell(2); sn.setCellValue("  └ " + skill.getName()); sn.setCellStyle(subStyle);
                        Cell ss = subRow.createCell(3); ss.setCellValue(subSkill.getName()); ss.setCellStyle(subStyle);
                        Cell sl = subRow.createCell(4);
                        Cell sno = subRow.createCell(5);
                        Cell sud = subRow.createCell(6);
                        if (ess != null) {
                            sl.setCellValue(ess.getLevel().getLabel());
                            sl.setCellStyle(levelCellStyle(wb, ess.getLevel(), expertStyle, readyStyle, assocStyle, geplantStyle));
                            sno.setCellValue(ess.getNotes() != null ? ess.getNotes() : "");
                            sud.setCellValue(ess.getUpdatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")));
                        } else {
                            sl.setCellValue("UNKNOWN");
                            sl.setCellStyle(unknownStyle);
                            sno.setCellValue("");
                            sud.setCellValue("");
                        }
                        sno.setCellStyle(subStyle);
                        sud.setCellStyle(subStyle);
                    }
                }
                row++; // spacer between categories
            }

            // ── Sheet 2: Import-Format (pre-filled) ──────────────
            XSSFSheet imp = wb.createSheet("Import");
            imp.setColumnWidth(0, 5000);
            imp.setColumnWidth(1, 6000);
            imp.setColumnWidth(2, 4500);
            imp.setColumnWidth(3, 10000);

            Row ih = imp.createRow(0);
            ih.setHeightInPoints(22);
            String[] impCols = {"Category", "Skill", "SubSkill", "Description", engineer.getUsername()};
            for (int i = 0; i < impCols.length; i++) {
                Cell c = ih.createCell(i);
                c.setCellValue(impCols[i]);
                c.setCellStyle(headerStyle);
            }
            imp.setColumnWidth(4, 3800);

            int ir = 1;
            for (Skill skill : allSkills) {
                EngineerSkill es = skillLookup.get(skill.getId());
                Row r = imp.createRow(ir++);
                r.createCell(0).setCellValue(skill.getCategory() != null ? skill.getCategory().getName() : "");
                r.createCell(1).setCellValue(skill.getName());
                r.createCell(2).setCellValue("");
                r.createCell(3).setCellValue(skill.getDescription() != null ? skill.getDescription() : "");
                r.createCell(4).setCellValue(es != null ? es.getLevel().name() : "UNKNOWN");

                for (SubSkill subSkill : skill.getSubSkills()) {
                    if (!subSkill.isActive()) continue;
                    EngineerSubSkill ess = subSkillLookup.get(subSkill.getId());
                    Row sr = imp.createRow(ir++);
                    sr.createCell(0).setCellValue(skill.getCategory() != null ? skill.getCategory().getName() : "");
                    sr.createCell(1).setCellValue(skill.getName());
                    sr.createCell(2).setCellValue(subSkill.getName());
                    sr.createCell(3).setCellValue("");
                    sr.createCell(4).setCellValue(ess != null ? ess.getLevel().name() : "UNKNOWN");
                }
            }

            // Dropdown for level column on Import sheet
            if (ir > 1) {
                DataValidationHelper dvHelper = imp.getDataValidationHelper();
                DataValidationConstraint dvConstraint = dvHelper.createExplicitListConstraint(
                    new String[]{"EXPERT", "READY", "ASSOCIATE", "GEPLANT", "UNKNOWN"});
                DataValidation dv = dvHelper.createValidation(
                    dvConstraint, new CellRangeAddressList(1, ir - 1, 4, 4));
                dv.setSuppressDropDownArrow(false);
                dv.setShowErrorBox(true);
                dv.setErrorStyle(DataValidation.ErrorStyle.STOP);
                dv.createErrorBox("Ungültiger Wert", "Bitte einen Wert aus der Liste wählen.");
                imp.addValidationData(dv);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ── Import (self-service) ────────────────────────────────────
    public record ImportResult(int rows, int created, int updated, List<String> errors) {}

    public ImportResult importMySkills(Long engineerId, MultipartFile file) throws Exception {
        User engineer = userRepository.findById(engineerId).orElseThrow();
        List<String> errors = new ArrayList<>();
        int rows = 0, created = 0, updated = 0;

        try (InputStream is = file.getInputStream();
             XSSFWorkbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheet("Import");
            if (sheet == null) sheet = wb.getSheetAt(0);

            // Validate that column E header matches the engineer's own username
            Row header = sheet.getRow(0);
            if (header == null) throw new RuntimeException("Leere Datei.");

            // Find own column — must match username
            int ownCol = -1;
            for (int c = 4; c <= header.getLastCellNum(); c++) {
                Cell cell = header.getCell(c);
                if (cell != null && str(cell).trim().equalsIgnoreCase(engineer.getUsername())) {
                    ownCol = c;
                    break;
                }
            }
            if (ownCol == -1) {
                // Accept the first data column if header is ambiguous
                ownCol = 4;
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String skillName = str(row.getCell(1)).trim();
                if (skillName.isBlank()) continue;
                rows++;

                String catName  = str(row.getCell(0)).trim();
                String subName  = str(row.getCell(2)).trim();
                String levelStr = str(row.getCell(ownCol)).trim().toUpperCase();

                if (levelStr.isBlank() || levelStr.equals("-") || levelStr.equals("—") || levelStr.equals("UNKNOWN")) continue;

                EngineerSkill.SkillLevel level;
                try {
                    level = EngineerSkill.SkillLevel.valueOf(levelStr);
                } catch (IllegalArgumentException ex) {
                    errors.add("Zeile " + (r + 1) + ": Unbekanntes Level '" + levelStr + "'");
                    continue;
                }

                // Find skill by name (must already exist — engineer cannot create new skills)
                final String finalSkillName = skillName;
                Optional<Skill> skillOpt = skillRepository.findAllActiveWithCategory().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(finalSkillName))
                    .findFirst();

                if (skillOpt.isEmpty()) {
                    errors.add("Zeile " + (r + 1) + ": Skill '" + skillName + "' nicht gefunden. Bitte Admin kontaktieren.");
                    continue;
                }
                Skill skill = skillOpt.get();
                final EngineerSkill.SkillLevel finalLevel = level;

                if (subName.isBlank()) {
                    // Top-level skill
                    boolean exists = engineerSkillRepository
                        .findByEngineerIdAndSkillId(engineerId, skill.getId()).isPresent();
                    engineerSkillRepository.findByEngineerIdAndSkillId(engineerId, skill.getId())
                        .ifPresentOrElse(
                            es -> { es.setLevel(finalLevel); engineerSkillRepository.save(es); },
                            () -> engineerSkillRepository.save(new EngineerSkill(engineer, skill, finalLevel))
                        );
                    if (exists) updated++; else created++;
                } else {
                    // SubSkill
                    final String finalSubName = subName;
                    Optional<SubSkill> ssOpt = subSkillRepository
                        .findBySkillIdAndName(skill.getId(), finalSubName);
                    if (ssOpt.isEmpty()) {
                        errors.add("Zeile " + (r + 1) + ": SubSkill '" + subName + "' für '" + skillName + "' nicht gefunden.");
                        continue;
                    }
                    SubSkill ss = ssOpt.get();
                    boolean exists = engineerSubSkillRepository
                        .findByEngineerIdAndSubSkillId(engineerId, ss.getId()).isPresent();
                    engineerSubSkillRepository.findByEngineerIdAndSubSkillId(engineerId, ss.getId())
                        .ifPresentOrElse(
                            ess -> { ess.setLevel(finalLevel); engineerSubSkillRepository.save(ess); },
                            () -> engineerSubSkillRepository.save(new EngineerSubSkill(engineer, ss, finalLevel))
                        );
                    if (exists) updated++; else created++;
                }
            }
        }
        return new ImportResult(rows, created, updated, errors);
    }

    // ── Style helpers ─────────────────────────────────────────────
    private XSSFCellStyle titleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)15,(byte)23,(byte)42}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        f.setBold(true); f.setFontHeightInPoints((short)13);
        s.setFont(f);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private XSSFCellStyle headerStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)30,(byte)41,(byte)59}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        f.setBold(true); f.setFontHeightInPoints((short)10);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private XSSFCellStyle catStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)79,(byte)70,(byte)229}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        f.setBold(true); f.setFontHeightInPoints((short)10);
        s.setFont(f);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private XSSFCellStyle levelStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(rgb, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        f.setBold(true); f.setFontHeightInPoints((short)9);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private XSSFCellStyle unknownStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)203,(byte)213,(byte)225}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)100,(byte)116,(byte)139}, null));
        f.setFontHeightInPoints((short)9);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setBorderBottom(BorderStyle.THIN);
        return s;
    }

    private XSSFCellStyle subSkillRowStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)248,(byte)250,(byte)252}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setItalic(true); f.setFontHeightInPoints((short)9);
        f.setColor(new XSSFColor(new byte[]{(byte)71,(byte)85,(byte)105}, null));
        s.setFont(f);
        s.setBorderBottom(BorderStyle.DOTTED);
        return s;
    }

    private XSSFCellStyle normalStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setBorderBottom(BorderStyle.THIN);
        s.setBottomBorderColor(new XSSFColor(new byte[]{(byte)226,(byte)232,(byte)240}, null));
        return s;
    }

    private XSSFCellStyle levelCellStyle(XSSFWorkbook wb, EngineerSkill.SkillLevel level,
                                          XSSFCellStyle expert, XSSFCellStyle ready,
                                          XSSFCellStyle assoc, XSSFCellStyle geplant) {
        return switch (level) {
            case EXPERT    -> expert;
            case READY     -> ready;
            case ASSOCIATE -> assoc;
            case GEPLANT   -> geplant;
        };
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
