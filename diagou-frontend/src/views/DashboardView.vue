<script setup>
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app">
    <nav class="navbar">
      <div class="nav-left">
        <h1 class="brand">代購平台</h1>
      </div>
      <div class="nav-right">
        <RouterLink to="/profile" class="nav-link">個人資料</RouterLink>
        <RouterLink to="/addresses" class="nav-link">地址管理</RouterLink>
        <span class="nav-divider"></span>
        <span class="nav-user">{{ auth.user?.displayName || auth.user?.fullName }}</span>
        <button class="nav-btn logout" @click="handleLogout">登出</button>
      </div>
    </nav>

    <main class="main">
      <div class="greeting">
        <h2>Hi, {{ auth.user?.displayName || auth.user?.fullName }} 👋</h2>
      </div>

      <div class="card-grid">
        <div class="card commission-card" @click="router.push('/commissionForm')">
          <div class="card-icon">📋</div>
          <div class="card-body">
            <h3>委託單</h3>
            <p>建立代購委託、追蹤進度</p>
          </div>
        </div>

        <div class="card shipment-card" @click="() => {}">
          <div class="card-icon">📦</div>
          <div class="card-body">
            <h3>出貨單</h3>
            <p>申請出貨、追蹤物流</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.app { min-height: 100vh; background: #f7f8fc; }

.navbar {
  background: #fff; padding: 0 1.5rem; height: 56px;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  position: sticky; top: 0; z-index: 10;
}
.brand { font-size: 1.125rem; font-weight: 700; color: #5b7ee5; }
.nav-right { display: flex; align-items: center; gap: 0.75rem; font-size: 0.8125rem; }
.nav-link {
  color: #5b7ee5; text-decoration: none; font-weight: 500;
  transition: opacity 0.2s;
}
.nav-link:hover { opacity: 0.7; }
.nav-divider { width: 1px; height: 16px; background: #ddd; }
.nav-user { color: #555; }
.nav-btn {
  background: none; border: 1px solid #e0e0e0; padding: 0.25rem 0.625rem;
  border-radius: 4px; font-size: 0.75rem; cursor: pointer; color: #999;
  transition: all 0.2s;
}
.nav-btn.logout:hover { color: #d32f2f; border-color: #d32f2f; }

.main { max-width: 800px; margin: 2rem auto; padding: 0 1rem; }

.greeting { margin-bottom: 1.5rem; }
.greeting h2 { font-size: 1.375rem; font-weight: 600; color: #333; }

.card-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; }
@media (max-width: 600px) { .card-grid { grid-template-columns: 1fr; } }

.card {
  background: #fff; border-radius: 12px; padding: 1.5rem;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  display: flex; align-items: center; gap: 1rem;
  cursor: default; transition: transform 0.15s, box-shadow 0.15s;
  border-left: 4px solid transparent;
}
.card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.commission-card { border-left-color: #5b7ee5; }
.shipment-card { border-left-color: #43b581; }

.card-icon { font-size: 2rem; }
.card-body h3 { font-size: 1rem; font-weight: 600; color: #333; margin-bottom: 0.125rem; }
.card-body p { font-size: 0.8125rem; color: #999; margin: 0; }
</style>
