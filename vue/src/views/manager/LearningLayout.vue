<template>
  <div class="learning-layout">
    <!-- 已删除顶部卡片（学习中心横幅） -->
    <main class="page-body">
      <router-view />
    </main>
  </div>
</template>

<script setup>
// 顶部卡片已删除，无需导入图标
</script>

<style scoped>
.learning-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-body { padding: 0; }
</style>