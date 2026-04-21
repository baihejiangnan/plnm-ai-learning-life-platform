package com.example.service;

import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户业务处理
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 新增用户
     */
    public void add(User user) {
        // 检查用户名是否已存在
        User dbUser = userMapper.selectByUsername(user.getUsername());
        if (dbUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 加密密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        // 设置默认角色
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        userMapper.insert(user);
    }

    /**
     * 删除用户
     */
    public void deleteById(Integer id) {
        userMapper.deleteById(id);
    }

    /**
     * 修改用户
     */
    public void updateById(User user) {
        userMapper.updateById(user);
    }

    /**
     * 安全更新昵称和头像
     */
    public void updateProfile(Integer id, String name, String avatar) {
        userMapper.updateProfile(id, name, avatar);
    }

    /**
     * 根据ID查询用户
     */
    public User selectById(Integer id) {
        return userMapper.selectById(id);
    }

    /**
     * 查询所有用户
     */
    public List<User> selectAll() {
        return userMapper.selectAll();
    }

    /**
     * 根据用户名查询用户
     */
    public User selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    /**
     * 用户登录
     */
    public User login(User user) {
        System.out.println("UserService.login - 开始查询用户: " + user.getUsername());
        // 先根据用户名查询用户
        User dbUser = userMapper.selectByUsername(user.getUsername());
        System.out.println("UserService.login - 查询结果: " + (dbUser != null ? "找到用户" : "用户不存在"));
        if (dbUser == null) {
            System.out.println("UserService.login - 抛出异常: 用户名或密码错误");
            throw new RuntimeException("用户名或密码错误");
        }
        System.out.println("UserService.login - 返回用户: " + dbUser.getUsername());
        // 验证密码（需要注入PasswordEncoder）
        // 这里暂时返回用户，密码验证将在Controller层处理
        return dbUser;
    }

    /**
     * 用户注册
     */
    public void register(User user) {
        // 检查用户名是否已存在
        User dbUser = userMapper.selectByUsername(user.getUsername());
        if (dbUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 设置默认角色
        user.setRole("USER");
        userMapper.insert(user);
    }

    /**
     * 修改密码
     * @param userId 当前用户ID
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     */
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User dbUser = userMapper.selectById(userId);
        if (dbUser == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, dbUser.getPassword())) {
            throw new RuntimeException("原密码不正确");
        }
        String encoded = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, encoded);
    }
}