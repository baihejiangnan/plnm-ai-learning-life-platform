package com.example.controller;

import com.example.common.Result;
import com.example.entity.Account;
import com.example.entity.User;

import com.example.service.UserService;
import com.example.service.ExpenseCategoryService;
import com.example.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RestController
@Tag(name = "认证接口", description = "用户认证相关接口")
public class WebController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);



    @Resource
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ExpenseCategoryService expenseCategoryService;


    /**
     * 默认请求接口
     */
    @GetMapping("/")
    public Result hello() {
        return Result.success();
    }



    /**
     * 登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录接口，返回JWT令牌")
    public Result login(@RequestBody Account account) {
        System.out.println("=== 登录请求开始 ===");
        System.out.println("用户名: " + account.getUsername());
        System.out.println("密码: " + account.getPassword());
        
        try {
            // 统一使用UserService进行用户认证
            User user = new User();
            user.setUsername(account.getUsername());
            user.setPassword(account.getPassword());
            
            System.out.println("调用userService.login...");
            User dbUser = userService.login(user);
            System.out.println("找到用户: " + dbUser.getUsername() + ", 角色: " + dbUser.getRole());
            System.out.println("数据库密码: " + dbUser.getPassword());
            
            // 验证密码
            System.out.println("开始密码验证...");
            boolean matches = passwordEncoder.matches(user.getPassword(), dbUser.getPassword());
            System.out.println("密码匹配结果: " + matches);
            
            if (!matches) {
                System.out.println("密码验证失败");
                throw new RuntimeException("用户名或密码错误");
            }
            
            System.out.println("密码验证成功");
            
            // 生成JWT令牌
            String token = jwtUtil.generateToken(dbUser.getUsername(), dbUser.getRole(), dbUser.getId());
            
            // 清除密码信息后返回
            dbUser.setPassword(null);
            
            // 返回用户信息和令牌
            Map<String, Object> result = new HashMap<>();
            result.put("user", dbUser);
            result.put("token", token);
            
            System.out.println("登录成功");
            return Result.success(result);
        } catch (Exception e) {
            System.out.println("=== 登录异常详情 ===");
            System.out.println("异常类型: " + e.getClass().getName());
            System.out.println("异常消息: " + e.getMessage());
            e.printStackTrace();
            return Result.error("登录失败：" + e.getMessage());
        }
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册接口")
    public Result register(@RequestBody User user) {
        try {
            // 加密密码
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userService.register(user);
            // 初始化默认消费分类
            if (user.getId() != null) {
                try {
                    expenseCategoryService.initDefault(user.getId());
                } catch (Exception ignore) {}
            }
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error("注册失败：" + e.getMessage());
        }
    }



}
