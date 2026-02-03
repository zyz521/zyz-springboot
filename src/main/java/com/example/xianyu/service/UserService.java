package com.example.xianyu.service;

import com.example.xianyu.entity.User;
import com.example.xianyu.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class UserService {


    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String rawPassword, String phone, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodePassword(rawPassword));
        user.setPhone(phone);
        user.setEmail(email);
        return userRepository.save(user);
    }
    public Optional<User> login(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(u -> u.getPassword().equals(encodePassword(rawPassword)));
    }

    private String encodePassword(String raw) {
        // 简单 MD5 加密示例，生产环境建议使用 BCrypt 等更安全算法
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

   /**
    * 更新用户头像
    */
    public boolean updateAvatar(Long userId, String avatarPath)
    {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setAvatar(avatarPath);
            userRepository.save(user);
            return true;
        }
        return false;
   }

   /**
    * 修改用户密码
    * @param userId 用户ID
    * @param oldPassword 旧密码（原始密码）
    * @param newPassword 新密码（原始密码）
    * @return 修改结果：true-成功，false-失败（旧密码错误或用户不存在）
    */
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 验证旧密码
            if (user.getPassword().equals(encodePassword(oldPassword))) {
                // 更新为新密码
                user.setPassword(encodePassword(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
}


