# SDD：日本代購平台（暫名）

> 最後更新：2026-04-06
> 狀態：資料庫設計定案，準備開始實作

---

## 背景與目標

建立一個日本代購平台，**不限二手商品**，涵蓋一般服飾店、樂器、相機、二手拍賣等各類日本市場。

**核心差異化：**
1. **費用透明化** — 下單前即可看到完整費用明細，不在結帳時才驚喜
2. **AI 輔助搜尋** — 海外用戶無需懂日文，也能找到想要的商品

---

## Tech Stack

| 層次 | 技術 |
|------|------|
| 後端 | Spring Boot（JWT 自行處理） |
| 前端 | Vue.js |
| 資料庫 | Supabase（PostgreSQL） |
| 檔案儲存 | Supabase Storage |
| AI 搜尋 | Claude API / OpenAI |

---

## 使用者角色

| 角色 | 說明 |
|------|------|
| Buyer | 提交委託、付款、追蹤訂單、與客服溝通 |
| Staff | 審核委託、處理訂單、更新進度、與買家溝通 |
| Admin | 後台管理、費率設定、廣告管理、數據統計 |

---

## 核心業務邏輯（已定案）

### 委託流程
- **委託單 = 訂單**：一張單從頭走到底，不拆兩張表
- Buyer 提交委託 → Staff 審核 → Buyer 預付 → Staff 購買 → 商品入倉 → 出貨 → 送達

### 兩階段付款
| 階段 | 時機 | 內容 |
|------|------|------|
| PREPAY | Staff 確認委託後 | 商品費 + 服務費 + 加值服務費（不含運費） |
| FINAL | 商品到倉、量完重量後 | 實際國際運費全額 + 日本境內運費 + 關稅估算 |

### 加值服務（固定選項，下單時選）
| 服務 | 說明 |
|------|------|
| INSPECTION_BASIC | 簡易檢品（確認有收到商品） |
| INSPECTION_DETAIL | 詳細檢品（確認狀態，附照片） |
| PHOTO | 拍照服務 |
| REPACKAGE | 重新包裝（去除原廠包裝減重） |
| CONSOLIDATION | 合併包裝（多件合一箱） |

### 其他決定
- **合併出貨**：同一 Buyer 多筆委託的商品可合進同一張出貨單
- **取消**：任何階段皆可取消，`cancel_stage` 記錄時間點，影響退款政策
- **拒絕通知**：Staff 拒絕委託 → email + 系統站內信，附拒絕原因
- **存取控制**：`user_role` 存於 users 表，Supabase RLS policy JOIN users 查 role

---

## 核心功能模組

### 1. 認證模組（Auth）
- 註冊 / 登入，Spring Boot 自行發 JWT
- RBAC 角色控制：Buyer / Staff / Admin
- Supabase RLS 作為資料層存取控制

### 2. 費用試算模組 ⭐
- 輸入：商品價格（日圓）、運送方式
- 輸出費用明細（PREPAY 段）：
  - 商品費（日圓 → 台幣，含匯率快照）
  - 代購服務費（百分比，Admin 設定）
  - 加值服務費（依選項）
- 費率由 Admin 後台設定，不 hardcode
- 注意：運費在商品到倉後才計算，試算頁僅提供運費估算參考

### 3. 委託單模組
流程：
```
Buyer 提交委託（填商品 URL、數量、規格、選加值服務）
  ↓
Staff 審核（確認可購買、填入實際價格）
  ↓
系統計算 PREPAY 費用明細 → 通知 Buyer
  ↓
Buyer 預付（商品費 + 服務費 + 加值服務費）
  ↓
Staff 在日本購買商品
  ↓
商品到達日本倉庫（執行加值服務）
  ↓
等待合併出貨
```

委託狀態機：
```
PENDING → CONFIRMED → PAID → PURCHASING → IN_JP_WAREHOUSE → WAITING_SHIPMENT → SHIPPED → DELIVERED
任何階段 → CANCELLED（cancel_stage 記錄時間點）
PENDING → REJECTED（附 rejection_reason）
```

