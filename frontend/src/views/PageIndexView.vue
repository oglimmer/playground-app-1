<script setup lang="ts">
import { computed, ref } from 'vue'
import { api } from '../api'
import { useAsyncData } from '../composables/useAsyncData'
import type { PageSummary } from '../types'

const { data: pages, loading, error } = useAsyncData<PageSummary[]>(() => api.listPages(), [])
const query = ref('')

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return pages.value
  return pages.value.filter((p) => p.title.toLowerCase().includes(q))
})

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}
</script>

<template>
  <div class="container">
    <div class="page-head">
      <h1>All pages</h1>
      <RouterLink to="/new" class="btn btn-primary">New page</RouterLink>
    </div>

    <input
      v-model="query"
      class="input search"
      type="search"
      placeholder="Filter pages by title…"
    >

    <div v-if="loading" class="state">Loading pages…</div>
    <div v-else-if="error" class="state state-error">{{ error }}</div>
    <div v-else-if="filtered.length === 0 && pages.length === 0" class="state">
      No pages yet. <RouterLink to="/new">Create the first one</RouterLink>.
    </div>
    <div v-else-if="filtered.length === 0" class="state">No pages match “{{ query }}”.</div>

    <ul v-else class="page-list">
      <li v-for="p in filtered" :key="p.slug" class="card page-row">
        <RouterLink :to="`/pages/${p.slug}`" class="page-title">{{ p.title }}</RouterLink>
        <span class="muted meta">Updated {{ formatDate(p.updatedAt) }} by {{ p.updatedBy }}</span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
h1 {
  margin: 0;
}
.search {
  margin: 1.25rem 0;
}
.page-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.page-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.85rem 1rem;
  flex-wrap: wrap;
}
.page-title {
  font-weight: 600;
  font-size: 1.05rem;
}
.meta {
  font-size: 0.85rem;
}
</style>
