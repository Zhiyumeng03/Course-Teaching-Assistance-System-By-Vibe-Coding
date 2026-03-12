package com.zym.hd.QuestionBank.service;

import com.zym.hd.QuestionBank.dto.AiGenerateQuestionRequest;
import com.zym.hd.QuestionBank.dto.AiGenerateQuestionResponse;

public interface AiQuestionService {

    AiGenerateQuestionResponse generateQuestions(AiGenerateQuestionRequest request);
}