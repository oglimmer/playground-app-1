<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, errMsg } from '../api'
import { renderMarkdown } from '../lib/markdown'

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
</style>
