package com.example.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消费预算实体（categoryId 为空表示全局月预算）
 */
public class ExpenseBudget {
    private Long id;
    private Integer userId;
    private String month; // YYYY-MM
    private Integer categoryId; // 可为空，NULL 表示全局预算
    private BigDecimal amount; // 预算金额，人民币，两位小数
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}