import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      name: 'dashboard',
      component: () => import('@/views/DashboardView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/addresses',
      name: 'addresses',
      component: () => import('@/views/AddressView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

let initialized = false

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (!initialized) {
    initialized = true
    await auth.fetchUser()
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return '/login'
  }

  if (to.meta.guest && auth.isLoggedIn) {
    return '/'
  }
})

export default router
