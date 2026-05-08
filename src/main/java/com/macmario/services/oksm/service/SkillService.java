package com.macmario.services.oksm.service;

import com.macmario.services.oksm.model.*;
import com.macmario.services.oksm.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class SkillService {

    @Autowired private SkillRepository skillRepository;
    @Autowired private SkillCategoryRepository categoryRepository;
    @Autowired private EngineerSkillRepository engineerSkillRepository;
    @Autowired private EngineerSubSkillRepository engineerSubSkillRepository;
    @Autowired private SubSkillRepository subSkillRepository;
    @Autowired private UserRepository userRepository;

    // ── Categories ──────────────────────────────────────────
    public List<SkillCategory> getAllCategories() { return categoryRepository.findAllByOrderByNameAsc(); }
    public SkillCategory saveCategory(SkillCategory c) { return categoryRepository.save(c); }
    public Optional<SkillCategory> findCategoryById(Long id) { return categoryRepository.findById(id); }

    // ── Skills ───────────────────────────────────────────────
    public List<Skill> getAllActiveSkills() { return skillRepository.findAllActiveWithCategory(); }
    public List<Skill> getSkillsByCategory(Long catId) { return skillRepository.findByCategoryId(catId); }
    public Skill saveSkill(Skill skill) { return skillRepository.save(skill); }
    public Optional<Skill> findSkillById(Long id) { return skillRepository.findById(id); }

    public void toggleSkillActive(Long id) {
        Skill s = skillRepository.findById(id).orElseThrow();
        s.setActive(!s.isActive());
        skillRepository.save(s);
    }

    public void moveSkillToCategory(Long skillId, Long categoryId) {
        Skill skill = skillRepository.findById(skillId)
            .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));
        SkillCategory category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
        skill.setCategory(category);
        skillRepository.save(skill);
    }

    // ── SubSkills ────────────────────────────────────────────
    public List<SubSkill> getSubSkillsForSkill(Long skillId) {
        return subSkillRepository.findBySkillIdOrderByDisplayOrderAsc(skillId);
    }

    public SubSkill saveSubSkill(SubSkill ss) { return subSkillRepository.save(ss); }

    public SubSkill createSubSkill(Long skillId, String name, String description, int order) {
        Skill skill = skillRepository.findById(skillId).orElseThrow();
        // upsert by name
        return subSkillRepository.findBySkillIdAndName(skillId, name)
            .map(existing -> { existing.setDescription(description); existing.setDisplayOrder(order); return subSkillRepository.save(existing); })
            .orElseGet(() -> subSkillRepository.save(new SubSkill(skill, name, description, order)));
    }

    public void toggleSubSkillActive(Long id) {
        SubSkill ss = subSkillRepository.findById(id).orElseThrow();
        ss.setActive(!ss.isActive());
        subSkillRepository.save(ss);
    }

    public void deleteSubSkill(Long id) { subSkillRepository.deleteById(id); }

    // ── Engineer Skills ──────────────────────────────────────
    public List<EngineerSkill> getEngineerSkills(Long engineerId) {
        return engineerSkillRepository.findByEngineerIdWithDetails(engineerId);
    }

    public void saveOrUpdateEngineerSkill(Long engineerId, Long skillId,
                                          EngineerSkill.SkillLevel level, String notes) {
        User engineer = userRepository.findById(engineerId).orElseThrow();
        Skill skill = skillRepository.findById(skillId).orElseThrow();
        engineerSkillRepository.findByEngineerIdAndSkillId(engineerId, skillId)
            .ifPresentOrElse(
                es -> { es.setLevel(level); es.setNotes(notes); engineerSkillRepository.save(es); },
                () -> { EngineerSkill es = new EngineerSkill(engineer, skill, level); es.setNotes(notes); engineerSkillRepository.save(es); }
            );
    }

    public void removeEngineerSkill(Long engineerId, Long skillId) {
        User e = userRepository.findById(engineerId).orElseThrow();
        Skill s = skillRepository.findById(skillId).orElseThrow();
        engineerSkillRepository.deleteByEngineerAndSkill(e, s);
    }

    // ── Engineer SubSkills ───────────────────────────────────
    public List<EngineerSubSkill> getEngineerSubSkills(Long engineerId) {
        return engineerSubSkillRepository.findByEngineerIdWithDetails(engineerId);
    }

    public List<EngineerSubSkill> getEngineerSubSkillsForSkill(Long engineerId, Long skillId) {
        return engineerSubSkillRepository.findByEngineerIdAndSkillId(engineerId, skillId);
    }

    public void saveOrUpdateEngineerSubSkill(Long engineerId, Long subSkillId,
                                              EngineerSkill.SkillLevel level, String notes) {
        User engineer = userRepository.findById(engineerId).orElseThrow();
        SubSkill ss = subSkillRepository.findById(subSkillId).orElseThrow();
        engineerSubSkillRepository.findByEngineerIdAndSubSkillId(engineerId, subSkillId)
            .ifPresentOrElse(
                ess -> { ess.setLevel(level); ess.setNotes(notes); engineerSubSkillRepository.save(ess); },
                () -> engineerSubSkillRepository.save(new EngineerSubSkill(engineer, ss, level))
            );
    }

    public void removeEngineerSubSkill(Long engineerId, Long subSkillId) {
        engineerSubSkillRepository.deleteByEngineerIdAndSubSkillId(engineerId, subSkillId);
    }

    // ── Matrix & Gap Analysis ────────────────────────────────
    public Map<String, Object> buildSkillMatrix() {
        List<User> engineers = userRepository.findActiveEngineers();
        List<Skill> skills = skillRepository.findAllActiveWithCategory();
        List<EngineerSkill> allES = engineerSkillRepository.findAll();

        Map<String, EngineerSkill.SkillLevel> lookup = new HashMap<>();
        for (EngineerSkill es : allES) {
            lookup.put(es.getEngineer().getId() + "_" + es.getSkill().getId(), es.getLevel());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("engineers", engineers);
        result.put("skills", skills);
        result.put("lookup", lookup);
        return result;
    }

    public Map<String, Object> analyzeGaps(List<Long> requiredSkillIds,
                                            EngineerSkill.SkillLevel minLevel) {
        List<User> engineers = userRepository.findActiveEngineers();
        List<EngineerSkill> allSkills = engineerSkillRepository.findAll();

        Map<Long, Map<Long, EngineerSkill.SkillLevel>> esMap = new HashMap<>();
        for (EngineerSkill es : allSkills) {
            esMap.computeIfAbsent(es.getEngineer().getId(), k -> new HashMap<>())
                 .put(es.getSkill().getId(), es.getLevel());
        }

        List<Map<String, Object>> gapData = new ArrayList<>();
        for (User engineer : engineers) {
            Map<Long, EngineerSkill.SkillLevel> skills = esMap.getOrDefault(engineer.getId(), Collections.emptyMap());
            int covered = 0;
            List<Long> gaps = new ArrayList<>();
            for (Long sid : requiredSkillIds) {
                EngineerSkill.SkillLevel lv = skills.get(sid);
                if (lv != null && lv.getOrder() >= minLevel.getOrder()) covered++;
                else gaps.add(sid);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("engineer", engineer);
            row.put("covered", covered);
            row.put("total", requiredSkillIds.size());
            row.put("gaps", gaps);
            row.put("coverage", requiredSkillIds.isEmpty() ? 100 : (covered * 100 / requiredSkillIds.size()));
            gapData.add(row);
        }
        gapData.sort((a, b) -> (int) b.get("coverage") - (int) a.get("coverage"));

        Map<String, Object> result = new HashMap<>();
        result.put("engineers", gapData);
        result.put("requiredSkillIds", requiredSkillIds);
        return result;
    }
}
