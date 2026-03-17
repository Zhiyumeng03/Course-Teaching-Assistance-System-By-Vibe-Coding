package com.zym.hd.user.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zym.hd.user.dto.UserHomeDashboardDTO;
import com.zym.hd.user.entity.UserEntity;

public interface UserService extends IService<UserEntity> {

    UserEntity register(UserEntity user, String rawPassword, String smsCode);

    UserEntity registerTempPressureUser(String username, String rawPassword);

    UserEntity login(String username, String rawPassword);

    UserEntity loginByPhone(String phone, String smsCode);

    void sendSmsCode(String phone, String scene);

    String createLoginSessionToken(UserEntity user);

    void logoutByToken(String token);

    UserEntity getProfile(Long userId);

    UserEntity updateUser(UserEntity user, String newPassword);

    UserEntity getByTeacherNo(String teacherNo);

    UserEntity getByStudentNo(String studentNo);

    UserHomeDashboardDTO getHomeDashboard(Long userId);

    boolean markNotificationRead(Long userId, String notificationId);
}
