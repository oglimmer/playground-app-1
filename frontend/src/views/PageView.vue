<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, errMsg } from '../api'
import { useAsyncData } from '../composables/useAsyncData'
import { renderMarkdown } from '../lib/markdown'
import type { Page } from '../types'

const props = defineProps<{ slug: string }>()
const router = useRouter()

const { data: page, loading, error, reload } = useAsyncData<Page | null>(
  () => api.getPage(props.slug),
  null,
)

watch(() => props.slug, reload)

const showRaw = ref(false)
const html = computed(() => (page.value ? renderMarkdown(page.value.content) : ''))

const confirmingDelete = ref(false)
const deleteError = ref<string | null>(null)

async function doDelete() {
  deleteError.value = null
  try {
    await api.deletePage(props.slug)
    router.push('/')
  } catch (e) {
    deleteError.value = errMsg(e)
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString()
}
</script>

<template>
  <div class="container">
    <div v-if="loading" class="state">Loading…</div>
    <div v-else-if="error" class="state state-error">{{ error }}</div>
    <template v-else-if="page">
      <div class="page-head">
        <h1>{{ page.title }}</h1>
        <div class="actions">
          <button class="btn" @click="showRaw = !showRaw">
            {{ showRaw ? 'Rendered' : 'Raw' }}
          </button>
          <RouterLink :to="`/pages/${page.slug}/edit`" class="btn">Edit</RouterLink>
          <button v-if="!confirmingDelete" class="btn btn-danger" @click="confirmingDelete = true">
            Delete
          </button>
          <template v-else>
            <span class="muted">Delete?</span>
            <button class="btn btn-danger" @click="doDelete">Yes</button>
            <button class="btn" @click="confirmingDelete = false">No</button>
          </template>
        </div>
      </div>

      <p class="muted meta">Last updated {{ formatDate(page.updatedAt) }} by {{ page.updatedBy }}</p>
      <div v-if="deleteError" class="state state-error">{{ deleteError }}</div>

      <pre v-if="showRaw" class="raw">{{ page.content }}</pre>
      <!-- eslint-disable-next-line vue/no-v-html -- content is sanitized by DOMPurify -->
      <article v-else class="markdown-body" v-html="html" />
    </template>
  </div>
</template>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
h1 {
  margin: 0;
}
.actions {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  flex-wrap: wrap;
}
.meta {
  font-size: 0.85rem;
  margin-top: 0.35rem;
}
.raw {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 1rem;
  overflow-x: auto;
  font-family: var(--mono);
  font-size: 0.9rem;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
