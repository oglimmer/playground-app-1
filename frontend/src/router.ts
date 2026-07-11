import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    hideChrome?: boolean
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'index',
      component: () => import('./views/PageIndexView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/new',
      name: 'new',
      component: () => import('./views/PageEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/pages/:slug',
      name: 'page',
      component: () => import('./views/PageView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/pages/:slug/edit',
      name: 'page-edit',
      component: () => import('./views/PageEditView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('./views/AdminView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('./views/LoginView.vue'),
      meta: { hideChrome: true },
    },
    {
      path: '/pending',
      name: 'pending',
      component: () => import('./views/PendingView.vue'),
      meta: { hideChrome: true },
    },
    {
      path: '/error',
      name: 'error',
      component: () => import('./views/ErrorView.vue'),
      props: (route) => ({ code: Number(route.query.code) || 500 }),
      meta: { hideChrome: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('./views/ErrorView.vue'),
      props: { code: 404 },
      meta: { hideChrome: true },
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  // The error screen must always render, even for anonymous users.
  if (to.name === 'error' || to.name === 'not-found') return true

  try {
    await auth.ensureUser()
  } catch {
    return { path: '/error', query: { code: '500' } }
  }

  if (to.name === 'login') {
    return auth.isAuthenticated ? { path: '/' } : true
  }

  if (!auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // Authenticated but not yet approved by an admin.
  if (!auth.isApproved) {
    return to.name === 'pending' ? true : { path: '/pending' }
  }

  // Approved users have no business on the holding screen.
  if (to.name === 'pending') return { path: '/' }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { path: '/error', query: { code: '403' } }
  }

  return true
})

export default router
