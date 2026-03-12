package com.zym.hd.user.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.user.dto.UserHomeDashboardDTO;
import com.zym.hd.user.entity.UserEntity;

public interface UserService extends IService<UserEntity> {

    UserEntity register(UserEntity user, String rawPassword);

    UserEntity login(String username, String rawPassword);

    UserEntity getProfile(Long userId);

    UserEntity updateUser(UserEntity user, String newPassword);

    UserEntity getByTeacherNo(String teacherNo);

    UserEntity getByStudentNo(String studentNo);

    UserHomeDashboardDTO getHomeDashboard(Long userId);

    boolean markNotificationRead(Long userId, String notificationId);
}
