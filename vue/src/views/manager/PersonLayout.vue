<template>
  <div class="person-layout">
    <main class="page-body">
      <router-view @updateUser="$emit('updateUser')" />
    </main>
  </div>
</template>

<script setup>
defineEmits(['updateUser'])
</script>

<style scoped>
.person-layout {
  display: flex;
  flex-direction: column;
}
.page-body { padding: 0; }
</style>
