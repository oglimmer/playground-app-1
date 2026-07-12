<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { toggleTheme } from './theme'

const route = useRoute()
const auth = useAuthStore()

const showChrome = computed(() => !route.meta.hideChrome && auth.isAuthenticated && auth.isApproved)

function onToggleTheme() {
  toggleTheme()
}

async function onLogout() {
  await auth.logout()
}
</script>

<template>
  <header v-if="showChrome" class="app-header">
    <div class="app-header-inner">
      <RouterLink to="/" class="brand">📚 Wiki</RouterLink>
      <nav class="nav">
        <RouterLink to="/">Index</RouterLink>
        <RouterLink to="/new">New page</RouterLink>
        <RouterLink to="/tags">Tags</RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin">Admin</RouterLink>
      </nav>
      <div class="spacer" />
      <button class="btn theme-btn" title="Toggle theme" @click="onToggleTheme">◐</button>
      <span class="muted user-name">{{ auth.user?.displayName }}</span>
      <button class="btn" @click="onLogout">Log out</button>
    </div>
  </header>

  <main>
    <RouterView />
  </main>
</template>

<style scoped>
.app-header {
  border-bottom: 1px solid var(--border);
  background: var(--surface);
  position: sticky;
  top: 0;
  z-index: 10;
}
.app-header-inner {
  max-width: 920px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.7rem 1.25rem;
  flex-wrap: wrap;
}
.brand {
  font-weight: 700;
  color: var(--text);
}
.nav {
  display: flex;
  gap: 1rem;
}
.nav a.router-link-active {
  color: var(--accent);
}
.spacer {
  flex: 1;
}
.user-name {
  font-size: 0.9rem;
}
.theme-btn {
  padding: 0.4rem 0.6rem;
}
</style>
