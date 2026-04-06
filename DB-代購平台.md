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
| 設定 | `fee_configs`, `banners` |

共 **12 張表、15 個 ENUM**

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
  'BEFORE_CONFIRMED',   -- 審核前取消 → 全額退款
  'BEFORE_PAID',        -- 確認後付款前取消 → 全額退款
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
CREATE TYPE banner_status       AS ENUM ('ACTIVE', 'INACTIVE', 'SCHEDULED');
CREATE TYPE fee_config_key      AS ENUM (
  'SERVICE_FEE_RATE',           -- 服務費率（%）
  'SERVICE_FEE_MIN',            -- 最低服務費（TWD）
  'INSPECTION_FEE_PER_ITEM',    -- 每件檢品費（TWD）
  'DOMESTIC_SHIPPING_JP_FLAT',  -- 日本境內運費估算（TWD）
  'INTL_SHIPPING_RATE_AIR',     -- 空運費率（TWD/kg）
  'INTL_SHIPPING_RATE_SEA',     -- 海運費率（TWD/kg）
  'CUSTOMS_ESTIMATE_RATE',      -- 關稅預估率（%）
  'FREE_INSPECTION_THRESHOLD',  -- 免費檢品門檻（TWD）
  'CANCEL_FEE_RATE_AFTER_PAID'  -- 付款後取消手續費率（%）
);
```

---

## 資料表定義

### users

```sql
CREATE TABLE users (
  id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  email             VARCHAR(255) NOT NULL UNIQUE,
  phone             VARCHAR(20),
  full_name         VARCHAR(100) NOT NULL,
  display_name      VARCHAR(50),
  avatar_url        TEXT,
  role              user_role    NOT NULL DEFAULT 'BUYER',
  status            user_status  NOT NULL DEFAULT 'ACTIVE',
  auth_uid          UUID         UNIQUE,        -- 對應 Supabase auth.users，RLS 用
  preferred_language VARCHAR(10) DEFAULT 'zh-TW',
  timezone          VARCHAR(50)  DEFAULT 'Asia/Taipei',
  buyer_note        TEXT,                        -- 固定給 Staff 看的備註
  deleted_at        TIMESTAMPTZ,                 -- 軟刪除
  created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  last_login_at     TIMESTAMPTZ
);
```

**設計理由：**
- `auth_uid`：Supabase 認證與業務資料分離，透過此欄橋接
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
  postal_code     VARCHAR(10)  NOT NULL,
  city            VARCHAR(50)  NOT NULL,
  district        VARCHAR(50)  NOT NULL,
  address_line1   VARCHAR(200) NOT NULL,
  address_line2   VARCHAR(200),
  country_code    CHAR(2)      NOT NULL DEFAULT 'TW',
  is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
  is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- 獨立表：Buyer 可存多個地址，有列表、設預設、刪除等操作
- `is_deleted` 軟刪除：commission 的 `shipping_address_id` FK 不能壞

---

### commissions（核心）

```sql
CREATE TABLE commissions (
  id                    UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
  commission_number     VARCHAR(20)        NOT NULL UNIQUE,  -- CO-YYYYMMDD-序號

  buyer_id              UUID               NOT NULL REFERENCES users(id),
  assigned_staff_id     UUID               REFERENCES users(id),

  status                commission_status  NOT NULL DEFAULT 'PENDING',
  cancel_stage          cancel_stage,

  -- 地址：FK + 快照雙存
  shipping_address_id       UUID           REFERENCES addresses(id),
  shipping_address_snapshot JSONB,         -- 確認時寫入，鎖定當下地址

  shipping_method       shipping_method    NOT NULL DEFAULT 'AIR_STANDARD',
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
  cancel_requested_by   UUID               REFERENCES users(id),

  -- 費用（JSONB 快照，結構見下方）
  fee_snapshot          JSONB              NOT NULL DEFAULT '{}',
  estimated_total_twd   NUMERIC(12,2),    -- 冗餘，方便排序/篩選
  actual_total_twd      NUMERIC(12,2),
  jpy_to_twd_rate       NUMERIC(8,4),     -- 確認時匯率快照

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

#### fee_snapshot 結構（JSONB）

```json
{
  "prepay": {
    "items_cost_jpy": 12500,
    "items_cost_twd": 2875,
    "jpy_to_twd_rate": 0.23,
    "service_fee_twd": 288,
    "service_fee_rate": 0.10,
    "value_added_services": [
      { "type": "INSPECTION_DETAIL", "fee_twd": 100 },
      { "type": "PHOTO", "fee_twd": 50 }
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

**設計理由：**
- 委託單 = 訂單：一張單走完整流程，避免兩表同步問題
- `shipping_address_snapshot`：地址快照鎖定當下版本，Buyer 之後修改地址不影響歷史
- `cancel_stage`：取消後 status 變 CANCELLED，需獨立欄位記錄「當時在哪個階段」，決定退款政策
- `fee_snapshot` 用 JSONB：費率會隨時間改變，快照保留歷史費用；結構不固定（非每筆都有所有項目）
- 多個時間戳：每個節點有獨立業務意義（SLA、帳期、倉儲時間計算）

---

### commission_items

```sql
CREATE TABLE commission_items (
  id                      UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
  commission_id           UUID      NOT NULL REFERENCES commissions(id) ON DELETE CASCADE,

  product_url             TEXT      NOT NULL,
  product_name            TEXT      NOT NULL,
  product_name_ja         TEXT,
  product_image_url       TEXT,
  specifications          JSONB     NOT NULL DEFAULT '{}',  -- {"color":"紅","size":"M"}
  quantity                SMALLINT  NOT NULL DEFAULT 1 CHECK (quantity > 0),

  unit_price_budget_jpy   NUMERIC(10,2),   -- Buyer 填的預算上限
  unit_price_actual_jpy   NUMERIC(10,2),   -- Staff 實際購買價

  weight_estimate_g       INT,             -- 估算重量
  weight_actual_g         INT,             -- Staff 量測的實際重量

  item_note               TEXT,
  is_purchased            BOOLEAN   NOT NULL DEFAULT FALSE,
  purchased_at            TIMESTAMPTZ,
  purchase_receipt_url    TEXT,
  is_shipped              BOOLEAN   NOT NULL DEFAULT FALSE,
  sort_order              SMALLINT  NOT NULL DEFAULT 0,

  created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- `specifications` 用 JSONB：不同平台商品規格維度不同，無法用固定欄位覆蓋
- 雙價格欄位：`budget_jpy` 供 Staff 超額警示，`actual_jpy` 用於費用計算與對帳
- 雙重量欄位：`estimate_g` 供下單時費用試算，`actual_g` 是 FINAL 付款計費依據
- `is_shipped`：快速判斷此商品是否已進出貨單，避免 JOIN

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
  shipment_number            VARCHAR(20)      NOT NULL UNIQUE,  -- SH-YYYYMMDD-序號

  buyer_id                   UUID             NOT NULL REFERENCES users(id),
  created_by_staff_id        UUID             NOT NULL REFERENCES users(id),
  shipping_address_snapshot  JSONB            NOT NULL,

  status                     shipment_status  NOT NULL DEFAULT 'PREPARING',
  shipping_method            shipping_method  NOT NULL DEFAULT 'AIR_STANDARD',

  total_weight_g             INT,
  intl_shipping_actual_twd   NUMERIC(10,2),
  intl_shipping_estimate_twd NUMERIC(10,2),

  tracking_number            VARCHAR(100),
  carrier                    VARCHAR(50),
  tracking_url               TEXT,

  package_image_urls         JSONB            DEFAULT '[]',
  staff_note                 TEXT,

  packed_at                  TIMESTAMPTZ,
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
- `shipping_address_snapshot`：出貨單建立後地址鎖定，不受後續修改影響

---

### shipment_items

```sql
CREATE TABLE shipment_items (
  id                    UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
  shipment_id           UUID    NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
  commission_item_id    UUID    NOT NULL REFERENCES commission_items(id),
  commission_id         UUID    NOT NULL REFERENCES commissions(id),  -- 冗餘
  item_weight_g         INT,
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
  payment_number         VARCHAR(20)     NOT NULL UNIQUE,  -- PAY-YYYYMMDD-序號

  commission_id          UUID            REFERENCES commissions(id),   -- PREPAY
  shipment_id            UUID            REFERENCES shipments(id),     -- FINAL
  buyer_id               UUID            NOT NULL REFERENCES users(id),

  payment_type           payment_type    NOT NULL,
  amount_twd             NUMERIC(12,2)   NOT NULL CHECK (amount_twd > 0),
  payment_method         payment_method  NOT NULL,
  status                 payment_status  NOT NULL DEFAULT 'PENDING',

  gateway_transaction_id VARCHAR(200),
  gateway_response       JSONB,           -- 金流平台原始回傳，備查
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
- `gateway_response` JSONB：保留金流原始回傳，用於客訴、對帳、稽核
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

  email_sent          BOOLEAN            NOT NULL DEFAULT FALSE,
  email_sent_at       TIMESTAMPTZ,
  email_error         TEXT,

  metadata            JSONB              DEFAULT '{}',  -- 前端路由跳轉用

  created_at          TIMESTAMPTZ        NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- `email_sent` 直接放通知表：站內信與 email 1:1 對應，不需獨立拆表
- 多型關聯（`related_entity_type` + `related_entity_id`）：通知可對應不同實體，比多個 nullable FK 乾淨
- `metadata`：前端用來決定點擊後的跳轉路由，例如 `{"route": "/commissions/xxx"}`

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
  id            UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
  config_key    fee_config_key  NOT NULL,
  numeric_value NUMERIC(10,4),
  text_value    TEXT,
  display_name  VARCHAR(100)    NOT NULL,
  description   TEXT,
  unit          VARCHAR(20),               -- '%', 'TWD', 'TWD/kg'
  is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
  version       INT             NOT NULL DEFAULT 1,
  valid_from    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
  valid_until   TIMESTAMPTZ,               -- NULL = 目前有效
  created_by    UUID            REFERENCES users(id),
  created_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);
```

**設計理由：**
- `valid_from` / `valid_until` 版本控制：每次修改費率建新紀錄 + 關舊紀錄，保留完整歷史
- 查詢有效費率：`WHERE valid_until IS NULL AND is_active = TRUE`
- 這樣可回答「三個月前這筆委託適用的費率是多少」

---

### banners

```sql
CREATE TABLE banners (
  id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  title            VARCHAR(200)  NOT NULL,
  subtitle         VARCHAR(300),
  image_url        TEXT          NOT NULL,
  image_url_mobile TEXT,
  link_url         TEXT,
  link_target      VARCHAR(20)   DEFAULT '_self',
  status           banner_status NOT NULL DEFAULT 'INACTIVE',
  scheduled_start  TIMESTAMPTZ,
  scheduled_end    TIMESTAMPTZ,
  sort_order       SMALLINT      NOT NULL DEFAULT 0,
  click_count      INT           NOT NULL DEFAULT 0,
  impression_count INT           NOT NULL DEFAULT 0,
  created_by       UUID          REFERENCES users(id),
  created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
  updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
```

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
  BEFORE_CONFIRMED  → 全額退款
  BEFORE_PAID       → 全額退款
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
CREATE INDEX idx_users_auth_uid ON users(auth_uid);  -- Supabase RLS 必備

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

-- fee_configs（查有效費率）
CREATE INDEX idx_fee_configs_active
  ON fee_configs(config_key)
  WHERE valid_until IS NULL AND is_active = TRUE;

-- shipment_items（防重複）
-- UNIQUE (shipment_id, commission_item_id) 已自動建立索引
```

---

## 設計原則總結

| 原則 | 體現 |
|------|------|
| **快照 > FK** | 地址、費用、匯率都做快照，歷史資料不受後續修改影響 |
| **冗餘換效能** | `shipment_items.commission_id`、`messages.sender_role` 刻意重複存 |
| **DB 層最後防線** | UNIQUE、CHECK constraint 在應用層之外再擋一道 |
| **軟刪除** | users、addresses 不硬刪，保留歷史關聯 |
| **JSONB 用在彈性結構** | 商品規格、費用明細、金流回傳等結構不固定的資料 |
| **版本控制** | fee_configs 每次修改建新紀錄，保留完整費率歷史 |

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
└── V7__fee_configs_banners_indexes.sql
```
