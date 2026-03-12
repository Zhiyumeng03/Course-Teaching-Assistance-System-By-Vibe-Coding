package com.zym.hd.QuestionBank.controller;

import com.zym.hd.QuestionBank.dto.QuestionPageResponse;
import com.zym.hd.QuestionBank.entity.QuestionEntity;
import com.zym.hd.QuestionBank.service.QuestionService;
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
@RequestMapping("/api/question")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @PostMapping("/create")
    public QuestionEntity create(@RequestBody Map<String, Object> payload) {
        LoginUser user = SecurityContextUtil.currentUser();
        return questionService.createQuestion(payload, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @PutMapping("/update")
    public QuestionEntity update(@RequestBody Map<String, Object> payload) {
        LoginUser user = SecurityContextUtil.currentUser();
        return questionService.updateQuestion(payload, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        LoginUser user = SecurityContextUtil.currentUser();
        return questionService.deleteQuestion(id, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/getById")
    public QuestionEntity getById(@RequestParam("id") Long id) {
        return questionService.getQuestionById(id);
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/list")
    public List<QuestionEntity> list(@RequestParam("courseId") Long courseId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return questionService.listByCourseId(courseId, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/page")
    public QuestionPageResponse page(@RequestParam("courseId") Long courseId,
                                     @RequestParam(value = "current", defaultValue = "1") long current,
                                     @RequestParam(value = "size", defaultValue = "10") long size,
                                     @RequestParam(value = "type", required = false) String type,
                                     @RequestParam(value = "reviewStatus", required = false) String reviewStatus,
                                     @RequestParam(value = "keyword", required = false) String keyword) {
        LoginUser user = SecurityContextUtil.currentUser();
        return questionService.pageByCourseId(courseId, user.getUserId(), user.getRole(), current, size, type, reviewStatus, keyword);
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/review")
    public QuestionEntity review(@RequestBody Map<String, Object> payload) {
        LoginUser user = SecurityContextUtil.currentUser();
        Long id = payload.get("id") == null ? null : Long.parseLong(String.valueOf(payload.get("id")));
        String reviewStatus = payload.get("reviewStatus") == null ? null : String.valueOf(payload.get("reviewStatus"));
        return questionService.reviewQuestion(id, reviewStatus, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/convert")
    public Map<String, String> convert(@RequestBody Map<String, Object> payload) {
        return questionService.convertQuestionJson(
                String.valueOf(payload.get("type")),
                payload.get("content"),
                payload.get("answer"));
    }
}

