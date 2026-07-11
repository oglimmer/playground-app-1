import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api, errStatus, logout as apiLogout } from '../api'
import type { User } from '../types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const loaded = ref(false)
  let inflight: Promise<User | null> | null = null

  const isAuthenticated = computed(() => user.value !== null)
  const isApproved = computed(() => user.value?.status === 'APPROVED')
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function fetchMe(): Promise<User | null> {
    try {
      user.value = await api.me()
    } catch (e) {
      if (errStatus(e) === 401) {
        user.value = null
      } else {
        throw e
      }
    } finally {
      loaded.value = true
    }
    return user.value
  }

  /** One-shot session load; concurrent callers share the same request. */
  function ensureUser(): Promise<User | null> {
    if (loaded.value) return Promise.resolve(user.value)
    if (!inflight) {
      inflight = fetchMe().finally(() => {
        inflight = null
      })
    }
    return inflight
  }

  /** Force a refetch (e.g. after an admin approves the current pending user). */
  async function refresh(): Promise<User | null> {
    loaded.value = false
    return ensureUser()
  }

  async function logout(): Promise<void> {
    await apiLogout()
    user.value = null
  }

  return { user, loaded, isAuthenticated, isApproved, isAdmin, ensureUser, refresh, logout }
})
