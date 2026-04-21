<template>
  <div class="person-layout">
    <main class="page-body">
      <router-view />
    </main>
  </div>
</template>

<script setup>
// 个人中心父级布局，仅承载子路由内容
</script>

<style scoped>
.person-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.page-body { padding: 0; }
</style>