取消退款政策：
| cancel_stage | 退款政策 |
|------|------|
| BEFORE_CONFIRMED | 全額退款 |
| BEFORE_PAID | 全額退款 |
| BEFORE_PURCHASING | 退款，扣手續費（費率 Admin 設定） |
| AFTER_PURCHASING | 無法退款，需個案處理 |

### 4. 出貨單模組
- 出貨單（Shipment）獨立管理，可合併同一 Buyer 多筆委託的商品
- Staff 打包後量實際重量，計算 FINAL 運費
- 通知 Buyer 補繳運費後出貨

出貨單狀態機：
```
PREPARING → PACKED → PENDING_PAYMENT → PAID → SHIPPED → IN_TRANSIT → CUSTOMS → DELIVERED
任何階段 → EXCEPTION（遺失、損毀等）
```

### 5. 付款模組
- 金流平台：Stripe 或 ECPay（待決定）
- 兩階段付款（見上方業務邏輯）
- 銀行轉帳支援人工核帳（Staff 確認）
- 退款：全額 / 部分退款

### 6. 訊息模組
- Buyer ↔ Staff 依委託單一對一對話（不是全域聊天室）
- 支援圖片附件（Supabase Storage）
- 訊息可軟刪除（撤回）
- Supabase Realtime 實作即時更新

### 7. 通知模組
- 站內信匣 + email 雙管道
- 觸發事件：委託確認/拒絕、付款通知、商品入倉、出貨、加值服務完成等
- email 發送狀態追蹤（含失敗重送）

### 8. 商品搜尋模組 ⭐

#### 介面設計（參考 Buyee）
- 上方 tab = 各購物平台切換
- 搜尋列支援「直接搜尋」和「AI 自動翻譯搜尋」兩種模式
- 主頁顯示個人化推薦商品（不做靜態分類）
- 新用戶冷啟動：顯示 Rakuten 熱門商品

#### 支援平台（MVP）
| 平台 | 方式 |
|------|------|
| Rakuten 樂天 | 官方 API（個人開發者帳號申請，免費） |
| Other sites | Buyer 直接貼商品網址提交委託 |

Mercari、Yahoo Auction 等無官方 API 或申請門檻高，MVP 不做，留後續擴充。

#### 搜尋流程
```
Buyer 輸入中文 / 英文關鍵字
  ↓
Claude API 生成多組相關日文搜尋詞（Query Expansion）
例：「復古相機」→ ["フィルムカメラ", "レトロカメラ", "コンパクトカメラ"]
  ↓
並行搜尋：
  ├── Rakuten API（官方支援）
  └── AI 爬蟲（Mercari / Yahoo Auction 等無 API 的平台）
  ↓
合併結果、去重、排序
  ↓
顯示商品列表（名稱、圖片、價格 JPY）
  ↓
Buyer 點擊商品 → 自動帶入委託表單（URL、商品名、參考價格、圖片）
  ↓
Buyer 填規格、數量、選加值服務 → 提交委託
```

#### AI 爬蟲設計
傳統爬蟲依賴固定 CSS selector（`class="item-price"`），網站改版就壞。
AI 爬蟲改用語意理解：把頁面內容交給 Claude API，讓 AI 自己判斷哪段是商品名稱、價格、圖片，對改版容忍度更高。

技術流程：
```
目標網站 URL
  ↓
Playwright 渲染完整頁面（處理 React / Vue 動態渲染）
  ↓
取得完整 HTML / 頁面文字
  ↓
Claude API 解析，擷取：商品名稱、價格（JPY）、圖片 URL、商品規格
  ↓
回傳結構化資料
```

注意事項：
- Mercari、Yahoo Auction 等為動態渲染網站，必須用 Playwright 等待 JS 執行完畢才能拿到商品資料
- 網站大幅改版或反爬蟲機制升級時仍可能失效（作品集可接受）
- 爬蟲失敗時 fallback：讓 Buyer 手動填入商品資訊

#### 直接貼網址流程
```
Buyer 貼入任意日本購物網站 URL
  ↓
Playwright 渲染頁面 → Claude API 解析商品資訊
  ↓（解析失敗則跳過，讓 Buyer 手動填）
自動填入委託表單 → Buyer 確認並提交
```

