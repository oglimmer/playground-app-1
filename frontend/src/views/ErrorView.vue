<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ code: number }>()

const message = computed(() => {
  switch (props.code) {
    case 403:
      return "You don't have permission to view this page."
    case 404:
      return 'That page could not be found.'
    default:
      return 'Something went wrong. Please try again.'
  }
})
</script>

<template>
  <div class="error">
    <div class="card error-card">
      <div class="code">{{ code }}</div>
      <p>{{ message }}</p>
      <RouterLink to="/" class="btn btn-primary">Back to wiki</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.error {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 1.5rem;
}
.error-card {
  max-width: 420px;
  width: 100%;
  padding: 2.5rem 2rem;
  text-align: center;
}
.code {
  font-size: 3rem;
  font-weight: 700;
  color: var(--accent);
}
</style>
