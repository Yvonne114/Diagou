# 資料庫設計：日本代購平台

> 最後更新：2026-04-06
> Tech Stack：Supabase PostgreSQL
> 負責人：Eileen

---

## 總覽

| 類別 | 資料表 |
|------|------|
| 使用者 | `users`, `addresses` |
| 委託核心 | `commissions`, `commission_items`, `commission_services` |
| 出貨 | `shipments`, `shipment_items` |
| 金流 | `payments` |
| 溝通 | `notifications`, `messages` |
| 設定 | `fee_configs` |

共 **11 張表、13 個 ENUM**

---

## ENUM 型別定義

```sql
-- 使用者
CREATE TYPE user_role   AS ENUM ('BUYER', 'STAFF', 'ADMIN');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'SUSPENDED', 'DELETED');

-- 委託單
CREATE TYPE commission_status AS ENUM (
  'PENDING',            -- Buyer 已提交，等 Staff 審核
  'CONFIRMED',          -- Staff 確認，等 Buyer 預付款
  'PAID',               -- Buyer 已預付，等 Staff 購買
  'PURCHASING',         -- Staff 正在日本購買
  'IN_JP_WAREHOUSE',    -- 商品已抵日本倉庫
  'WAITING_SHIPMENT',   -- 等待合併出貨
  'SHIPPED',            -- 已進出貨單且出貨
  'DELIVERED',          -- Buyer 確認收到
  'REJECTED',           -- Staff 拒絕
  'CANCELLED'           -- Buyer 取消
);

CREATE TYPE cancel_stage AS ENUM (
  'BEFORE_PAID',        -- 付款前取消（含審核前）→ 全額退款
  'BEFORE_PURCHASING',  -- 付款後購買前取消 → 退款扣手續費
  'AFTER_PURCHASING'    -- 購買後取消 → 無法退款，個案處理
);

-- 出貨單
CREATE TYPE shipment_status AS ENUM (
  'PREPARING',          -- 整理打包中
  'PACKED',             -- 打包完成，計算實際運費
  'PENDING_PAYMENT',    -- 等 Buyer 補繳運費
  'PAID',               -- 運費付清，可出貨
  'SHIPPED',            -- 已交運，有追蹤號
  'IN_TRANSIT',         -- 運送中
  'CUSTOMS',            -- 清關中（可能跳過）
  'DELIVERED',          -- 已送達
  'EXCEPTION'           -- 異常（遺失、損毀等）
);

-- 付款
CREATE TYPE payment_type   AS ENUM ('PREPAY', 'FINAL');
CREATE TYPE payment_status AS ENUM (
  'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED',
  'REFUNDED', 'PARTIALLY_REFUNDED'
);
CREATE TYPE payment_method AS ENUM (
  'CREDIT_CARD', 'BANK_TRANSFER', 'ECPAY', 'LINEPAY', 'OTHER'
);

-- 加值服務
CREATE TYPE value_added_service_type AS ENUM (
  'INSPECTION_BASIC',   -- 簡易檢品（確認有收到商品）
  'INSPECTION_DETAIL',  -- 詳細檢品（確認狀態，附照片）
  'PHOTO',              -- 拍照服務
  'REPACKAGE',          -- 重新包裝（去除原廠包裝減重）
  'CONSOLIDATION'       -- 合併包裝（多件合一箱）
);

-- 運送方式
CREATE TYPE shipping_method AS ENUM (
  'AIR_STANDARD', 'AIR_EXPRESS', 'SEA', 'EMS'
);

-- 通知
CREATE TYPE notification_type AS ENUM (
  'COMMISSION_CONFIRMED', 'COMMISSION_REJECTED', 'COMMISSION_PURCHASED',
  'COMMISSION_IN_WAREHOUSE', 'SHIPMENT_CREATED', 'SHIPMENT_SHIPPED',
  'SHIPMENT_DELIVERED', 'PAYMENT_REQUIRED', 'PAYMENT_CONFIRMED',
  'INSPECTION_DONE', 'MESSAGE_RECEIVED', 'CANCEL_APPROVED', 'SYSTEM'
);
CREATE TYPE entity_type AS ENUM ('COMMISSION', 'SHIPMENT', 'PAYMENT', 'MESSAGE');

-- 其他
CREATE TYPE message_sender_role AS ENUM ('BUYER', 'STAFF');
CREATE TYPE fee_config_key      AS ENUM (
  -- 【平台服務費】
  'SERVICE_FEE_RATE',           -- 服務費率（%）例：0.10 = 商品費的 10%
  'SERVICE_FEE_MIN',            -- 最低服務費（TWD）例：100，10% 算出來不足 100 元就收 100

  -- 【加值服務費】
  'INSPECTION_FEE_PER_ITEM',    -- 每件檢品費（TWD）例：50，選檢品時每件商品收 50 元

  -- 【運費】
  'DOMESTIC_SHIPPING_JP_FLAT',  -- 日本境內運費估算（TWD）例：150，商品從日本店家寄到倉庫的平均費用
  'INTL_SHIPPING_RATE_AIR',     -- 空運費率（TWD/kg）例：200，每公斤空運費用
  'INTL_SHIPPING_RATE_SEA',     -- 海運費率（TWD/kg）例：80，每公斤海運費用

  -- 【關稅】
  'CUSTOMS_ESTIMATE_RATE',      -- 關稅預估率（%）例：0.05 = 商品費的 5%（僅供估算，實際依海關裁定）

  -- 【優惠門檻】
  'FREE_INSPECTION_THRESHOLD',  -- 免費檢品門檻（TWD）例：10000，委託金額超過此值免收檢品費

  -- 【取消手續費】
  'CANCEL_FEE_RATE_AFTER_PAID'  -- 付款後取消手續費率（%）例：0.05 = 退款金額扣 5% 手續費
);
```

