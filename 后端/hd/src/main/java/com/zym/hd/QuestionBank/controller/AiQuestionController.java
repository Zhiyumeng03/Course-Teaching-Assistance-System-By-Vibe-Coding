package com.zym.hd.QuestionBank.controller;

import com.zym.hd.QuestionBank.dto.AiGenerateQuestionRequest;
import com.zym.hd.QuestionBank.dto.AiGenerateQuestionResponse;
import com.zym.hd.QuestionBank.service.AiQuestionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/question/ai")
public class AiQuestionController {

    private final AiQuestionService aiQuestionService;

    public AiQuestionController(AiQuestionService aiQuestionService) {
        this.aiQuestionService = aiQuestionService;
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/generate")
    public AiGenerateQuestionResponse generate(@RequestBody AiGenerateQuestionRequest request) {
        return aiQuestionService.generateQuestions(request);
    }
}