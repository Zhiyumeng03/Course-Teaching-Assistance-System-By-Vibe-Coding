package com.zym.hd.user.controller;

import com.zym.hd.user.entity.UserEntity;
import com.zym.hd.user.service.UserService;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserTempAliasController {

    private final UserService userService;

    public UserTempAliasController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register/temp")
    public UserEntity registerTemp(@RequestBody TempRegisterRequest request) {
        return userService.registerTempPressureUser(request.getUsername(), request.getPassword());
    }

    @Data
    public static class TempRegisterRequest {
        private String username;
        private String password;
    }
}

