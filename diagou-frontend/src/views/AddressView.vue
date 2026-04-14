<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { addressApi } from '@/api/address'
import zipcodeData from '@/assets/tw-zipcode.json'

const router = useRouter()
const addresses = ref([])
const loading = ref(false)
const error = ref('')

const showForm = ref(false)
const editingId = ref(null)
const form = ref(emptyForm())
const formError = ref('')
const formLoading = ref(false)
const smartText = ref('')
const smartParsed = ref(false)

function emptyForm() {
  return { label: '', recipientName: '', phone: '', postalCode: '', city: '', district: '', addressLine: '' }
}

// Zipcode helpers
const cities = computed(() => Object.keys(zipcodeData))
const districts = computed(() => {
  if (!form.value.city || !zipcodeData[form.value.city]) return []
  return Object.keys(zipcodeData[form.value.city])
})

watch(() => form.value.city, () => {
  form.value.district = ''
  form.value.postalCode = ''
})

watch(() => form.value.district, (district) => {
  if (form.value.city && district && zipcodeData[form.value.city]?.[district]) {
    form.value.postalCode = zipcodeData[form.value.city][district]
  } else {
    form.value.postalCode = ''
  }
})

async function fetchAddresses() {
  loading.value = true
  try {
    const res = await addressApi.list()
    addresses.value = res.data
  } catch { error.value = '載入地址失敗' }
  finally { loading.value = false }
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  formError.value = ''
  smartText.value = ''
  smartParsed.value = false
  showForm.value = true
}

function openEdit(addr) {
  editingId.value = addr.id
  form.value = {
    label: addr.label || '',
    recipientName: addr.recipientName,
    phone: addr.phone,
    postalCode: addr.postalCode,
    city: addr.city,
    district: addr.district,
    addressLine: addr.addressLine,
  }
  formError.value = ''
  smartText.value = ''
  smartParsed.value = false
  showForm.value = true
}

function cancelForm() {
  showForm.value = false
  editingId.value = null
}

async function saveForm() {
  formError.value = ''
  formLoading.value = true
  try {
    if (editingId.value) {
      await addressApi.update(editingId.value, form.value)
    } else {
      await addressApi.create(form.value)
    }
    showForm.value = false
    await fetchAddresses()
  } catch (e) {
    formError.value = e.response?.data?.error?.message || '儲存失敗'
  } finally { formLoading.value = false }
}

// Smart fill
function parseAddress(text) {
  const result = { recipientName: '', phone: '', city: '', district: '', addressLine: '' }

  const phoneMatch = text.match(/(?:09\d{8}|(?:\(0\d\)|0\d{1,2})[-\s]?\d{3,4}[-\s]?\d{4})/)
  if (phoneMatch) result.phone = phoneMatch[0].replace(/[\s\-()]/g, '')

  const cityKeys = Object.keys(zipcodeData)
  const cityMatch = text.match(new RegExp(`(${cityKeys.join('|')})`))
  if (cityMatch) {
    result.city = cityMatch[1]
    const districtKeys = Object.keys(zipcodeData[result.city])
    const districtMatch = text.match(new RegExp(`(${districtKeys.join('|')})`))
    if (districtMatch) result.district = districtMatch[1]
  }

  if (result.district) {
    const afterDistrict = text.split(result.district)[1]
    if (afterDistrict) result.addressLine = afterDistrict.trim().replace(/\s+/g, '').replace(/^\d{3,5}/, '')
  }

  let remaining = text
    .replace(result.phone || '', '')
    .replace(result.city || '', '')
    .replace(result.district || '', '')
    .replace(result.addressLine || '', '')
    .replace(/\d{3,5}/, '')
    .trim()
  const nameMatch = remaining.match(/[\u4e00-\u9fa5]{2,5}/)
  if (nameMatch) result.recipientName = nameMatch[0]

  return result
}

function applySmartFill() {
  if (!smartText.value.trim()) return
  const parsed = parseAddress(smartText.value)
  form.value = { ...form.value, ...parsed }
  // postalCode will be set by the district watcher
  smartParsed.value = true
}

async function removeAddress(id) {
  if (!confirm('確定要刪除這個地址嗎？')) return
  try {
    await addressApi.remove(id)
    await fetchAddresses()
  } catch { error.value = '刪除失敗' }
}

async function setDefault(id) {
  try {
    await addressApi.setDefault(id)
    await fetchAddresses()
  } catch { error.value = '設定失敗' }
}

onMounted(fetchAddresses)
</script>

