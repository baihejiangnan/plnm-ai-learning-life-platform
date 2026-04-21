<template>
  <div class="notes-layout">
    <main class="page-body">
      <router-view />
    </main>
  </div>
</template>

<script setup>
// 笔记中心父级布局，承载列表、编辑器、详情等子页面
</script>

<style scoped>
.notes-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-body { padding: 0; }
</style>