#### 個人化推薦（主頁）
- 有購買紀錄：依歷史委託的商品類別推薦
- 新用戶：顯示 Rakuten 熱門商品（ranking API）
- MVP 先做簡單版，不做協同過濾

### 9. 廣告模組
- Admin 管理首頁 / 分類頁橫幅
- 支援排程上下架（SCHEDULED 狀態）
- 點擊 / 曝光數統計

### 10. 後台管理
- 訂單管理、用戶管理
- 費率設定（服務費比例、運費表、加值服務費等）
- 廣告管理
- 數據統計

---

## 資料庫 Schema

### ENUM 型別

```sql
CREATE TYPE user_role AS ENUM ('BUYER', 'STAFF', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');

CREATE TYPE commission_status AS ENUM (
  'PENDING', 'CONFIRMED', 'PAID', 'PURCHASING',
  'IN_JP_WAREHOUSE', 'WAITING_SHIPMENT', 'SHIPPED', 'DELIVERED',
  'REJECTED', 'CANCELLED'
);

CREATE TYPE cancel_stage AS ENUM (
  'BEFORE_CONFIRMED', 'BEFORE_PAID', 'BEFORE_PURCHASING', 'AFTER_PURCHASING'
);

CREATE TYPE shipment_status AS ENUM (
  'PREPARING', 'PACKED', 'PENDING_PAYMENT', 'PAID',
  'SHIPPED', 'IN_TRANSIT', 'CUSTOMS', 'DELIVERED', 'EXCEPTION'
);

CREATE TYPE payment_type AS ENUM ('PREPAY', 'FINAL');

CREATE TYPE payment_status AS ENUM (
  'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED',
  'REFUNDED', 'PARTIALLY_REFUNDED'
);

CREATE TYPE payment_method AS ENUM (
  'CREDIT_CARD', 'BANK_TRANSFER', 'ECPAY', 'LINEPAY', 'OTHER'
);

CREATE TYPE value_added_service_type AS ENUM (
  'INSPECTION_BASIC', 'INSPECTION_DETAIL', 'PHOTO', 'REPACKAGE', 'CONSOLIDATION'
);

CREATE TYPE shipping_method AS ENUM (
  'AIR_STANDARD', 'AIR_EXPRESS', 'SEA', 'EMS'
);

CREATE TYPE notification_type AS ENUM (
  'COMMISSION_CONFIRMED', 'COMMISSION_REJECTED', 'COMMISSION_PURCHASED',
  'COMMISSION_IN_WAREHOUSE', 'SHIPMENT_CREATED', 'SHIPMENT_SHIPPED',
  'SHIPMENT_DELIVERED', 'PAYMENT_REQUIRED', 'PAYMENT_CONFIRMED',
  'INSPECTION_DONE', 'MESSAGE_RECEIVED', 'CANCEL_APPROVED', 'SYSTEM'
);

CREATE TYPE entity_type AS ENUM ('COMMISSION', 'SHIPMENT', 'PAYMENT', 'MESSAGE');

CREATE TYPE fee_config_key AS ENUM (
  'SERVICE_FEE_RATE', 'SERVICE_FEE_MIN', 'INSPECTION_FEE_PER_ITEM',
  'DOMESTIC_SHIPPING_JP_FLAT', 'INTL_SHIPPING_RATE_AIR', 'INTL_SHIPPING_RATE_SEA',
  'CUSTOMS_ESTIMATE_RATE', 'FREE_INSPECTION_THRESHOLD', 'CANCEL_FEE_RATE_AFTER_PAID'
);

CREATE TYPE message_sender_role AS ENUM ('BUYER', 'STAFF');
CREATE TYPE banner_status AS ENUM ('ACTIVE', 'INACTIVE', 'SCHEDULED');
```

### 資料表

#### users
```
id, email, phone, full_name, display_name, avatar_url
role (user_role), status (user_status)
auth_uid          ← 對應 Supabase auth.users，RLS 用
preferred_language, timezone
buyer_note        ← 固定給 Staff 看的備註
deleted_at        ← 軟刪除
created_at, updated_at, last_login_at
```

