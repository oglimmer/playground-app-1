<script setup lang="ts">
import { ref } from 'vue'
import { api, errMsg } from '../api'
import { useAsyncData } from '../composables/useAsyncData'
import { useAuthStore } from '../stores/auth'
import type { User } from '../types'

const auth = useAuthStore()
const { data: users, loading, error } = useAsyncData<User[]>(() => api.adminListUsers(), [])
const busyId = ref<string | null>(null)
const actionError = ref<string | null>(null)

function replaceUser(updated: User) {
  users.value = users.value.map((u) => (u.id === updated.id ? updated : u))
}

async function approve(user: User) {
  busyId.value = user.id
  actionError.value = null
  try {
    replaceUser(await api.adminApprove(user.id))
  } catch (e) {
    actionError.value = errMsg(e)
  } finally {
    busyId.value = null
  }
}

async function revoke(user: User) {
  busyId.value = user.id
  actionError.value = null
  try {
    replaceUser(await api.adminRevoke(user.id))
  } catch (e) {
    actionError.value = errMsg(e)
  } finally {
    busyId.value = null
  }
}
</script>

<template>
  <div class="container">
    <h1>User administration</h1>
    <p class="muted">Approve new members so they can read and edit the wiki.</p>

    <div v-if="loading" class="state">Loading users…</div>
    <div v-else-if="error" class="state state-error">{{ error }}</div>
    <template v-else>
      <div v-if="actionError" class="state state-error">{{ actionError }}</div>
      <div class="table-wrap">
        <table class="users">
          <thead>
            <tr>
              <th>User</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th class="right">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td>
                {{ u.displayName }}
                <span v-if="u.id === auth.user?.id" class="muted">(you)</span>
              </td>
              <td class="muted">{{ u.email }}</td>
              <td>{{ u.role }}</td>
              <td>
                <span
                  class="badge"
                  :class="u.status === 'APPROVED' ? 'badge-approved' : 'badge-pending'"
                >
                  {{ u.status === 'APPROVED' ? 'Approved' : 'Pending' }}
                </span>
              </td>
              <td class="right">
                <button
                  v-if="u.status === 'PENDING'"
                  class="btn btn-primary"
                  :disabled="busyId === u.id"
                  @click="approve(u)"
                >
                  Approve
                </button>
                <button
                  v-else
                  class="btn btn-danger"
                  :disabled="busyId === u.id || u.id === auth.user?.id"
                  @click="revoke(u)"
                >
                  Revoke
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<style scoped>
h1 {
  margin: 0 0 0.25rem;
}
.table-wrap {
  overflow-x: auto;
  margin-top: 1.25rem;
}
.users {
  width: 100%;
  border-collapse: collapse;
}
.users th,
.users td {
  text-align: left;
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}
.users th {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--muted);
}
.right {
  text-align: right;
}
</style>
