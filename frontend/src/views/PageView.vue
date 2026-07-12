<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api, errMsg } from '../api'
import { useAsyncData } from '../composables/useAsyncData'
import { renderMarkdown } from '../lib/markdown'
import type { Attachment, Page } from '../types'

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

function isImage(att: Attachment): boolean {
  return att.contentType.startsWith('image/')
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
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

      <section v-if="page.attachments && page.attachments.length" class="attachments">
        <h2>Attachments</h2>
        <div class="attachment-gallery">
          <div v-for="att in page.attachments" :key="att.id" class="attachment-item">
            <template v-if="isImage(att)">
              <img
                :src="api.attachmentDataUrl(page.slug, att.id)"
                :alt="att.filename"
                class="attachment-image"
                loading="lazy"
              >
            </template>
            <template v-else>
              <a
                :href="api.attachmentDataUrl(page.slug, att.id)"
                class="attachment-file"
                :title="att.filename"
              >
                <span class="attachment-icon">📎</span>
                <span class="attachment-name">{{ att.filename }}</span>
                <span class="attachment-size muted">{{ formatBytes(att.size) }}</span>
              </a>
            </template>
          </div>
        </div>
      </section>
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
.attachments {
  margin-top: 2.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border);
}
.attachments h2 {
  font-size: 1.1rem;
  margin: 0 0 1rem;
}
.attachment-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
}
.attachment-item {
  display: flex;
  align-items: center;
}
.attachment-image {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius);
  border: 1px solid var(--border);
}
.attachment-file {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  text-decoration: none;
  color: var(--text);
  width: 100%;
}
.attachment-file:hover {
  border-color: var(--accent);
  text-decoration: none;
}
.attachment-icon {
  font-size: 1.25rem;
  flex-shrink: 0;
}
.attachment-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.9rem;
}
.attachment-size {
  font-size: 0.8rem;
  flex-shrink: 0;
}
</style>
