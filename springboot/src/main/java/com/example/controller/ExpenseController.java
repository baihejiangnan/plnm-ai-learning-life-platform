package com.example.controller;

import com.example.common.Result;
import com.example.entity.Expense;
import com.example.entity.ExpenseBudget;
import com.example.service.ExpenseBudgetService;
import com.example.service.ExpenseService;
import com.example.utils.JwtUtil;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;
    @Autowired
    private ExpenseBudgetService budgetService;
    @Autowired
    private JwtUtil jwtUtil;

    private Integer parseUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public Result create(@RequestHeader(value = "Authorization", required = false) String auth,
                         @RequestBody Expense e) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        e.setUserId(userId);
        Expense created = expenseService.create(e);
        return Result.success(created);
    }

    @PutMapping("/{id}")
    public Result update(@RequestHeader(value = "Authorization", required = false) String auth,
                         @PathVariable Long id,
                         @RequestBody Expense e) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        e.setId(id);
        e.setUserId(userId);
        Expense updated = expenseService.update(e);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result delete(@RequestHeader(value = "Authorization", required = false) String auth,
                         @PathVariable Long id) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        boolean ok = expenseService.delete(id, userId);
        return ok ? Result.success("删除成功") : Result.error("删除失败");
    }

    @GetMapping
    public Result list(@RequestHeader(value = "Authorization", required = false) String auth,
                       @RequestParam(required = false) String month,
                       @RequestParam(required = false) Integer categoryId,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        PageInfo<Expense> pageInfo = expenseService.list(userId, month, categoryId, page, size);
        return Result.success(pageInfo);
    }

    @GetMapping("/stats/month")
    public Result monthStats(@RequestHeader(value = "Authorization", required = false) String auth,
                             @RequestParam(required = false) String month) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        if (month == null || month.isEmpty()) {
            month = LocalDate.now().toString().substring(0, 7);
        }
        return Result.success(expenseService.monthStats(userId, month));
    }

    @GetMapping("/budgets")
    public Result getBudgets(@RequestHeader(value = "Authorization", required = false) String auth,
                             @RequestParam String month) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        List<ExpenseBudget> list = budgetService.getBudgets(userId, month);
        return Result.success(list);
    }

    @PostMapping("/budgets")
    public Result setBudget(@RequestHeader(value = "Authorization", required = false) String auth,
                            @RequestBody Map<String, Object> payload) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        Object monthObj = payload.get("month");
        if (monthObj == null) return Result.error("月份不能为空");
        String month = String.valueOf(monthObj);
        Integer categoryId = null;
        if (payload.get("categoryId") != null) {
            try { categoryId = Integer.valueOf(String.valueOf(payload.get("categoryId"))); } catch (Exception ignored) {}
        }
        Object amtObj = payload.get("budget");
        if (amtObj == null) amtObj = payload.get("amount");
        BigDecimal amount;
        try { amount = new BigDecimal(String.valueOf(amtObj)); } catch (Exception e) { return Result.error("预算金额不合法"); }
        ExpenseBudget b = new ExpenseBudget();
        b.setUserId(userId);
        b.setMonth(month);
        b.setCategoryId(categoryId);
        b.setAmount(amount);
        ExpenseBudget saved = budgetService.setBudget(b);
        return Result.success(saved);
    }
}