<template>
  <div class="address-page">
    <div class="page-header">
      <button class="back-btn" @click="router.push('/')">← 返回</button>
      <h1>收件地址</h1>
      <button class="add-btn" @click="openCreate">+ 新增地址</button>
    </div>

    <p v-if="error" class="msg error">{{ error }}</p>
    <div v-if="loading" class="empty">載入中...</div>

    <div v-else-if="addresses.length === 0 && !showForm" class="empty">
      <p>尚無收件地址</p>
      <button class="btn-primary" @click="openCreate">新增第一個地址</button>
    </div>

    <div v-else class="address-list">
      <div v-for="addr in addresses" :key="addr.id" class="address-card" :class="{ default: addr.isDefault }">
        <div class="card-top">
          <div class="name-row">
            <span class="recipient">{{ addr.recipientName }}</span>
            <span v-if="addr.label" class="label-tag">{{ addr.label }}</span>
            <span v-if="addr.isDefault" class="default-tag">預設</span>
          </div>
          <div class="card-actions">
            <button v-if="!addr.isDefault" class="action-btn" @click="setDefault(addr.id)">設為預設</button>
            <button class="action-btn" @click="openEdit(addr)">編輯</button>
            <button class="action-btn danger" @click="removeAddress(addr.id)">刪除</button>
          </div>
        </div>
        <p class="addr-phone">{{ addr.phone }}</p>
        <p class="addr-line">{{ addr.postalCode }} {{ addr.city }}{{ addr.district }}{{ addr.addressLine }}</p>
      </div>
    </div>

    <!-- Form Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="cancelForm">
      <div class="modal">
        <div class="modal-header">
          <h2>{{ editingId ? '編輯地址' : '新增地址' }}</h2>
          <button class="close-btn" @click="cancelForm">&times;</button>
        </div>
        <form @submit.prevent="saveForm">
          <!-- Smart Fill -->
          <div class="smart-fill-box">
            <label class="smart-label">智能填入</label>
            <p class="smart-hint">貼上姓名、電話和地址，系統自動識別填入各欄位</p>
            <textarea
              v-model="smartText"
              placeholder="例：王小明 0912345678&#10;台北市大安區忠孝東路四段1號5樓"
              rows="3"
            />
            <button type="button" class="btn-smart" @click="applySmartFill">自動填入</button>
            <p v-if="smartParsed" class="smart-ok">✓ 已自動填入，請確認下方欄位</p>
          </div>
          <div class="divider"><span>或手動填寫</span></div>

          <div class="field">
            <label>標籤（選填）</label>
            <input v-model="form.label" type="text" placeholder="家、公司..." />
          </div>
          <div class="form-row">
            <div class="field">
              <label>收件人姓名 *</label>
              <input v-model="form.recipientName" type="text" required />
            </div>
            <div class="field">
              <label>電話 *</label>
              <input v-model="form.phone" type="tel" required />
            </div>
          </div>

          <!-- City / District / Postal -->
          <div class="form-row">
            <div class="field">
              <label>縣市 *</label>
              <select v-model="form.city" required>
                <option value="" disabled>請選擇縣市</option>
                <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
              </select>
            </div>
            <div class="field">
              <label>區域 *</label>
              <select v-model="form.district" required :disabled="!form.city">
                <option value="" disabled>{{ form.city ? '請選擇區域' : '請先選縣市' }}</option>
                <option v-for="d in districts" :key="d" :value="d">{{ d }}</option>
              </select>
            </div>
            <div class="field narrow">
              <label>郵遞區號</label>
              <input v-model="form.postalCode" type="text" readonly class="readonly" />
            </div>
          </div>

          <div class="field">
            <label>街道地址 *</label>
            <input v-model="form.addressLine" type="text" required placeholder="路/街/巷/弄/號/樓" />
          </div>
          <p v-if="formError" class="msg error">{{ formError }}</p>
          <div class="modal-footer">
            <button type="button" class="btn-secondary" @click="cancelForm">取消</button>
            <button type="submit" class="btn-primary" :disabled="formLoading">
              {{ formLoading ? '儲存中...' : '儲存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.address-page { max-width: 700px; margin: 0 auto; padding: 1.5rem 1rem; }
.page-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 1.5rem; }
.page-header h1 { font-size: 1.25rem; font-weight: 600; flex: 1; }
.back-btn { background: none; border: none; font-size: 0.875rem; color: #5b7ee5; cursor: pointer; }
.back-btn:hover { text-decoration: underline; }
.add-btn { background: #5b7ee5; color: #fff; border: none; padding: 0.4rem 0.875rem; border-radius: 6px; font-size: 0.875rem; cursor: pointer; }
.add-btn:hover { background: #4a6bd4; }

.empty { text-align: center; padding: 3rem 0; color: #aaa; }
.empty p { margin-bottom: 1rem; }
.address-list { display: flex; flex-direction: column; gap: 0.75rem; }

.address-card { background: #fff; border-radius: 10px; padding: 1.25rem; box-shadow: 0 1px 3px rgba(0,0,0,0.07); border-left: 4px solid #e0e0e0; }
.address-card.default { border-left-color: #5b7ee5; }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.5rem; }
.name-row { display: flex; align-items: center; gap: 0.5rem; }
.recipient { font-weight: 600; font-size: 1rem; }
.label-tag { background: #f0f0f0; color: #666; font-size: 0.6875rem; padding: 0.125rem 0.4rem; border-radius: 3px; }
.default-tag { background: #e8eeff; color: #5b7ee5; font-size: 0.6875rem; padding: 0.125rem 0.4rem; border-radius: 3px; font-weight: 500; }
.card-actions { display: flex; gap: 0.375rem; }
.action-btn { background: none; border: 1px solid #e0e0e0; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.75rem; cursor: pointer; color: #555; }
.action-btn:hover { border-color: #5b7ee5; color: #5b7ee5; }
.action-btn.danger:hover { border-color: #d32f2f; color: #d32f2f; }
.addr-phone { font-size: 0.875rem; color: #555; margin: 0.125rem 0; }
.addr-line { font-size: 0.875rem; color: #333; margin: 0; }

.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 1rem; }
.modal { background: #fff; border-radius: 10px; padding: 1.5rem; width: 100%; max-width: 500px; box-shadow: 0 4px 16px rgba(0,0,0,0.12); max-height: 90vh; overflow-y: auto; }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.modal-header h2 { font-size: 1.125rem; font-weight: 600; }
.close-btn { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #999; }
.close-btn:hover { color: #333; }

.field { margin-bottom: 0.75rem; }
.field label { display: block; font-size: 0.8125rem; color: #555; margin-bottom: 0.25rem; }
.field input, .field select {
  width: 100%; padding: 0.5rem 0.625rem; border: 1px solid #e0e0e0;
  border-radius: 6px; font-size: 0.9375rem; background: #fff;
}
.field input:focus, .field select:focus { outline: none; border-color: #5b7ee5; }
.field select:disabled { background: #f5f5f5; color: #aaa; cursor: not-allowed; }
.field input.readonly { background: #f5f5f5; color: #666; cursor: default; }
.field.narrow { max-width: 100px; }

.form-row { display: flex; gap: 0.625rem; }
.form-row .field { flex: 1; }

.modal-footer { display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 1rem; }
.btn-primary { background: #5b7ee5; color: #fff; border: none; padding: 0.5rem 1.25rem; border-radius: 6px; font-size: 0.875rem; font-weight: 500; cursor: pointer; }
.btn-primary:hover:not(:disabled) { background: #4a6bd4; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-secondary { background: none; border: 1px solid #ddd; padding: 0.5rem 1rem; border-radius: 6px; font-size: 0.875rem; cursor: pointer; color: #555; }
.btn-secondary:hover { border-color: #999; }

.msg { font-size: 0.8125rem; margin-bottom: 0.5rem; }
.error { color: #d32f2f; }

.smart-fill-box { background: #f7f8fc; border-radius: 8px; padding: 1rem; margin-bottom: 0.75rem; border: 1px solid #e8eeff; }
.smart-label { display: block; font-size: 0.8125rem; font-weight: 600; color: #5b7ee5; margin-bottom: 0.25rem; }
.smart-hint { font-size: 0.75rem; color: #999; margin-bottom: 0.5rem; }
.smart-fill-box textarea { width: 100%; padding: 0.5rem 0.625rem; border: 1px solid #e0e0e0; border-radius: 6px; font-size: 0.875rem; resize: vertical; font-family: inherit; margin-bottom: 0.5rem; }
.smart-fill-box textarea:focus { outline: none; border-color: #5b7ee5; }
.btn-smart { background: #5b7ee5; color: #fff; border: none; padding: 0.375rem 0.875rem; border-radius: 6px; font-size: 0.8125rem; cursor: pointer; }
.btn-smart:hover { background: #4a6bd4; }
.smart-ok { color: #2e7d32; font-size: 0.75rem; margin-top: 0.375rem; }

.divider { display: flex; align-items: center; gap: 0.5rem; margin: 0.75rem 0; color: #bbb; font-size: 0.75rem; }
.divider::before, .divider::after { content: ''; flex: 1; height: 1px; background: #e0e0e0; }
</style>
