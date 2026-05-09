package com.macmario.services.oksm.service;

import com.macmario.services.oksm.model.*;
import com.macmario.services.oksm.repository.EngineerSkillRepository;
import com.macmario.services.oksm.repository.EngineerSubSkillRepository;
import com.macmario.services.oksm.repository.SkillRepository;
import com.macmario.services.oksm.repository.SubSkillRepository;
import com.macmario.services.oksm.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelExportService {

    @Autowired private UserRepository userRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private EngineerSkillRepository engineerSkillRepository;
    @Autowired private SubSkillRepository subSkillRepository;
    @Autowired private EngineerSubSkillRepository engineerSubSkillRepository;

    public byte[] exportSkillMatrix() throws Exception {
        List<User> engineers = userRepository.findActiveEngineers();
        List<Skill> skills = skillRepository.findAllActiveWithCategory();
        List<EngineerSkill> allEngineerSkills = engineerSkillRepository.findAll();

        // Build lookup
        Map<String, EngineerSkill.SkillLevel> lookup = new HashMap<>();
        for (EngineerSkill es : allEngineerSkills) {
            lookup.put(es.getEngineer().getId() + "_" + es.getSkill().getId(), es.getLevel());
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Skill Matrix");
            sheet.setDefaultColumnWidth(18);

            // Styles
            CellStyle headerStyle = createHeaderStyle(workbook, new byte[]{(byte)30, (byte)41, (byte)59});
            CellStyle categoryStyle = createHeaderStyle(workbook, new byte[]{(byte)51, (byte)65, (byte)85});
            CellStyle spezialistStyle     = createLevelStyle(workbook, new byte[]{(byte)124, (byte)58,  (byte)237});
            CellStyle experteStyle        = createLevelStyle(workbook, new byte[]{(byte)5,   (byte)150, (byte)105});
            CellStyle fortgeschrittenStyle= createLevelStyle(workbook, new byte[]{(byte)37,  (byte)99,  (byte)235});
            CellStyle anwenderStyle       = createLevelStyle(workbook, new byte[]{(byte)217, (byte)119, (byte)6});
            CellStyle grundwissenStyle    = createLevelStyle(workbook, new byte[]{(byte)234, (byte)88,  (byte)12});
            CellStyle nopStyle            = createLevelStyle(workbook, new byte[]{(byte)107, (byte)114, (byte)128});
            CellStyle emptyStyle = createEmptyStyle(workbook);
            CellStyle engineerStyle = createEngineerStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowNum = 0;

            // Title row
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(36);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Skill Matrix — " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, engineers.size()));

            rowNum++; // blank row

            // Group skills by category
            Map<String, List<Skill>> byCategory = new LinkedHashMap<>();
            for (Skill skill : skills) {
                String catName = skill.getCategory() != null ? skill.getCategory().getName() : "Sonstiges";
                byCategory.computeIfAbsent(catName, k -> new ArrayList<>()).add(skill);
            }

            for (Map.Entry<String, List<Skill>> entry : byCategory.entrySet()) {
                // Category header
                Row catRow = sheet.createRow(rowNum++);
                catRow.setHeightInPoints(22);
                Cell catCell = catRow.createCell(0);
                catCell.setCellValue("📂  " + entry.getKey());
                catCell.setCellStyle(categoryStyle);
                sheet.addMergedRegion(new CellRangeAddress(catRow.getRowNum(), catRow.getRowNum(), 0, engineers.size()));

                // Skill header row
                Row skillHeaderRow = sheet.createRow(rowNum++);
                skillHeaderRow.setHeightInPoints(30);
                Cell skillLabelCell = skillHeaderRow.createCell(0);
                skillLabelCell.setCellValue("Skill / Engineer →");
                skillLabelCell.setCellStyle(headerStyle);

                for (int i = 0; i < engineers.size(); i++) {
                    Cell engCell = skillHeaderRow.createCell(i + 1);
                    engCell.setCellValue(engineers.get(i).getFullName());
                    engCell.setCellStyle(headerStyle);
                }

                // Skill rows
                for (Skill skill : entry.getValue()) {
                    Row skillRow = sheet.createRow(rowNum++);
                    skillRow.setHeightInPoints(20);

                    Cell nameCell = skillRow.createCell(0);
                    nameCell.setCellValue(skill.getName());
                    nameCell.setCellStyle(engineerStyle);

                    for (int i = 0; i < engineers.size(); i++) {
                        Cell levelCell = skillRow.createCell(i + 1);
                        String key = engineers.get(i).getId() + "_" + skill.getId();
                        EngineerSkill.SkillLevel level = lookup.get(key);
                        if (level != null) {
                            levelCell.setCellValue(level.getLabel());
                            levelCell.setCellStyle(switch (level) {
                                case SPEZIALIST      -> spezialistStyle;
                                case EXPERTE         -> experteStyle;
                                case FORTGESCHRITTEN -> fortgeschrittenStyle;
                                case ANWENDER        -> anwenderStyle;
                                case GRUNDWISSEN     -> grundwissenStyle;
                                case NOP             -> nopStyle;
                            });
                        } else {
                            levelCell.setCellValue("—");
                            levelCell.setCellStyle(emptyStyle);
                        }
                    }
                }
                rowNum++; // spacing between categories
            }

            // Summary sheet
            XSSFSheet summarySheet = workbook.createSheet("Zusammenfassung");
            summarySheet.setDefaultColumnWidth(20);
            int sumRow = 0;

            Row sumTitle = summarySheet.createRow(sumRow++);
            sumTitle.setHeightInPoints(32);
            Cell sumTitleCell = sumTitle.createCell(0);
            sumTitleCell.setCellValue("Skill-Zusammenfassung pro Engineer");
            sumTitleCell.setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            sumRow++;
            Row sumHeader = summarySheet.createRow(sumRow++);
            sumHeader.setHeightInPoints(24);
            String[] headers = {"Engineer", "Team", "Spezialist", "Experte", "Fortgeschritten", "Anwender", "Grundwissen", "NOP", "Gesamt"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = sumHeader.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            for (User engineer : engineers) {
                List<EngineerSkill> engSkills = engineerSkillRepository.findByEngineerId(engineer.getId());
                int spezialist = 0, experte = 0, fortgeschritten = 0, anwender = 0, grundwissen = 0, nop = 0;
                for (EngineerSkill es : engSkills) {
                    switch (es.getLevel()) {
                        case SPEZIALIST      -> spezialist++;
                        case EXPERTE         -> experte++;
                        case FORTGESCHRITTEN -> fortgeschritten++;
                        case ANWENDER        -> anwender++;
                        case GRUNDWISSEN     -> grundwissen++;
                        case NOP             -> nop++;
                    }
                }
                Row sumDataRow = summarySheet.createRow(sumRow++);
                sumDataRow.setHeightInPoints(18);
                sumDataRow.createCell(0).setCellValue(engineer.getFullName());
                sumDataRow.createCell(1).setCellValue(engineer.getTeam() != null ? engineer.getTeam() : "—");
                Cell c2 = sumDataRow.createCell(2); c2.setCellValue(spezialist);     c2.setCellStyle(spezialistStyle);
                Cell c3 = sumDataRow.createCell(3); c3.setCellValue(experte);        c3.setCellStyle(experteStyle);
                Cell c4 = sumDataRow.createCell(4); c4.setCellValue(fortgeschritten);c4.setCellStyle(fortgeschrittenStyle);
                Cell c5 = sumDataRow.createCell(5); c5.setCellValue(anwender);       c5.setCellStyle(anwenderStyle);
                Cell c6 = sumDataRow.createCell(6); c6.setCellValue(grundwissen);    c6.setCellStyle(grundwissenStyle);
                Cell c7 = sumDataRow.createCell(7); c7.setCellValue(nop);            c7.setCellStyle(nopStyle);
                sumDataRow.createCell(8).setCellValue(engSkills.size());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFColor color = new XSSFColor(rgb, null);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        font.setBold(true);
        font.setFontHeightInPoints((short)11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createLevelStyle(XSSFWorkbook wb, byte[] rgb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFColor color = new XSSFColor(rgb, null);
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        font.setBold(true);
        font.setFontHeightInPoints((short)10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createEmptyStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        XSSFFont font = (XSSFFont) wb.createFont();
        font.setColor(new XSSFColor(new byte[]{(byte)180,(byte)180,(byte)180}, null));
        style.setFont(font);
        return style;
    }

    private CellStyle createEngineerStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)248,(byte)250,(byte)252}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setBold(false);
        font.setFontHeightInPoints((short)10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)15,(byte)23,(byte)42}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));
        font.setBold(true);
        font.setFontHeightInPoints((short)16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
}