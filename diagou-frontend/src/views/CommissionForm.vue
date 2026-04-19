<script setup>
import { ref, reactive } from 'vue'

// 表單資料結構
const form = reactive({
  buyerNote: '',
  requiresInspection: false,
  items: [
    {
      productUrl: '',
      productNameJa: '',
      quantity: 1,
      specifications: '',
      itemNote: ''
    }
  ]
})

// 新增商品項目
function addItem() {
  form.items.push({
    productUrl: '',
    productNameJa: '',
    quantity: 1,
    specifications: '',
    itemNote: ''
  })
}

// 移除商品項目
function removeItem(index) {
  if (form.items.length > 1) {
    form.items.splice(index, 1)
  }
}

// 模擬提交
function handleSubmit() {
  console.log('提交的委託資料：', JSON.stringify(form, null, 2))
  alert('表單已生成在 Console，準備串接後端！')
}
</script>

<template>
  <div class="form-container">
    <div class="form-card">
      <h1>建立代購委託</h1>
      
      <form @submit.prevent="handleSubmit">
        <section class="form-section">
          <h3>基本資訊</h3>
          <div class="field">
            <label>委託備註</label>
            <textarea v-model="form.buyerNote" placeholder="例如：請加強包裝..."></textarea>
          </div>
          <div class="checkbox-field">
            <input id="inspection" v-model="form.requiresInspection" type="checkbox" />
            <label for="inspection">需要商品檢查服務</label>
          </div>
        </section>

        <hr />

        <section class="form-section">
          <div class="section-header">
            <h3>商品項目</h3>
            <button type="button" class="btn-secondary" @click="addItem">+ 新增商品</button>
          </div>

          <div v-for="(item, index) in form.items" :key="index" class="item-card">
            <div class="item-header">
              <span>商品 #{{ index + 1 }}</span>
              <button v-if="form.items.length > 1" type="button" class="btn-remove" @click="removeItem(index)">移除</button>
            </div>

            <div class="field">
              <label>商品連結 (URL)</label>
              <input v-model="item.productUrl" type="url" placeholder="https://..." required />
            </div>

            <div class="row">
              <div class="field flex-2">
                <label>日文品名</label>
                <input v-model="item.productNameJa" type="text" placeholder="商品名稱" />
              </div>
              <div class="field flex-1">
                <label>數量</label>
                <input v-model.number="item.quantity" type="number" min="1" required />
              </div>
            </div>

            <div class="field">
              <label>規格說明</label>
              <input v-model="item.specifications" type="text" placeholder="顏色、尺寸、版本等" />
            </div>

            <div class="field">
              <label>單件商品備註</label>
              <input v-model="item.itemNote" type="text" placeholder="對此商品的特殊要求" />
            </div>
          </div>
        </section>

        <button type="submit" class="btn-primary">確認送出委託</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
/* 延續你的登入頁面風格 */
.form-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
  background: #f9f9f9;
}

.form-card {
  background: #fff;
  border-radius: 8px;
  padding: 2rem;
  width: 100%;
  max-width: 600px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

h1 {
  font-size: 1.5rem;
  font-weight: 600;
  margin-bottom: 2rem;
  text-align: center;
}

h3 {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: #333;
}

.form-section {
  margin-bottom: 2rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.field {
  margin-bottom: 1.25rem;
}

.row {
  display: flex;
  gap: 1rem;
}

.flex-1 { flex: 1; }
.flex-2 { flex: 2; }

label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  margin-bottom: 0.35rem;
  color: #555;
}

input, textarea {
  width: 100%;
  padding: 0.625rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 0.9375rem;
}

textarea {
  height: 80px;
  resize: vertical;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.checkbox-field input {
  width: auto;
}

/* 商品卡片樣式 */
.item-card {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 1.25rem;
  margin-bottom: 1.5rem;
  background: #fafafa;
}

.item-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 1rem;
  font-weight: 600;
  color: #777;
}

/* 按鈕樣式 */
button {
  cursor: pointer;
  font-size: 0.9375rem;
  font-weight: 500;
  border-radius: 6px;
  transition: all 0.2s;
}

.btn-primary {
  width: 100%;
  padding: 0.75rem;
  background: #333;
  color: #fff;
  border: none;
  margin-top: 1rem;
}

.btn-primary:hover { background: #555; }

.btn-secondary {
  padding: 0.4rem 0.8rem;
  background: #fff;
  border: 1px solid #333;
  color: #333;
}

.btn-secondary:hover { background: #f0f0f0; }

.btn-remove {
  padding: 0.2rem 0.5rem;
  background: none;
  border: 1px solid #ff4d4f;
  color: #ff4d4f;
  font-size: 0.75rem;
}

.btn-remove:hover {
  background: #ff4d4f;
  color: #fff;
}

hr {
  border: 0;
  border-top: 1px solid #eee;
  margin: 2rem 0;
}
</style>