#### addresses
```
id, user_id (FK users)
label             ← 「家」「公司」
recipient_name, phone
postal_code, city, district, address_line1, address_line2
country_code (預設 TW)
is_default, is_deleted (軟刪除)
```

#### commissions
```
id, commission_number (CO-YYYYMMDD-序號)
buyer_id (FK), assigned_staff_id (FK)
status (commission_status), cancel_stage
shipping_address_snapshot (JSONB)  ← 確認時寫入快照，鎖定地址
shipping_method, requires_inspection
buyer_note, staff_note
rejection_reason, rejected_at, rejected_by
cancel_reason, cancelled_at, cancel_requested_by
fee_snapshot (JSONB)               ← 見下方結構
estimated_total_twd, actual_total_twd  ← 冗餘，方便排序
jpy_to_twd_rate                    ← 確認時匯率快照
submitted_at, confirmed_at, paid_at, purchasing_started_at,
arrived_warehouse_at, delivered_at
```

fee_snapshot 結構：
```json
{
  "prepay": {
    "items_cost_jpy": 12500,
    "items_cost_twd": 2875,
    "jpy_to_twd_rate": 0.23,
    "service_fee_twd": 288,
    "service_fee_rate": 0.10,
    "value_added_services": [
      {"type": "INSPECTION_DETAIL", "fee_twd": 100},
      {"type": "PHOTO", "fee_twd": 50}
    ],
    "prepay_total_twd": 3313
  },
  "final": {
    "actual_weight_g": 850,
    "shipping_method": "AIR_STANDARD",
    "domestic_shipping_jp_twd": 150,
    "intl_shipping_twd": 920,
    "customs_estimate_twd": 200,
    "final_total_twd": 1270
  }
}
```

#### commission_services
```
id, commission_id (FK), service_type (value_added_service_type)
fee_twd           ← 收費快照（避免費率改了影響歷史）
status            ← PENDING / DONE
completed_at, staff_note
result_image_urls (JSONB)  ← 檢品/拍照照片 URL 陣列
UNIQUE (commission_id, service_type)
```

#### commission_items
```
id, commission_id (FK)
product_url, product_name, product_name_ja, product_image_url
specifications (JSONB)       ← {"color": "紅", "size": "M"}
quantity
unit_price_budget_jpy        ← Buyer 填的預算上限
unit_price_actual_jpy        ← Staff 實際購買價
weight_estimate_g, weight_actual_g
item_note
is_purchased, purchased_at, purchase_receipt_url
is_shipped                   ← 是否已進出貨單
sort_order
```

#### shipments
```
id, shipment_number (SH-YYYYMMDD-序號)
buyer_id (FK), created_by_staff_id (FK)
shipping_address_snapshot (JSONB)
status (shipment_status), shipping_method
total_weight_g
intl_shipping_actual_twd, intl_shipping_estimate_twd
tracking_number, carrier, tracking_url
package_image_urls (JSONB)
packed_at, shipped_at, delivered_at, estimated_delivery_at
staff_note
```

#### shipment_items
```
id, shipment_id (FK), commission_item_id (FK), commission_id (FK 冗餘)
item_weight_g
allocated_shipping_twd       ← 按重量比例分攤的運費
UNIQUE (shipment_id, commission_item_id)
```

#### payments
```
id, payment_number (PAY-YYYYMMDD-序號)
commission_id (FK, nullable), shipment_id (FK, nullable)
buyer_id (FK)
payment_type (PREPAY/FINAL), amount_twd
payment_method, status (payment_status)
gateway_transaction_id
gateway_response (JSONB)     ← 金流原始回傳，備查
paid_at
refund_amount_twd, refund_reason, refunded_at
payment_breakdown (JSONB)    ← 此次付款費用明細快照
confirmed_by_staff_id        ← 銀行轉帳人工核帳
UNIQUE (commission_id, payment_type)
CHECK (commission_id IS NOT NULL OR shipment_id IS NOT NULL)
```

#### notifications
```
id, user_id (FK)
type (notification_type), title, content
related_entity_type (entity_type), related_entity_id  ← 多型關聯
is_read, read_at
email_sent, email_sent_at, email_error
metadata (JSONB)             ← 前端路由跳轉用
```

