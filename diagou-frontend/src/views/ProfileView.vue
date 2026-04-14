<script setup>
import { ref, computed, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'

const auth = useAuthStore()
const router = useRouter()

const editing = ref(false)
const form = ref({})
const loading = ref(false)
const error = ref('')
const success = ref('')

// Verification state
const emailCodeSent = ref(false)
const emailVerified = ref(false)
const emailCode = ref('')
const emailCooldown = ref(0)
const phoneCodeSent = ref(false)
const phoneVerified = ref(false)
const phoneCode = ref('')
const phoneCooldown = ref(0)

const emailChanged = computed(() => editing.value && form.value.email !== auth.user?.email)
const phoneChanged = computed(() => editing.value && form.value.phone !== (auth.user?.phone || ''))
const canSave = computed(() => {
  if (emailChanged.value && !emailVerified.value) return false
  if (phoneChanged.value && !phoneVerified.value) return false
  return true
})

function startEdit() {
  form.value = {
    fullName: auth.user?.fullName || '',
    displayName: auth.user?.displayName || '',
    email: auth.user?.email || '',
    phone: auth.user?.phone || '',
  }
  resetVerification()
  error.value = ''
  success.value = ''
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  resetVerification()
}

function resetVerification() {
  emailCodeSent.value = false
  emailVerified.value = false
  emailCode.value = ''
  emailCooldown.value = 0
  phoneCodeSent.value = false
  phoneVerified.value = false
  phoneCode.value = ''
  phoneCooldown.value = 0
}

// Reset verification when email/phone changes
watch(() => form.value.email, () => { emailVerified.value = false; emailCodeSent.value = false; emailCode.value = '' })
watch(() => form.value.phone, () => { phoneVerified.value = false; phoneCodeSent.value = false; phoneCode.value = '' })

function startCooldown(type) {
  const set = type === 'email' ? (v) => (emailCooldown.value = v) : (v) => (phoneCooldown.value = v)
  set(30)
  const timer = setInterval(() => {
    const current = type === 'email' ? emailCooldown.value : phoneCooldown.value
    if (current <= 1) { clearInterval(timer); set(0) }
    else set(current - 1)
  }, 1000)
}

async function sendEmailCodeFn() {
  if (!form.value.email || emailCooldown.value > 0) return
  try {
    await authApi.sendCode('email', form.value.email)
    emailCodeSent.value = true
    startCooldown('email')
  } catch { error.value = '發送 Email 驗證碼失敗' }
}

async function verifyEmailCodeFn() {
  try {
    await authApi.verifyCode('email', form.value.email, emailCode.value)
    emailVerified.value = true
    error.value = ''
  } catch (e) { error.value = e.response?.data?.error?.message || '驗證碼錯誤' }
}

async function sendPhoneCodeFn() {
  if (!form.value.phone || phoneCooldown.value > 0) return
  try {
    await authApi.sendCode('phone', form.value.phone)
    phoneCodeSent.value = true
    startCooldown('phone')
  } catch { error.value = '發送手機驗證碼失敗' }
}

async function verifyPhoneCodeFn() {
  try {
    await authApi.verifyCode('phone', form.value.phone, phoneCode.value)
    phoneVerified.value = true
    error.value = ''
  } catch (e) { error.value = e.response?.data?.error?.message || '驗證碼錯誤' }
}

async function save() {
  error.value = ''
  success.value = ''
  loading.value = true
  try {
    const payload = { ...form.value }
    if (emailChanged.value) payload.emailCode = emailCode.value
    if (phoneChanged.value) payload.phoneCode = phoneCode.value
    await auth.updateProfile(payload)
    success.value = '個人資料已更新'
    editing.value = false
    resetVerification()
    setTimeout(() => (success.value = ''), 2000)
  } catch (e) {
    error.value = e.response?.data?.error?.message || '更新失敗'
  } finally { loading.value = false }
}

function formatDate(d) { return d ? new Date(d).toLocaleString('zh-TW') : '-' }
</script>

<template>
  <div class="profile-page">
    <div class="page-header">
      <button class="back-btn" @click="router.push('/')">← 返回</button>
      <h1>個人資料</h1>
    </div>

    <div class="profile-card">
      <div class="card-top">
        <span class="card-title">基本資料</span>
        <button v-if="!editing" class="edit-btn" @click="startEdit">編輯資料</button>
        <button v-else class="edit-btn cancel" @click="cancelEdit">取消</button>
      </div>

      <!-- View Mode -->
      <div v-if="!editing" class="info-grid">
        <div class="info-item"><span class="info-label">姓名</span><span>{{ auth.user?.fullName }}</span></div>
        <div class="info-item"><span class="info-label">顯示名稱</span><span>{{ auth.user?.displayName || '-' }}</span></div>
        <div class="info-item"><span class="info-label">Email</span><span>{{ auth.user?.email }}</span></div>
        <div class="info-item"><span class="info-label">電話</span><span>{{ auth.user?.phone || '-' }}</span></div>
      </div>

      <!-- Edit Mode -->
      <form v-else @submit.prevent="save">
        <div class="form-grid">
          <div class="field">
            <label>姓名</label>
            <input v-model="form.fullName" type="text" />
          </div>
          <div class="field">
            <label>顯示名稱</label>
            <input v-model="form.displayName" type="text" />
          </div>
        </div>

        <!-- Email -->
        <div class="field">
          <label>Email</label>
          <input v-model="form.email" type="email" />
        </div>
        <div v-if="emailChanged" class="verify-row">
          <template v-if="!emailVerified">
            <input v-model="emailCode" type="text" placeholder="輸入 6 位驗證碼" maxlength="6" class="code-input" />
            <button type="button" class="code-btn" :disabled="emailCooldown > 0 || !form.email" @click="sendEmailCodeFn">
              {{ emailCooldown > 0 ? `${emailCooldown}s` : emailCodeSent ? '重新發送' : '發送驗證碼' }}
            </button>
            <button type="button" class="verify-btn" :disabled="emailCode.length < 6" @click="verifyEmailCodeFn">驗證</button>
          </template>
          <span v-else class="verified-tag">✓ 已驗證</span>
        </div>

        <!-- Phone -->
        <div class="field">
          <label>電話</label>
          <input v-model="form.phone" type="tel" />
        </div>
        <div v-if="phoneChanged" class="verify-row">
          <template v-if="!phoneVerified">
            <input v-model="phoneCode" type="text" placeholder="輸入 6 位驗證碼" maxlength="6" class="code-input" />
            <button type="button" class="code-btn" :disabled="phoneCooldown > 0 || !form.phone" @click="sendPhoneCodeFn">
              {{ phoneCooldown > 0 ? `${phoneCooldown}s` : phoneCodeSent ? '重新發送' : '發送驗證碼' }}
            </button>
            <button type="button" class="verify-btn" :disabled="phoneCode.length < 6" @click="verifyPhoneCodeFn">驗證</button>
          </template>
          <span v-else class="verified-tag">✓ 已驗證</span>
        </div>

        <p v-if="error" class="msg error">{{ error }}</p>
        <div class="actions">
          <button type="submit" class="btn-save" :disabled="loading || !canSave">
            {{ loading ? '儲存中...' : '儲存變更' }}
          </button>
        </div>
      </form>

      <p v-if="success" class="msg success outside">{{ success }}</p>
    </div>

    <div class="meta-card">
      <div class="meta-item"><span class="meta-label">角色</span><span class="meta-value role-tag">{{ auth.user?.role }}</span></div>
      <div class="meta-item"><span class="meta-label">帳號狀態</span><span class="meta-value">{{ auth.user?.status }}</span></div>
      <div class="meta-item"><span class="meta-label">註冊時間</span><span class="meta-value">{{ formatDate(auth.user?.createdAt) }}</span></div>
      <div class="meta-item"><span class="meta-label">上次登入</span><span class="meta-value">{{ formatDate(auth.user?.lastLoginAt) }}</span></div>
    </div>
  </div>
</template>

<style scoped>
.profile-page { max-width: 600px; margin: 0 auto; padding: 1.5rem 1rem; }
.page-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.5rem; }
.page-header h1 { font-size: 1.25rem; font-weight: 600; }
.back-btn { background: none; border: none; font-size: 0.875rem; color: #5b7ee5; cursor: pointer; }
.back-btn:hover { text-decoration: underline; }

.profile-card { background: #fff; border-radius: 10px; padding: 1.5rem; box-shadow: 0 1px 4px rgba(0,0,0,0.07); margin-bottom: 1rem; }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.card-title { font-size: 0.9375rem; font-weight: 600; color: #333; }
.edit-btn { background: none; border: 1px solid #5b7ee5; color: #5b7ee5; padding: 0.25rem 0.75rem; border-radius: 4px; font-size: 0.75rem; cursor: pointer; }
.edit-btn:hover { background: #5b7ee5; color: #fff; }
.edit-btn.cancel { border-color: #ccc; color: #999; }
.edit-btn.cancel:hover { background: #eee; color: #666; }

.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.info-item { display: flex; flex-direction: column; }
.info-label { font-size: 0.75rem; color: #999; text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 0.25rem; }
.info-item span:last-child { font-size: 1rem; color: #333; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; margin-bottom: 0.75rem; }
@media (max-width: 500px) { .form-grid, .info-grid { grid-template-columns: 1fr; } }
.field { margin-bottom: 0.875rem; }
.field label { display: block; font-size: 0.8125rem; font-weight: 500; color: #666; margin-bottom: 0.3rem; text-transform: uppercase; letter-spacing: 0.03em; }
.field input { width: 100%; padding: 0.625rem 0.75rem; border: 1px solid #e0e0e0; border-radius: 6px; font-size: 0.9375rem; }
.field input:focus { outline: none; border-color: #5b7ee5; }

.verify-row { display: flex; align-items: center; gap: 0.625rem; margin: -0.25rem 0 0.875rem; }
.code-input { flex: 1; padding: 0.625rem 0.75rem; border: 1px solid #e0e0e0; border-radius: 6px; font-size: 0.9375rem; }
.code-input:focus { outline: none; border-color: #5b7ee5; }
.code-btn { background: #5b7ee5; color: #fff; border: none; padding: 0.625rem 1rem; border-radius: 6px; font-size: 0.875rem; cursor: pointer; white-space: nowrap; }
.code-btn:hover:not(:disabled) { background: #4a6bd4; }
.code-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.verify-btn { background: #333; color: #fff; border: none; padding: 0.625rem 1rem; border-radius: 6px; font-size: 0.875rem; cursor: pointer; white-space: nowrap; }
.verify-btn:hover:not(:disabled) { background: #555; }
.verify-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.verified-tag { color: #2e7d32; font-size: 0.9375rem; font-weight: 500; }

.actions { margin-top: 1rem; display: flex; justify-content: flex-end; }
.btn-save { background: #5b7ee5; color: #fff; border: none; padding: 0.625rem 1.5rem; border-radius: 6px; font-size: 0.9375rem; font-weight: 500; cursor: pointer; }
.btn-save:hover:not(:disabled) { background: #4a6bd4; }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }

.msg { font-size: 0.8125rem; margin-top: 0.5rem; }
.error { color: #d32f2f; }
.success { color: #2e7d32; }
.outside { margin-top: 0.75rem; }

.meta-card { background: #fff; border-radius: 10px; padding: 1rem 1.5rem; box-shadow: 0 1px 4px rgba(0,0,0,0.07); display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
.meta-item { display: flex; flex-direction: column; }
.meta-label { font-size: 0.6875rem; color: #999; text-transform: uppercase; letter-spacing: 0.05em; }
.meta-value { font-size: 0.875rem; font-weight: 500; color: #333; margin-top: 0.125rem; }
.role-tag { color: #5b7ee5; }
</style>
