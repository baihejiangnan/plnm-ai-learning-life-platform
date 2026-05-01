package com.example.controller;

import com.example.common.Result;
import com.example.dto.RegisterRequest;
import com.example.entity.User;
import com.example.service.CaptchaService;
import com.example.service.LogoutLogService;
import com.example.service.UserService;
import com.example.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

/**
 * 用户信息表前端操作接口
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LogoutLogService logoutLogService;

    @Autowired
    private CaptchaService captchaService;

    /**
     * 新增用户
     */
    @PostMapping("/add")
    public Result add(@RequestBody User user) {
        userService.add(user);
        return Result.success();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        userService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping("/delete/batch")
    public Result deleteBatch(@RequestBody List<Integer> ids) {
        for (Integer id : ids) {
            userService.deleteById(id);
        }
        return Result.success();
    }

    /**
     * 修改用户
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        User user = userService.selectById(id);
        return Result.success(user);
    }

    /**
     * 查询所有用户
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        List<User> list = userService.selectAll();
        return Result.success(list);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        try {
            User dbUser = userService.login(user);
            // 验证密码
            if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
                throw new RuntimeException("用户名或密码错误");
            }
            
            // 生成JWT令牌
            String token = jwtUtil.generateToken(dbUser.getUsername(), dbUser.getRole(), dbUser.getId());
            
            // 清除密码信息后返回
            dbUser.setPassword(null);
            
            // 返回用户信息和令牌
            Map<String, Object> result = new HashMap<>();
            result.put("user", dbUser);
            result.put("token", token);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("登录失败：" + e.getMessage());
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody RegisterRequest request) {
        try {
            if (!captchaService.validate(request.getCaptchaId(), request.getCaptchaCode())) {
                return Result.error("验证码错误或已过期");
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setName(request.getName());

            // 仅将明文密码透传到业务层，在业务层统一加密，避免重复加密
            User created = userService.register(user);
            created.setPassword(null);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error("注册失败：" + e.getMessage());
        }
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public Result changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            // 修复参数名：前端使用 oldPassword
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            if (oldPassword == null || newPassword == null) {
                return Result.error("参数不完整");
            }
            String token = getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("未授权访问，请先登录");
            }
            Integer userId = jwtUtil.getUserIdFromToken(token);
            userService.changePassword(userId, oldPassword, newPassword);
            return Result.success("修改密码成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request) {
        try {
            // 从请求头中获取JWT token
            String token = getTokenFromRequest(request);
            
            if (token != null && jwtUtil.validateToken(token)) {
                // 从token中获取用户信息
                String username = jwtUtil.getUsernameFromToken(token);
                Integer userId = jwtUtil.getUserIdFromToken(token);
                
                // 记录登出日志
                logoutLogService.recordLogout(userId, username, request, "MANUAL");
                
                return Result.success("登出成功");
            } else {
                // token无效或不存在，仍然返回成功（前端已经清除了token）
                return Result.success("登出成功");
            }
        } catch (Exception e) {
            // 即使记录日志失败，也不应该影响登出流程
            System.err.println("登出处理异常: " + e.getMessage());
            return Result.success("登出成功");
        }
    }
    
    /** 新增：更新个人资料（仅允许修改昵称与头像） */
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("未授权访问，请先登录");
            }
            Integer userId = jwtUtil.getUserIdFromToken(token);
            String name = body.get("name");
            String avatar = body.get("avatar");
            userService.updateProfile(userId, name, avatar);
            // 返回最新用户信息（隐藏密码）
            User user = userService.selectById(userId);
            if (user != null) user.setPassword(null);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /** 获取当前登录用户信息 */
    @GetMapping("/info")
    public Result info(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("未授权访问，请先登录");
            }
            Integer userId = jwtUtil.getUserIdFromToken(token);
            User user = userService.selectById(userId);
            if (user != null) {
                user.setPassword(null);
            }
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 从请求中获取JWT token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 获取客户端真实IP地址（用于会话列表展示）
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 取第一个IP
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * 下线该用户的所有会话（预留：后续可接入Redis/数据库的Token黑名单或会话表）
     */
    @PostMapping("/logoutAll")
    public Result logoutAllSessions(HttpServletRequest request) {
        try {
            String token = getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("未授权访问，请先登录");
            }
            String username = jwtUtil.getUsernameFromToken(token);
            Integer userId = jwtUtil.getUserIdFromToken(token);
            // 记录“强制下线所有会话”的审计日志
            try {
                logoutLogService.recordLogout(userId, username, request, "FORCE_ALL");
            } catch (Exception e) {
                // 日志失败不影响主流程
                System.err.println("记录下线所有会话日志失败: " + e.getMessage());
            }
            // 提示：实际让所有设备失效需要配合Token黑名单/会话存储，这里先返回成功
            return Result.success("已下线所有会话");
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户的登录会话列表（预留接口）
     * 后续接入真实会话存储后返回分页数据，目前先返回当前会话信息
     */
    @GetMapping("/sessions")
    public Result getSessions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        try {
            String token = getTokenFromRequest(request);
            if (token == null || !jwtUtil.validateToken(token)) {
                return Result.error("未授权访问，请先登录");
            }

            // 当前会话信息
            Date issuedAt = jwtUtil.getIssuedAtDateFromToken(token);
            Date expiration = jwtUtil.getExpirationDateFromToken(token);
            String userAgent = Optional.ofNullable(request.getHeader("User-Agent")).orElse("-");
            String ip = getClientIpAddress(request);

            Map<String, Object> session = new HashMap<>();
            session.put("id", Math.abs(token.hashCode()));
            session.put("device", userAgent);
            session.put("ip", ip);
            session.put("loginTime", issuedAt != null ? issuedAt.getTime() : null);
            session.put("expireTime", expiration != null ? expiration.getTime() : null);
            session.put("current", true);
            session.put("location", null);

            List<Map<String, Object>> list = new ArrayList<>();
            if (page != null && page == 1) {
                list.add(session);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("list", list);
            data.put("page", page);
            data.put("size", size);
            data.put("total", 1);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
