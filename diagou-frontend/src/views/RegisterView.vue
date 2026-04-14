<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { authApi } from '@/api/auth'

const router = useRouter()
const auth = useAuthStore()

// step: 'form' | 'email' | 'phone' | 'success'
const step = ref('form')
const form = ref({ email: '', password: '', fullName: '', phone: '' })
const error = ref('')
const loading = ref(false)

// verification
const emailCode = ref('')
const emailCooldown = ref(0)
const emailVerified = ref(false)
const phoneCode = ref('')
const phoneCooldown = ref(0)
const phoneVerified = ref(false)

const hasPhone = computed(() => form.value.phone && form.value.phone.trim().length > 0)

function startCooldown(type) {
  const set = type === 'email' ? v => (emailCooldown.value = v) : v => (phoneCooldown.value = v)
  set(30)
  const timer = setInterval(() => {
    const cur = type === 'email' ? emailCooldown.value : phoneCooldown.value
    if (cur <= 1) { clearInterval(timer); set(0) } else set(cur - 1)
  }, 1000)
}

async function goToEmailStep() {
  if (!form.value.fullName || !form.value.email || !form.value.password) {
    error.value = '請填寫所有必填欄位'
    return
  }
  error.value = ''
  loading.value = true
  try {
    await authApi.sendCode('email', form.value.email)
    startCooldown('email')
    step.value = 'email'
  } catch { error.value = '發送 Email 驗證碼失敗，請稍後再試' }
  finally { loading.value = false }
}

async function resendEmailCode() {
  if (emailCooldown.value > 0) return
  await authApi.sendCode('email', form.value.email)
  startCooldown('email')
}

async function verifyEmail() {
  error.value = ''
  try {
    await authApi.verifyCode('email', form.value.email, emailCode.value)
    emailVerified.value = true
  } catch (e) { error.value = e.response?.data?.error?.message || '驗證碼錯誤' }
}

async function proceedAfterEmail() {
  if (hasPhone.value) {
    loading.value = true
    try {
      await authApi.sendCode('phone', form.value.phone)
      startCooldown('phone')
      step.value = 'phone'
    } catch { error.value = '發送手機驗證碼失敗' }
    finally { loading.value = false }
  } else {
    await doRegister()
  }
}

async function resendPhoneCode() {
  if (phoneCooldown.value > 0) return
  await authApi.sendCode('phone', form.value.phone)
  startCooldown('phone')
}

async function verifyPhone() {
  error.value = ''
  try {
    await authApi.verifyCode('phone', form.value.phone, phoneCode.value)
    phoneVerified.value = true
  } catch (e) { error.value = e.response?.data?.error?.message || '驗證碼錯誤' }
}

async function doRegister() {
  error.value = ''
  loading.value = true
  try {
    await auth.register({
      ...form.value,
      emailCode: emailCode.value,
      phoneCode: phoneCode.value || undefined,
    })
    step.value = 'success'
    setTimeout(() => router.push('/login'), 2500)
  } catch (e) {
    error.value = e.response?.data?.error?.message || '註冊失敗'
    step.value = 'form'
  } finally { loading.value = false }
}
</script>

<template>
  <div class="auth-container">
    <div class="auth-card">

      <!-- Step: Form -->
      <template v-if="step === 'form'">
        <h1>註冊</h1>
        <form @submit.prevent="goToEmailStep">
          <div class="field">
            <label>姓名</label>
            <input v-model="form.fullName" type="text" required placeholder="王小明" />
          </div>
          <div class="field">
            <label>Email</label>
            <input v-model="form.email" type="email" required placeholder="your@email.com" />
          </div>
          <div class="field">
            <label>密碼</label>
            <input v-model="form.password" type="password" required placeholder="至少 6 個字元" />
          </div>
          <div class="field">
            <label>電話（選填）</label>
            <input v-model="form.phone" type="tel" placeholder="0912345678" />
          </div>
          <p v-if="error" class="error">{{ error }}</p>
          <button type="submit" :disabled="loading">
            {{ loading ? '處理中...' : '下一步' }}
          </button>
        </form>
        <p class="link">已有帳號？<RouterLink to="/login">登入</RouterLink></p>
      </template>

      <!-- Step: Email Verification -->
      <template v-else-if="step === 'email'">
        <div class="step-icon">✉️</div>
        <h1>驗證電子郵件</h1>
        <p class="step-desc">驗證碼已發送至後端 console，請複製貼上</p>
        <p class="step-target">{{ form.email }}</p>

        <div v-if="!emailVerified">
          <div class="field">
            <label>驗證碼</label>
            <input v-model="emailCode" type="text" placeholder="請輸入 6 位驗證碼" maxlength="6" />
          </div>
          <p v-if="error" class="error">{{ error }}</p>
          <div class="btn-group">
            <button class="btn-secondary" :disabled="emailCooldown > 0" @click="resendEmailCode">
              {{ emailCooldown > 0 ? `重新發送 (${emailCooldown}s)` : '重新發送' }}
            </button>
            <button class="btn-primary" :disabled="emailCode.length < 6" @click="verifyEmail">
              驗證
            </button>
          </div>
        </div>

        <div v-else class="verified-section">
          <p class="verified-msg">✓ Email 驗證成功</p>
          <button class="btn-primary full" :disabled="loading" @click="proceedAfterEmail">
            {{ loading ? '處理中...' : hasPhone ? '下一步：驗證手機號碼' : '完成註冊' }}
          </button>
        </div>
      </template>

      <!-- Step: Phone Verification -->
      <template v-else-if="step === 'phone'">
        <div class="step-icon">📱</div>
        <h1>驗證手機號碼</h1>
        <p class="step-desc">驗證碼已發送至後端 console，請複製貼上</p>
        <p class="step-target">{{ form.phone }}</p>

        <div v-if="!phoneVerified">
          <div class="field">
            <label>驗證碼</label>
            <input v-model="phoneCode" type="text" placeholder="請輸入 6 位驗證碼" maxlength="6" />
          </div>
          <p v-if="error" class="error">{{ error }}</p>
          <div class="btn-group">
            <button class="btn-secondary" :disabled="phoneCooldown > 0" @click="resendPhoneCode">
              {{ phoneCooldown > 0 ? `重新發送 (${phoneCooldown}s)` : '重新發送' }}
            </button>
            <button class="btn-primary" :disabled="phoneCode.length < 6" @click="verifyPhone">
              驗證
            </button>
          </div>
        </div>

        <div v-else class="verified-section">
          <p class="verified-msg">✓ 手機驗證成功</p>
          <button class="btn-primary full" :disabled="loading" @click="doRegister">
            {{ loading ? '註冊中...' : '完成註冊' }}
          </button>
        </div>
      </template>

      <!-- Step: Success -->
      <template v-else-if="step === 'success'">
        <div class="success-section">
          <div class="step-icon">🎉</div>
          <h1>註冊成功</h1>
          <p class="redirecting">正在跳至登入頁面<span class="dots"><span>.</span><span>.</span><span>.</span></span></p>
        </div>
      </template>

    </div>
  </div>
