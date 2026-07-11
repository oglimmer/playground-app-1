<script setup lang="ts">
import { login } from '../api'
</script>

<template>
  <div class="login">
    <div class="card login-card">
      <h1>📚 Wiki</h1>
      <p class="muted">A shared Markdown knowledge base. Sign in with your organisation account.</p>
      <button class="btn btn-primary login-btn" @click="login">Sign in with Keycloak</button>
      <p class="muted small">New accounts need admin approval before you can read or edit pages.</p>
    </div>
  </div>
</template>

<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 1.5rem;
}
.login-card {
  max-width: 420px;
  width: 100%;
  padding: 2.5rem 2rem;
  text-align: center;
}
h1 {
  margin: 0 0 0.5rem;
}
.login-btn {
  width: 100%;
  justify-content: center;
  margin: 1.5rem 0 1rem;
  padding: 0.7rem;
}
.small {
  font-size: 0.85rem;
}
</style>
