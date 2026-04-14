<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.error?.message || '登入失敗'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-card">
      <h1>登入</h1>
      <form @submit.prevent="handleLogin">
        <div class="field">
          <label for="email">Email</label>
          <input id="email" v-model="email" type="email" required placeholder="your@email.com" />
        </div>
        <div class="field">
          <label for="password">密碼</label>
          <input id="password" v-model="password" type="password" required placeholder="••••••••" />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? '登入中...' : '登入' }}
        </button>
      </form>
      <p class="link">
        還沒有帳號？<RouterLink to="/register">註冊</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
}

.auth-card {
  background: #fff;
  border-radius: 8px;
  padding: 2.5rem;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

h1 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 1.5rem;
  text-align: center;
}

.field {
  margin-bottom: 1rem;
}

label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.25rem;
  color: #555;
}

input {
  width: 100%;
  padding: 0.625rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9375rem;
  transition: border-color 0.2s;
}

input:focus {
  outline: none;
  border-color: #4a90d9;
}

button {
  width: 100%;
  padding: 0.625rem;
  background: #333;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  margin-top: 0.5rem;
  transition: background 0.2s;
}

button:hover:not(:disabled) {
  background: #555;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: #d32f2f;
  font-size: 0.8125rem;
  margin-bottom: 0.5rem;
}

.link {
  text-align: center;
  margin-top: 1rem;
  font-size: 0.875rem;
  color: #777;
}

.link a {
  color: #4a90d9;
  text-decoration: none;
}

.link a:hover {
  text-decoration: underline;
}
</style>