#### messages
```
id, commission_id (FK)
sender_id (FK), sender_role (message_sender_role)
content
attachments (JSONB)          ← 圖片 URL 陣列
is_read, read_at
is_deleted, deleted_at       ← 軟刪除（撤回）
```

#### fee_configs
```
id, config_key (fee_config_key)
numeric_value, text_value
display_name, description, unit
is_active
valid_from, valid_until      ← 版本控制，valid_until IS NULL = 目前有效
created_by (FK)
```

#### banners
```
id, title, subtitle
image_url, image_url_mobile, link_url, link_target
status (banner_status), scheduled_start, scheduled_end
sort_order
click_count, impression_count
created_by (FK)
```

### 關鍵索引
```sql
-- commissions（查詢最頻繁）
idx_commissions_buyer_status  ON (buyer_id, status, created_at DESC)
idx_commissions_pending       ON (status, submitted_at) WHERE status='PENDING'
idx_commissions_waiting       ON (buyer_id, status) WHERE status='WAITING_SHIPMENT'

-- notifications（未讀查詢高頻）
idx_notifications_unread      ON (user_id, created_at DESC) WHERE is_read=FALSE

-- fee_configs（查有效費率）
idx_fee_configs_active        ON (config_key) WHERE valid_until IS NULL AND is_active=TRUE

-- users（Supabase RLS）
idx_users_auth_uid            ON (auth_uid)
```

---

## API 設計（高層次）

```
POST   /api/auth/register
POST   /api/auth/login

POST   /api/fee/calculate                    # 下單前費用試算（PREPAY 段）

POST   /api/commissions                      # 提交委託單（含加值服務選項）
GET    /api/commissions
GET    /api/commissions/{id}
PATCH  /api/commissions/{id}/confirm         # Staff 確認委託
PATCH  /api/commissions/{id}/reject          # Staff 拒絕委託
PATCH  /api/commissions/{id}/cancel          # Buyer 取消委託
PATCH  /api/commissions/{id}/status          # Staff 更新採購進度

POST   /api/shipments                        # 建立出貨單（合併委託）
GET    /api/shipments/{id}
PATCH  /api/shipments/{id}/pack              # Staff 填入實際重量、計算運費
PATCH  /api/shipments/{id}/ship              # Staff 填入追蹤號碼

POST   /api/payments                         # 建立付款（PREPAY / FINAL）
GET    /api/payments/{id}
POST   /api/payments/{id}/confirm            # Staff 人工確認銀行轉帳

GET    /api/commissions/{id}/messages
POST   /api/commissions/{id}/messages

GET    /api/notifications
PATCH  /api/notifications/{id}/read

GET    /api/admin/commissions
GET    /api/admin/users
PUT    /api/admin/fee-config
POST   /api/admin/banners
```

---

## 前端頁面規劃

```
/               首頁（廣告 banner + AI 搜尋入口 + 費用試算器）
/search         搜尋結果頁（AI 輔助）
/submit         提交委託單（含加值服務選項）
/commissions    我的委託列表
/commissions/:id  委託詳情 + 訊息 + 加值服務狀態
/shipments/:id  出貨單詳情 + 追蹤
/notifications  通知信匣
/staff          Staff 工作介面（委託審核、採購更新、出貨管理）
/admin          後台管理
```

---

## MVP 範圍

**第一版：**
- Auth
- 委託單完整流程（含加值服務）
- 兩階段付款（先串接一種金流）
- 出貨單 + 合併出貨
- 訊息功能
- 通知（站內信 + email）
- Staff 工作介面
- 費率後台

**第二版：**
- 商品搜尋（Rakuten API + AI 爬蟲 + Query Expansion）
- 個人化推薦
- 廣告模組
- 擴充更多購物平台（Mercari、Yahoo Auction）

---

## 開放問題（待決定）

- [ ] 金流平台：Stripe vs ECPay？還是兩個都要？
- [ ] 幣別：只收台幣，還是支援日幣？匯率怎麼更新（人工 vs 串 API）？
- [ ] 物流：有日本實體倉庫？還是串第三方集貨（例如轉運倉）？
- [ ] Buyer 需要實名驗證嗎？
