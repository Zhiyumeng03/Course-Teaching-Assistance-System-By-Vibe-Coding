package com.zym.hd.QuestionBank.controller;

import com.zym.hd.QuestionBank.dto.PracticeRecordPageResponse;
import com.zym.hd.QuestionBank.entity.PracticeAnswerEntity;
import com.zym.hd.QuestionBank.entity.PracticeEntity;
import com.zym.hd.QuestionBank.service.PracticeService;
import com.zym.hd.security.LoginUser;
import com.zym.hd.security.SecurityContextUtil;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/practice")
public class PracticeController {

    private final PracticeService practiceService;

    public PracticeController(PracticeService practiceService) {
        this.practiceService = practiceService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/create")
    public PracticeEntity create(@RequestBody Map<String, Object> payload) {
        return practiceService.createRecord(payload, currentUserId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/update")
    public PracticeEntity update(@RequestBody Map<String, Object> payload) {
        return practiceService.updateRecord(payload, currentUserId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return practiceService.deleteRecord(id, currentUserId());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/getById")
    public PracticeEntity getById(@RequestParam("id") Long id) {
        LoginUser user = currentUser();
        return practiceService.getRecordById(id, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/list")
    public List<PracticeEntity> list() {
        return practiceService.listByStudent(currentUserId());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/student/list")
    public List<Map<String, Object>> listStudentRecords(@RequestParam("courseId") Long courseId) {
        return practiceService.listStudentRecords(courseId, currentUserId());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/teacher/list")
    public List<Map<String, Object>> listTeacherRecords(@RequestParam("courseId") Long courseId) {
        LoginUser user = currentUser();
        return practiceService.listTeacherRecords(courseId, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/teacher/page")
    public PracticeRecordPageResponse pageTeacherRecords(@RequestParam("courseId") Long courseId,
                                                         @RequestParam(value = "current", defaultValue = "1") long current,
                                                         @RequestParam(value = "size", defaultValue = "6") long size,
                                                         @RequestParam(value = "status", required = false) String status,
                                                         @RequestParam(value = "keyword", required = false) String keyword) {
        LoginUser user = currentUser();
        return practiceService.pageTeacherRecords(courseId, user.getUserId(), user.getRole(), current, size, status, keyword);
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/answer/save")
    public PracticeAnswerEntity saveAnswer(@RequestBody Map<String, Object> payload) {
        return practiceService.saveAnswer(payload, currentUserId());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/answer/list")
    public List<PracticeAnswerEntity> listAnswers(@RequestParam("recordId") Long recordId) {
        LoginUser user = currentUser();
        return practiceService.listAnswers(recordId, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/submit")
    public Map<String, Object> submit(@RequestBody Map<String, Object> payload) {
        return practiceService.submitPractice(payload, currentUserId());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/detail")
    public Map<String, Object> detail(@RequestParam("recordId") Long recordId) {
        LoginUser user = currentUser();
        return practiceService.getRecordDetail(recordId, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/review")
    public Map<String, Object> review(@RequestBody Map<String, Object> payload) {
        LoginUser user = currentUser();
        return practiceService.reviewPractice(payload, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/{recordId}/diagnosis")
    public Map<String, Object> diagnosis(@PathVariable("recordId") Long recordId) {
        LoginUser user = currentUser();
        return practiceService.generateDiagnosis(recordId, user.getUserId(), user.getRole());
    }

    private LoginUser currentUser() {
        return SecurityContextUtil.currentUser();
    }

    private Long currentUserId() {
        return SecurityContextUtil.currentUserId();
    }
}