---

## 資料表定義

### users

```sql
CREATE TABLE users (
  id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  phone         VARCHAR(20),
  full_name     VARCHAR(100) NOT NULL,
  display_name  VARCHAR(50),
  role          user_role    NOT NULL DEFAULT 'BUYER',
  status        user_status  NOT NULL DEFAULT 'ACTIVE',
  deleted_at    TIMESTAMPTZ,                 -- 軟刪除
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  last_login_at TIMESTAMPTZ
);
```

**設計理由：**
- `password_hash`：Spring Boot 自管 JWT，由後端驗證帳密後自行簽發 token
- JWT payload 帶 `user_id` + `role`，後端每支 API 從 token 取得身份，自行做存取控制
- Supabase 僅作為 PostgreSQL 使用，不用 Supabase Auth，故不需要 `auth_uid`
- `deleted_at` 軟刪除：帳號刪除後歷史委託仍需保留，FK 不能壞
- `role` 單一欄位：角色互斥，不用多個 boolean flag

---

### addresses

```sql
CREATE TABLE addresses (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  label           VARCHAR(50),                   -- 「家」「公司」
  recipient_name  VARCHAR(100) NOT NULL,
  phone           VARCHAR(20)  NOT NULL,
  postal_code   VARCHAR(10)  NOT NULL,
  city          VARCHAR(50)  NOT NULL,            -- 縣市
  district      VARCHAR(50)  NOT NULL,            -- 區
  address_line  VARCHAR(200) NOT NULL,            -- 街道地址（含樓層）
  is_default    BOOLEAN      NOT NULL DEFAULT FALSE,
  is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- 獨立表：Buyer 可存多個地址，有列表、設預設、刪除等操作
- `is_deleted` 軟刪除：shipment 的 `shipping_address_id` FK 不能壞

---

### commissions（核心）

```sql
CREATE TABLE commissions (
  id                    UUID               PRIMARY KEY DEFAULT gen_random_uuid(),

  buyer_id              UUID               NOT NULL REFERENCES users(id),
  assigned_staff_id     UUID               REFERENCES users(id),

  status                commission_status  NOT NULL DEFAULT 'PENDING',
  cancel_stage          cancel_stage,

  requires_inspection   BOOLEAN            NOT NULL DEFAULT FALSE,

  buyer_note            TEXT,
  staff_note            TEXT,

  -- 拒絕
  rejection_reason      TEXT,
  rejected_at           TIMESTAMPTZ,
  rejected_by           UUID               REFERENCES users(id),

  -- 取消
  cancel_reason         TEXT,
  cancelled_at          TIMESTAMPTZ,

  -- 費用（PREPAY 段，委託確認時寫入）
  items_cost_jpy        NUMERIC(10,2),    -- 商品費（日圓）
  items_cost_twd        NUMERIC(10,2),    -- 商品費（台幣）
  jpy_to_twd_rate       NUMERIC(8,4),     -- 確認時匯率
  service_fee_twd       NUMERIC(10,2),    -- 服務費
  inspection_fee_twd    NUMERIC(10,2),    -- 加值服務費小計
  prepay_total_twd      NUMERIC(10,2),    -- 預付總額

  -- 各階段時間戳
  submitted_at          TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
  confirmed_at          TIMESTAMPTZ,
  paid_at               TIMESTAMPTZ,
  purchasing_started_at TIMESTAMPTZ,
  arrived_warehouse_at  TIMESTAMPTZ,
  delivered_at          TIMESTAMPTZ,
  created_at            TIMESTAMPTZ        NOT NULL DEFAULT NOW(),
  updated_at            TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- 委託單 = 訂單：一張單走完整流程，避免兩表同步問題
- 地址和運送方式不放在委託單：委託單只管「買什麼」，地址和運送方式是出貨時才需要，放在 `shipments` 表
- `cancel_stage`：取消後 status 變 CANCELLED，需獨立欄位記錄「當時在哪個階段」，決定退款政策
- 費用拆成獨立欄位：比 JSONB 更直覺，查詢和 debug 更方便
- 費用分兩段：PREPAY（確認時填）、FINAL（出貨後填），對應兩次付款時機
- 多個時間戳：每個節點有獨立業務意義（SLA、帳期、倉儲時間計算）

---

### commission_items

```sql
CREATE TABLE commission_items (
  id                      UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  commission_id           UUID      NOT NULL REFERENCES commissions(id) ON DELETE CASCADE,

  product_url             TEXT      NOT NULL,
  product_name_ja         TEXT,             -- 日文商品名稱，從搜尋結果或爬蟲帶入
  product_image_url       TEXT,
  specifications          JSONB     NOT NULL DEFAULT '{}',  -- {"color":"紅","size":"M"}
  quantity                SMALLINT  NOT NULL DEFAULT 1 CHECK (quantity > 0),

  unit_price_actual_jpy   NUMERIC(10,2),   -- Staff 實際購買價
  weight_actual_g         INT,             -- Staff 量測的實際重量

  item_note               TEXT,

  created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- `specifications` 用 JSONB：不同平台商品規格維度不同，無法用固定欄位覆蓋
- `unit_price_actual_jpy`：Staff 實際購買後填入，用於費用計算與對帳
- `weight_actual_g`：Staff 打包時量測，是 FINAL 運費計費依據
- 採購和出貨狀態不在 item 層追蹤，跟著 commission status 走即可

---

### commission_services

```sql
CREATE TABLE commission_services (
  id                UUID                     PRIMARY KEY DEFAULT gen_random_uuid(),
  commission_id     UUID                     NOT NULL REFERENCES commissions(id) ON DELETE CASCADE,
  service_type      value_added_service_type NOT NULL,
  fee_twd           NUMERIC(10,2)            NOT NULL,   -- 收費快照
  status            VARCHAR(20)              NOT NULL DEFAULT 'PENDING',  -- PENDING / DONE
  completed_at      TIMESTAMPTZ,
  staff_note        TEXT,
  result_image_urls JSONB                    DEFAULT '[]',

  created_at        TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ              NOT NULL DEFAULT NOW(),

  UNIQUE (commission_id, service_type)
);
```

**設計理由：**
- 獨立表而非 JSONB：每項服務有自己的執行狀態、完成時間、照片，需要獨立 UPDATE
- `fee_twd` 快照：避免 Admin 修改費率影響歷史收費紀錄
- `UNIQUE (commission_id, service_type)`：防止重複選同一服務，避免重複收費

---

### shipments

```sql
CREATE TABLE shipments (
  id                         UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
  buyer_id                   UUID             NOT NULL REFERENCES users(id),
  created_by_staff_id        UUID             NOT NULL REFERENCES users(id),
  shipping_address_id        UUID             NOT NULL REFERENCES addresses(id),

  status                     shipment_status  NOT NULL DEFAULT 'PREPARING',
  shipping_method            shipping_method  NOT NULL DEFAULT 'AIR_STANDARD',

  total_weight_g        INT,
  domestic_shipping_twd NUMERIC(10,2),    -- 日本境內運費
  intl_shipping_twd     NUMERIC(10,2),    -- 實際國際運費
  customs_twd           NUMERIC(10,2),    -- 關稅估算
  final_total_twd       NUMERIC(10,2),    -- 尾款總額

  tracking_number            VARCHAR(100),
  carrier                    VARCHAR(50),

  shipped_at                 TIMESTAMPTZ,
  delivered_at               TIMESTAMPTZ,
  estimated_delivery_at      DATE,

  created_at                 TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
  updated_at                 TIMESTAMPTZ      NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- 獨立表：一張出貨單可包含同一 Buyer 多筆委託的商品（合併出貨），無法內嵌在 commission
- `buyer_id` 限制一個 Buyer：一個包裹只寄給一個人
- `shipping_address_id` 只存 FK：地址一旦進出貨單即不允許修改，用應用層控制

---

### shipment_items

```sql
CREATE TABLE shipment_items (
  id                    UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  shipment_id           UUID    NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
  commission_item_id    UUID    NOT NULL REFERENCES commission_items(id),
  commission_id         UUID    NOT NULL REFERENCES commissions(id),  -- 冗餘
  allocated_shipping_twd NUMERIC(10,2),

  created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  UNIQUE (shipment_id, commission_item_id)
);
```

**設計理由：**
- `commission_id` 冗餘欄位：「這張出貨單涉及哪些委託」是高頻查詢，冗餘避免多層 JOIN
- `UNIQUE (shipment_id, commission_item_id)`：DB 層強制一件商品不能進兩張出貨單
- `allocated_shipping_twd`：按重量比例分攤運費，用於退款計算與對帳

---

### payments

```sql
CREATE TABLE payments (
  id                     UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  payment_number         VARCHAR(20)     NOT NULL UNIQUE,  -- ECPay MerchantTradeNo 用，格式：P + 13位時間戳（共14字元）

  commission_id          UUID            REFERENCES commissions(id),   -- PREPAY
  shipment_id            UUID            REFERENCES shipments(id),     -- FINAL
  buyer_id               UUID            NOT NULL REFERENCES users(id),

  payment_type           payment_type    NOT NULL,
  amount_twd             NUMERIC(12,2)   NOT NULL CHECK (amount_twd > 0),
  payment_method         payment_method  NOT NULL,
  status                 payment_status  NOT NULL DEFAULT 'PENDING',

  gateway_transaction_id VARCHAR(200),     -- ECPay 的 TradeNo
  gateway_response       JSONB,           -- ECPay callback 完整原始資料，備查
  ecpay_payment_type     VARCHAR(30),     -- ECPay 付款類型：Credit_CreditCard / ATM_BOT / CVS_CVS 等
  atm_info               JSONB,           -- ATM 虛擬帳號資訊（僅 ATM 付款時有值）
  paid_at                TIMESTAMPTZ,

  refund_amount_twd      NUMERIC(12,2),
  refund_reason          TEXT,
  refunded_at            TIMESTAMPTZ,

  payment_breakdown      JSONB            NOT NULL DEFAULT '{}',
  confirmed_by_staff_id  UUID             REFERENCES users(id),  -- 銀行轉帳人工核帳

  created_at             TIMESTAMPTZ      NOT NULL DEFAULT NOW(),
  updated_at             TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

  CONSTRAINT unique_commission_prepay
    UNIQUE (commission_id, payment_type),
  CONSTRAINT payment_must_have_entity
    CHECK (commission_id IS NOT NULL OR shipment_id IS NOT NULL)
);
```

**設計理由：**
- 雙 nullable FK（`commission_id` / `shipment_id`）：PREPAY 對應委託，FINAL 對應出貨單，兩者時機和對象不同
- `gateway_response` JSONB：保留 ECPay callback 完整原始回傳，用於客訴、對帳、稽核
- `ecpay_payment_type`：記錄 Buyer 選的付款方式（信用卡 / ATM / 超商），`choosePayment` 對應值
- `atm_info` JSONB：ATM 付款時 ECPay callback 回傳虛擬帳號、銀行代碼、繳費期限，前端需顯示給 Buyer，獨立存比從 `gateway_response` 解析方便
  ```json
  { "bank_code": "005", "v_account": "9381234567890", "expire_date": "2026/04/10" }
  ```
- `payment_number` 直接當 ECPay 的 `MerchantTradeNo` 使用，UUID 超過 20 字元限制故獨立產生，格式：`P` + 13 位 epoch 毫秒，共 14 字元
- `gateway_transaction_id` 存 ECPay 回傳的 `TradeNo`
- `checkMacValue` 和 `formData` 不存 DB，每次付款時重新產生
- `UNIQUE (commission_id, payment_type)`：防止同一委託的預付被重複建立
- `confirmed_by_staff_id`：銀行轉帳需人工核帳，記錄操作人供稽核

---

### notifications

```sql
CREATE TABLE notifications (
  id                  UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id             UUID               NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  type                notification_type  NOT NULL,
  title               VARCHAR(200)       NOT NULL,
  content             TEXT               NOT NULL,

  related_entity_type entity_type,
  related_entity_id   UUID,              -- 多型關聯

  is_read             BOOLEAN            NOT NULL DEFAULT FALSE,
  read_at             TIMESTAMPTZ,

  metadata            JSONB              DEFAULT '{}',  -- 前端路由跳轉用

  created_at          TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- 多型關聯（`related_entity_type` + `related_entity_id`）：通知可對應不同實體，比多個 nullable FK 乾淨
- `metadata`：前端用來決定點擊後的跳轉路由，例如 `{"route": "/commissions/xxx"}`
- Email 發送由 application layer（Spring Boot Event / Queue）負責，不在 DB 追蹤狀態

---

### messages

```sql
CREATE TABLE messages (
  id              UUID                  PRIMARY KEY DEFAULT gen_random_uuid(),
  commission_id   UUID                  NOT NULL REFERENCES commissions(id) ON DELETE CASCADE,
  sender_id       UUID                  NOT NULL REFERENCES users(id),
  sender_role     message_sender_role   NOT NULL,
  content         TEXT                  NOT NULL,
  attachments     JSONB                 DEFAULT '[]',  -- 圖片 URL 陣列
  is_read         BOOLEAN               NOT NULL DEFAULT FALSE,
  read_at         TIMESTAMPTZ,
  is_deleted      BOOLEAN               NOT NULL DEFAULT FALSE,  -- 軟刪除（撤回）
  deleted_at      TIMESTAMPTZ,
  created_at      TIMESTAMPTZ           NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- 以 commission 分組而非全域聊天室：訊息與委託脈絡強烈相關，Staff 可快速找到上下文
- `sender_role` 冗餘：前端顯示氣泡時需判斷「對方/自己」，冗餘避免 JOIN users

---

### fee_configs

```sql
CREATE TABLE fee_configs (
  config_key  fee_config_key  PRIMARY KEY,
  value       NUMERIC(10,4)   NOT NULL,
  updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- `config_key` 直接當 PK，一個 key 只有一筆，改費率直接 UPDATE
- 作品集不需要費率歷史版本控制，簡化設計

---

## 狀態機

### 委託單（commission_status）
```
                    PENDING
                   /       \
            Staff 拒絕    Staff 確認
                 |              |
            REJECTED        CONFIRMED
                            Buyer 預付
                                |
                              PAID
                          Staff 開始採購
                                |
                          PURCHASING
                          商品到日本倉
                                |
                        IN_JP_WAREHOUSE
                          等待出貨請求
                                |
                        WAITING_SHIPMENT
                          進入出貨單
                                |
                            SHIPPED
                          Buyer 確認
                                |
                           DELIVERED

── 取消路徑 ──────────────────────────────────
任何階段 → CANCELLED，cancel_stage 記錄時間點：
  BEFORE_PAID       → 全額退款（付款前，含審核前）
  BEFORE_PURCHASING → 退款扣手續費
  AFTER_PURCHASING  → 無法退款，個案處理
```

### 出貨單（shipment_status）
```
PREPARING → PACKED → PENDING_PAYMENT → PAID → SHIPPED → IN_TRANSIT → CUSTOMS → DELIVERED
任何階段 → EXCEPTION（遺失、損毀等）
```

### 付款（payment_status）
```
PENDING → PROCESSING → COMPLETED
                     → FAILED（可重新發起）
COMPLETED → REFUNDED / PARTIALLY_REFUNDED
```

---

## 關鍵索引

```sql
-- users
CREATE INDEX idx_users_email ON users(email);  -- 登入查詢

-- commissions（查詢最頻繁）
CREATE INDEX idx_commissions_buyer_status
  ON commissions(buyer_id, status, created_at DESC);

CREATE INDEX idx_commissions_pending
  ON commissions(status, submitted_at)
  WHERE status = 'PENDING';

CREATE INDEX idx_commissions_waiting
  ON commissions(buyer_id, status)
  WHERE status = 'WAITING_SHIPMENT';

-- notifications（未讀查詢高頻）
CREATE INDEX idx_notifications_unread
  ON notifications(user_id, created_at DESC)
  WHERE is_read = FALSE;

-- shipment_items（防重複）
-- UNIQUE (shipment_id, commission_item_id) 已自動建立索引
```

---

## 角色權限對照

Spring Boot 實作：
- Staff API 加 `@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")`
- Admin 專屬 API 加 `@PreAuthorize("hasRole('ADMIN')")`
- Buyer API 加 `@PreAuthorize("hasRole('BUYER')")`

| 操作 | Buyer | Staff | Admin |
|------|-------|-------|-------|
| **commissions** | | | |
| 提交委託 | ✅ | ❌ | ❌ |
| 查看自己的委託 | ✅ | ❌ | ❌ |
| 查看所有委託 | ❌ | ✅ | ✅ |
| 確認 / 拒絕委託 | ❌ | ✅ | ✅ |
| 更新採購進度 | ❌ | ✅ | ✅ |
| 取消委託 | ✅ | ❌ | ✅ |
| **commission_services** | | | |
| 選擇加值服務（下單時）| ✅ | ❌ | ❌ |
| 更新服務執行狀態 | ❌ | ✅ | ✅ |
| **shipments** | | | |
| 查看自己的出貨單 | ✅ | ❌ | ❌ |
| 建立 / 管理出貨單 | ❌ | ✅ | ✅ |
| 填入追蹤號碼 | ❌ | ✅ | ✅ |
| **payments** | | | |
| 發起付款 | ✅ | ❌ | ❌ |
| 人工核帳（銀行轉帳）| ❌ | ✅ | ✅ |
| 查看所有付款紀錄 | ❌ | ❌ | ✅ |
| **messages** | | | |
| 傳送 / 查看訊息 | ✅ | ✅ | ✅ |
| **notifications** | | | |
| 查看自己的通知 | ✅ | ✅ | ✅ |
| **users** | | | |
| 查看自己的帳號 | ✅ | ✅ | ✅ |
| 查看所有用戶 | ❌ | ❌ | ✅ |
| 停用 / 修改用戶角色 | ❌ | ❌ | ✅ |
| **fee_configs** | | | |
| 查看費率（計算用）| ❌ | ✅ | ✅ |
| 修改費率 | ❌ | ❌ | ✅ |

---

## 設計原則總結

| 原則 | 體現 |
|------|------|
| **FK 取代快照** | 地址用 FK，應用層控制不允許修改已使用的地址 |
| **冗餘換效能** | `shipment_items.commission_id`、`messages.sender_role` 刻意重複存 |
| **DB 層最後防線** | UNIQUE、CHECK constraint 在應用層之外再擋一道 |
| **軟刪除** | users、addresses 不硬刪，保留歷史關聯 |
| **JSONB 用在彈性結構** | 商品規格、金流回傳、ATM 資訊等結構不固定的資料 |
| **費率直接 UPDATE** | fee_configs 一個 key 一筆，改費率直接覆蓋，作品集不需要歷史版本 |

---

## Migration 規劃

```
src/main/resources/db/migration/
├── V1__init_enums.sql
├── V2__users_addresses.sql
├── V3__commissions_items_services.sql
├── V4__shipments.sql
├── V5__payments.sql
├── V6__notifications_messages.sql
└── V7__fee_configs_indexes.sql
```