</template>

<style scoped>
.auth-container {
  min-height: 100vh; display: flex; align-items: center;
  justify-content: center; padding: 1rem; background: #f7f8fc;
}
.auth-card {
  background: #fff; border-radius: 10px; padding: 2.5rem;
  width: 100%; max-width: 420px; box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
h1 { font-size: 1.5rem; font-weight: 600; margin-bottom: 1.25rem; text-align: center; color: #333; }

.field { margin-bottom: 0.875rem; }
label { display: block; font-size: 0.8125rem; font-weight: 500; margin-bottom: 0.3rem; color: #555; }
input {
  width: 100%; padding: 0.625rem 0.75rem; border: 1px solid #e0e0e0;
  border-radius: 6px; font-size: 0.9375rem; transition: border-color 0.2s;
}
input:focus { outline: none; border-color: #5b7ee5; }

button[type="submit"] {
  width: 100%; padding: 0.625rem; background: #333; color: #fff;
  border: none; border-radius: 6px; font-size: 0.9375rem; font-weight: 500;
  cursor: pointer; margin-top: 0.5rem; transition: background 0.2s;
}
button[type="submit"]:hover:not(:disabled) { background: #555; }
button[type="submit"]:disabled { opacity: 0.5; cursor: not-allowed; }

.error { color: #d32f2f; font-size: 0.8125rem; margin-bottom: 0.5rem; }
.link { text-align: center; margin-top: 1rem; font-size: 0.875rem; color: #777; }
.link a { color: #5b7ee5; text-decoration: none; }
.link a:hover { text-decoration: underline; }

/* Verification steps */
.step-icon { font-size: 2.5rem; text-align: center; margin-bottom: 0.5rem; }
.step-desc { text-align: center; color: #777; font-size: 0.875rem; margin-bottom: 0.25rem; }
.step-target { text-align: center; font-weight: 600; color: #333; margin-bottom: 1.25rem; }

.btn-group { display: flex; gap: 0.625rem; margin-top: 0.5rem; }
.btn-primary {
  flex: 1; background: #5b7ee5; color: #fff; border: none;
  padding: 0.625rem 1rem; border-radius: 6px; font-size: 0.9375rem;
  font-weight: 500; cursor: pointer; transition: background 0.2s;
}
.btn-primary:hover:not(:disabled) { background: #4a6bd4; }
.btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary.full { width: 100%; flex: none; }
.btn-secondary {
  flex: 1; background: none; border: 1px solid #ddd; color: #555;
  padding: 0.625rem 1rem; border-radius: 6px; font-size: 0.875rem;
  cursor: pointer; transition: all 0.2s;
}
.btn-secondary:hover:not(:disabled) { border-color: #999; color: #333; }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }

.verified-section { text-align: center; }
.verified-msg { color: #2e7d32; font-size: 1rem; font-weight: 500; margin-bottom: 1rem; }

/* Success */
.success-section { text-align: center; padding: 1rem 0; }
.redirecting { color: #555; font-size: 0.9375rem; margin-top: 0.5rem; }
.dots span {
  display: inline-block;
  animation: blink 1.2s infinite;
  font-size: 1.25rem;
  line-height: 1;
}
.dots span:nth-child(2) { animation-delay: 0.2s; }
.dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}
</style>
