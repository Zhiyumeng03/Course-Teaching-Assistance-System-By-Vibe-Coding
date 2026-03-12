package com.zym.hd.user.controller;

import com.zym.hd.security.JwtTokenService;
import com.zym.hd.security.SecurityContextUtil;
import com.zym.hd.user.dto.UserHomeDashboardDTO;
import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.service.UserService;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    public UserController(UserService userService, JwtTokenService jwtTokenService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    public UserEntity register(@RequestBody RegisterRequest request) {
        UserEntity user = new UserEntity();
        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setRealName(request.getRealName());
        user.setStudentNo(request.getStudentNo());
        user.setTeacherNo(request.getTeacherNo());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        return userService.register(user, request.getPassword());
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        UserEntity user = userService.login(request.getUsername(), request.getPassword());
        String token = jwtTokenService.generateToken(user);
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(user);
        return response;
    }

    @GetMapping("/profile")
    public UserEntity profile() {
        return userService.getProfile(SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @GetMapping("/dashboard")
    public UserHomeDashboardDTO dashboard() {
        return userService.getHomeDashboard(SecurityContextUtil.currentUserId());
    }

    @PreAuthorize("hasAnyRole('STUDENT','TEACHER','ADMIN')")
    @PostMapping("/dashboard/notifications/read")
    public boolean markNotificationRead(@RequestBody NotificationReadRequest request) {
        return userService.markNotificationRead(SecurityContextUtil.currentUserId(), request.getNotificationId());
    }

    @GetMapping("/getById")
    public UserEntity getById(@RequestParam("id") Long id) {
        return userService.getById(id);
    }

    @GetMapping("/getByTeacherNo")
    public UserEntity getByTeacherNo(@RequestParam("teacherNo") String teacherNo) {
        return userService.getByTeacherNo(teacherNo);
    }

    @GetMapping("/getByStudentNo")
    public UserEntity getByStudentNo(@RequestParam("studentNo") String studentNo) {
        return userService.getByStudentNo(studentNo);
    }

    @PutMapping("/update")
    public UserEntity update(@RequestBody UpdateRequest request) {
        UserEntity user = new UserEntity();
        user.setId(SecurityContextUtil.currentUserId());
        user.setRealName(request.getRealName());
        user.setStudentNo(request.getStudentNo());
        user.setTeacherNo(request.getTeacherNo());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        return userService.updateUser(user, request.getPassword());
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String role;
        private String realName;
        private String studentNo;
        private String teacherNo;
        private String email;
        private String phone;
        private String avatarUrl;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class UpdateRequest {
        private String password;
        private String realName;
        private String studentNo;
        private String teacherNo;
        private String email;
        private String phone;
        private String avatarUrl;
    }

    @Data
    public static class NotificationReadRequest {
        private String notificationId;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private UserEntity user;
    }
}

