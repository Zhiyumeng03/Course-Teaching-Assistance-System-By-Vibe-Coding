package com.zym.hd.QuestionBank.controller;

import com.zym.hd.QuestionBank.entity.KnowledgeEntity;
import com.zym.hd.QuestionBank.service.KnowledgeService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping(value = "/tree", produces = MediaType.APPLICATION_JSON_VALUE)
    public String tree(@RequestParam("courseId") Long courseId) {
        return knowledgeService.getKnowledgeTreeJson(courseId);
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/seed/cs")
    public String seedComputerScienceTree(@RequestParam("courseId") Long courseId) {
        int count = knowledgeService.seedComputerScienceTree(courseId);
        return "seeded knowledge points: " + count;
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/create")
    public boolean create(@RequestBody KnowledgeEntity entity) {
        return knowledgeService.createKnowledgePoint(entity);
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/update")
    public boolean update(@RequestBody KnowledgeEntity entity) {
        return knowledgeService.updateById(entity);
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return knowledgeService.removeById(id);
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/getById")
    public KnowledgeEntity getById(@RequestParam("id") Long id) {
        return knowledgeService.getById(id);
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/list")
    public List<KnowledgeEntity> list(@RequestParam("courseId") Long courseId) {
        return knowledgeService.lambdaQuery()
                .eq(KnowledgeEntity::getCourseId, courseId)
                .orderByAsc(KnowledgeEntity::getSortNo)
                .list();
    }
}

