<script setup lang="ts">
import { api } from '../api'
import { useAsyncData } from '../composables/useAsyncData'

const { data: tags, loading, error } = useAsyncData<string[]>(() => api.listTags(), [])
</script>

<template>
  <div class="container">
    <div class="page-head">
      <h1>All tags</h1>
    </div>

    <div v-if="loading" class="state">Loading tags…</div>
    <div v-else-if="error" class="state state-error">{{ error }}</div>
    <div v-else-if="tags.length === 0" class="state">No tags yet.</div>

    <ul v-else class="tag-list">
      <li v-for="t in tags" :key="t">
        <RouterLink :to="`/tags/${encodeURIComponent(t)}`" class="tag">#{{ t }}</RouterLink>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
h1 {
  margin: 0;
}
.tag-list {
  list-style: none;
  margin: 1.25rem 0 0;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 0.6rem;
}
.tag {
  display: inline-block;
  border: 1px solid var(--border);
  background: var(--surface);
  border-radius: var(--radius);
  padding: 0.35rem 0.75rem;
  font-weight: 600;
}
</style>
