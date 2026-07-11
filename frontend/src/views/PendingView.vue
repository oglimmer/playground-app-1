<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const checking = ref(false)

async function checkAgain() {
  checking.value = true
  try {
    await auth.refresh()
    if (auth.isApproved) router.replace('/')
  } finally {
    checking.value = false
  }
}

async function onLogout() {
  await auth.logout()
}
</script>

<template>
  <div class="pending">
    <div class="card pending-card">
      <div class="hourglass">⏳</div>
      <h1>Waiting for approval</h1>
      <p class="muted">
        Thanks for signing in, {{ auth.user?.displayName }}. An administrator needs to approve your
        account before you can access the wiki.
      </p>
      <div class="actions">
        <button class="btn btn-primary" :disabled="checking" @click="checkAgain">
          {{ checking ? 'Checking…' : 'Check again' }}
        </button>
        <button class="btn" @click="onLogout">Log out</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pending {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 1.5rem;
}
.pending-card {
  max-width: 460px;
  width: 100%;
  padding: 2.5rem 2rem;
  text-align: center;
}
.hourglass {
  font-size: 2.5rem;
}
h1 {
  margin: 0.5rem 0;
}
.actions {
  display: flex;
  gap: 0.75rem;
  justify-content: center;
  margin-top: 1.5rem;
}
</style>
