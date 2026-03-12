package com.zym.hd.course.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.course.entity.CourseMember;
import com.zym.hd.course.mapper.CourseMemberMapper;
import com.zym.hd.course.service.CourseMemberService;
import org.springframework.stereotype.Service;

@Service
public class CourseMemberServiceImpl extends ServiceImpl<CourseMemberMapper, CourseMember> implements CourseMemberService {
}

