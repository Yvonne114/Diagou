import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!user.value)
  const userRole = computed(() => user.value?.role)

  async function register(data) {
    const res = await authApi.register(data)
    return res.data
  }

  async function login(email, password) {
    const res = await authApi.login({ email, password })
    user.value = res.data
    return res.data
  }

  async function logout() {
    await authApi.logout()
    user.value = null
  }

  async function fetchUser() {
    try {
      loading.value = true
      const res = await authApi.me()
      user.value = res.data
    } catch {
      user.value = null
    } finally {
      loading.value = false
    }
  }

  async function updateProfile(data) {
    const res = await authApi.updateProfile(data)
    user.value = res.data
    return res.data
  }

  return { user, loading, isLoggedIn, userRole, register, login, logout, fetchUser, updateProfile }
})
