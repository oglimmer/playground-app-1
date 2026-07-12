<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, errMsg } from '../api'
import { renderMarkdown } from '../lib/markdown'
import type { Attachment } from '../types'

const props = defineProps<{ slug?: string }>()
const router = useRouter()

const isEdit = computed(() => !!props.slug)
const title = ref('')
const content = ref('')
const tags = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const mode = ref<'write' | 'preview' | 'split'>('split')

const attachments = ref<Attachment[]>([])
const uploading = ref(false)
const uploadError = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const preview = computed(() => renderMarkdown(content.value))
const canSave = computed(() => title.value.trim().length > 0 && !saving.value)

onMounted(async () => {
  if (!props.slug) return
  loading.value = true
  try {
    const page = await api.getPage(props.slug)
    title.value = page.title
    content.value = page.content
    tags.value = (page.tags ?? []).join(', ')
    attachments.value = page.attachments ?? []
  } catch (e) {
    error.value = errMsg(e)
  } finally {
    loading.value = false
  }
})

async function save() {
  if (!canSave.value) return
  saving.value = true
  error.value = null
  try {
    const body = {
      title: title.value.trim(),
      content: content.value,
      tags: tags.value
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
    }
    const saved = props.slug
      ? await api.updatePage(props.slug, body)
      : await api.createPage(body)
    router.push(`/pages/${saved.slug}`)
  } catch (e) {
    error.value = errMsg(e)
  } finally {
    saving.value = false
  }
}

function cancel() {
  if (props.slug) router.push(`/pages/${props.slug}`)
  else router.push('/')
}

function isImageType(att: Attachment): boolean {
  return att.contentType.startsWith('image/')
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function onFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !props.slug) return
  uploading.value = true
  uploadError.value = null
  try {
    const att = await api.uploadAttachment(props.slug, file)
    attachments.value.push(att)
    input.value = '' // reset so the same file can be re-selected
  } catch (e) {
    uploadError.value = errMsg(e)
  } finally {
    uploading.value = false
  }
}

async function deleteAttachment(attId: string) {
  if (!props.slug) return
  try {
    await api.deleteAttachment(props.slug, attId)
    attachments.value = attachments.value.filter((a) => a.id !== attId)
  } catch (e) {
    uploadError.value = errMsg(e)
  }
}
</script>

<template>
  <div class="container">
    <div v-if="loading" class="state">Loading…</div>
    <template v-else>
      <div class="edit-head">
        <h1>{{ isEdit ? 'Edit page' : 'New page' }}</h1>
        <div class="actions">
          <button class="btn" @click="cancel">Cancel</button>
          <button class="btn btn-primary" :disabled="!canSave" @click="save">
            {{ saving ? 'Saving…' : 'Save' }}
          </button>
        </div>
      </div>

      <div v-if="error" class="state state-error">{{ error }}</div>

      <input v-model="title" class="input title" type="text" placeholder="Page title">

      <input
        v-model="tags"
        class="input tags-input"
        type="text"
        placeholder="Tags (comma-separated)"
      >

      <div class="tabs">
        <button class="tab" :class="{ active: mode === 'write' }" @click="mode = 'write'">
          Write
        </button>
        <button class="tab" :class="{ active: mode === 'split' }" @click="mode = 'split'">
          Split
        </button>
        <button class="tab" :class="{ active: mode === 'preview' }" @click="mode = 'preview'">
          Preview
        </button>
      </div>

      <div class="editor" :class="mode">
        <textarea
          v-if="mode !== 'preview'"
          v-model="content"
          class="textarea raw"
          placeholder="Write Markdown here…"
          spellcheck="false"
        />
        <!-- eslint-disable-next-line vue/no-v-html -- content is sanitized by DOMPurify -->
        <article v-if="mode !== 'write'" class="markdown-body preview" v-html="preview" />
      </div>

      <section v-if="isEdit" class="attachments-section">
        <h2>Attachments</h2>

        <div v-for="att in attachments" :key="att.id" class="att-row">
          <template v-if="isImageType(att)">
            <img
              :src="api.attachmentDataUrl(props.slug!, att.id)"
              :alt="att.filename"
              class="att-thumb"
            >
          </template>
          <span v-else class="att-icon">📎</span>
          <span class="att-name">{{ att.filename }}</span>
          <span class="att-size muted">{{ formatBytes(att.size) }}</span>
          <button class="btn btn-danger att-del" @click="deleteAttachment(att.id)">✕</button>
        </div>

        <div v-if="uploadError" class="state state-error">{{ uploadError }}</div>

        <label class="upload-label">
          <input
            ref="fileInput"
            type="file"
            class="upload-input"
            :disabled="uploading"
            @change="onFileSelected"
          >
          <span class="btn">{{ uploading ? 'Uploading…' : 'Choose file' }}</span>
        </label>
      </section>
    </template>
  </div>
</template>

<style scoped>
.edit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
h1 {
  margin: 0;
}
.actions {
  display: flex;
  gap: 0.5rem;
}
.title {
  margin: 1.25rem 0 0.75rem;
  font-size: 1.1rem;
}
.tabs {
  display: flex;
  gap: 0.25rem;
  margin-bottom: 0.5rem;
}
.tab {
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--muted);
  padding: 0.35rem 0.8rem;
  border-radius: var(--radius);
}
.tab.active {
  color: var(--accent);
  border-color: var(--accent);
}
.editor {
  display: grid;
  gap: 0.75rem;
  min-height: 60vh;
}
.editor.split {
  grid-template-columns: 1fr 1fr;
}
.editor .raw {
  min-height: 60vh;
  height: 100%;
}
.editor .preview {
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 0.7rem 1rem;
  overflow-y: auto;
  background: var(--surface);
}
@media (max-width: 700px) {
  .editor.split {
    grid-template-columns: 1fr;
  }
}
.attachments-section {
  margin-top: 2rem;
  padding-top: 1.25rem;
  border-top: 1px solid var(--border);
}
.attachments-section h2 {
  font-size: 1.05rem;
  margin: 0 0 0.75rem;
}
.att-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0;
}
.att-thumb {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid var(--border);
}
.att-icon {
  font-size: 1.2rem;
  width: 32px;
  text-align: center;
  flex-shrink: 0;
}
.att-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.9rem;
}
.att-size {
  font-size: 0.8rem;
  flex-shrink: 0;
}
.att-del {
  padding: 0.2rem 0.5rem;
  font-size: 0.85rem;
  flex-shrink: 0;
}
.upload-label {
  display: inline-block;
  margin-top: 0.75rem;
  cursor: pointer;
}
.upload-input {
  display: none;
}
</style>
