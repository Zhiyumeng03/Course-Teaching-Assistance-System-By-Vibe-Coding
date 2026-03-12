package com.zym.hd.QuestionBank.controller;

import com.zym.hd.QuestionBank.entity.PaperEntity;
import com.zym.hd.QuestionBank.entity.PaperQuestionEntity;
import com.zym.hd.QuestionBank.service.PaperService;
import com.zym.hd.course.dto.PageResponse;
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
@RequestMapping("/api/paper")
public class PaperController {

    private final PaperService paperService;

    public PaperController(PaperService paperService) {
        this.paperService = paperService;
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/create")
    public PaperEntity create(@RequestBody Map<String, Object> payload) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.createPaper(payload, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/update")
    public PaperEntity update(@RequestBody Map<String, Object> payload) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.updatePaper(payload, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.deletePaper(id, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/{id}/publish")
    public PaperEntity publish(@PathVariable("id") Long id) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.publishPaper(id, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/{id}/close")
    public PaperEntity close(@PathVariable("id") Long id) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.closePaper(id, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/getById")
    public PaperEntity getById(@RequestParam("id") Long id) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.getPaperById(id, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/list")
    public List<PaperEntity> list(@RequestParam("courseId") Long courseId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.listByCourseId(courseId, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/page")
    public PageResponse<PaperEntity> page(@RequestParam("courseId") Long courseId,
                                          @RequestParam(value = "current", defaultValue = "1") long current,
                                          @RequestParam(value = "size", defaultValue = "6") long size,
                                          @RequestParam(value = "keyword", required = false) String keyword) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.pageByCourseId(courseId, user.getUserId(), user.getRole(), current, size, keyword);
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/detail")
    public Map<String, Object> detail(@RequestParam("id") Long id) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.getPaperDetail(id, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/{paperId}/questions/bind")
    public List<PaperQuestionEntity> bindQuestions(@PathVariable("paperId") Long paperId,
                                                   @RequestBody List<Map<String, Object>> questionItems) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.bindQuestions(paperId, questionItems, user.getUserId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/{paperId}/questions")
    public List<Map<String, Object>> listPaperQuestions(@PathVariable("paperId") Long paperId) {
        LoginUser user = SecurityContextUtil.currentUser();
        return paperService.listPaperQuestions(paperId, user.getUserId(), user.getRole());
    }
}

