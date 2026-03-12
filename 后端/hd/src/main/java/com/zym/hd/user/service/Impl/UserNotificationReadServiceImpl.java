package com.zym.hd.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zym.hd.user.entity.UserNotificationReadEntity;
import com.zym.hd.user.mapper.UserNotificationReadMapper;
import com.zym.hd.user.service.UserNotificationReadService;
import org.springframework.stereotype.Service;

@Service
public class UserNotificationReadServiceImpl
        extends ServiceImpl<UserNotificationReadMapper, UserNotificationReadEntity>
        implements UserNotificationReadService {
}
