# 需求文件：日本代購平台（Proxy Purchase Platform）

## 簡介

本平台為日本商品代購服務系統，類似樂淘、跨買等代購網站。消費者（Buyer）可透過平台提交委託單，由後台操作人員（Staff）代為在日本購買商品，並提供從日本倉庫到台灣的全程物流追蹤、合併出貨、費用結算等功能。平台另設管理員（Admin）角色負責系統設定與營運管理。

技術棧：
- 後端：Spring Boot 4.0.5 + Java 21 + Spring Data JPA + PostgreSQL (Supabase)
- 前端：Vue 3.5 + Vite 8 + Pinia + Vue Router 5
- 資料庫：Supabase PostgreSQL（僅作為 PostgreSQL 使用，不使用 Supabase Auth / RLS）

## 詞彙表

- **Platform**：日本代購平台系統整體
- **Buyer**：消費者，提交委託單並接收商品的使用者
- **Staff**：後台操作人員，負責審核委託、代購商品、打包出貨
- **Admin**：系統管理員，負責費率設定、使用者管理
- **Commission**：委託單，Buyer 提交的代購請求，包含一或多個商品項目
- **Commission_Item**：委託商品項目，委託單中的單一商品
- **Commission_Service**：加值服務，附加於委託單的檢品、拍照、重新包裝等服務
- **Shipment**：出貨單，將一或多筆已抵台的委託商品合併打包出貨
- **Shipment_Item**：出貨商品項目，出貨單中的單一商品
- **Payment**：付款紀錄，包含預付款（PREPAY）與尾款（FINAL）兩種類型
- **Notification**：通知，站內通知與 Email 通知
- **Message**：站內訊息，Buyer 與 Staff 針對特定委託單的溝通紀錄
- **Fee_Config**：費率設定，平台各項費率設定（直接 UPDATE，不做版本控制）
- **Prepay**：預付款，Buyer 提交委託單時同步支付的預估費用
- **Final_Payment**：尾款，出貨單打包完成後 Buyer 需補繳的實際運費
- **Cancel_Stage**：取消階段，記錄委託單取消時所處的業務階段，決定退款政策
- **Value_Added_Service**：加值服務類型，包含簡易檢品、詳細檢品、拍照、重新包裝、合併包裝
- **AI_Assistant**：AI 輔助功能，提供智慧商品辨識、自動翻譯、費用試算等

## 需求

### 需求 1：使用者註冊與認證

**使用者故事：** 身為 Buyer，我希望能註冊帳號並登入平台，以便提交代購委託單並追蹤訂單狀態。

#### 驗收條件

1. WHEN Buyer 提供有效的 Email 與密碼進行註冊，THE Platform SHALL 在 users 資料表建立帳號紀錄，密碼須經過 BCrypt 雜湊處理後儲存（password_hash）
2. WHEN Buyer 使用已註冊的 Email 與密碼登入，THE Platform SHALL 驗證身份並簽發兩個 JWT：Access Token（短期，30 分鐘）與 Refresh Token（長期，7 天），payload 包含 user_id 與 role
3. THE Platform SHALL 將 JWT 存放於 HttpOnly Cookie，並設定 Secure（僅 HTTPS）、SameSite=Strict，防止 XSS 與 CSRF 攻擊
4. WHEN Access Token 過期，THE Platform SHALL 透過 Refresh Token 自動簽發新的 Access Token，使用者無需重新登入
5. WHEN Refresh Token 也過期，THE Platform SHALL 要求使用者重新登入
6. WHEN 未認證的使用者（未攜帶有效 JWT）嘗試存取受保護的 API，THE Platform SHALL 回傳 HTTP 401 狀態碼
7. IF Buyer 提供的 Email 已被註冊，THEN THE Platform SHALL 回傳明確的錯誤訊息，說明該 Email 已被使用
8. THE Platform SHALL 支援 BUYER、STAFF、ADMIN 三種互斥角色，每位使用者僅能擁有一種角色
9. WHEN 使用者帳號狀態為 DELETED，THE Platform SHALL 永久拒絕該帳號登入，但保留資料庫中所有相關資料（軟刪除），確保歷史委託單等關聯紀錄不受影響

### 需求 1.1：角色權限控制（RBAC）

**使用者故事：** 身為平台管理者，我希望不同角色的使用者只能存取其被授權的功能，以確保資料安全與操作正確性。

#### 權限矩陣

| 操作 | Buyer | Staff | Admin |
|------|:-----:|:-----:|:-----:|
| **commissions** | | | |
| 提交委託並付款 | ✅ | ❌ | ❌ |
| 查看自己的委託 | ✅ | ❌ | ❌ |
| 查看所有委託 | ❌ | ✅ | ✅ |
| 更新採購進度 | ❌ | ✅ | ✅ |
| 取消委託 | ✅ | ❌ | ✅ |
| **commission_services** | | | |
| 選擇加值服務（下單時） | ✅ | ❌ | ❌ |
| 更新服務執行狀態 | ❌ | ✅ | ✅ |
| **shipments** | | | |
| 查看自己的出貨單 | ✅ | ❌ | ❌ |
| 建立 / 管理出貨單 | ❌ | ✅ | ✅ |
| 填入追蹤號碼 | ❌ | ✅ | ✅ |
| **payments** | | | |
| 發起付款 | ✅ | ❌ | ❌ |
| 人工核帳（銀行轉帳） | ❌ | ✅ | ✅ |
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
| 查看費率（計算用） | ❌ | ✅ | ✅ |
| 修改費率 | ❌ | ❌ | ✅ |

#### 驗收條件

1. THE Platform SHALL 在每個 API 端點從 JWT 取得使用者身份與角色，僅允許上述權限矩陣中標記為 ✅ 的操作
2. WHEN 使用者嘗試執行未被授權的操作，THE Platform SHALL 回傳 HTTP 403 狀態碼
3. THE Platform SHALL 確保 Buyer 僅能存取自己的委託單、出貨單、通知等資料，不可存取其他 Buyer 的資料
4. THE Platform SHALL 確保 Staff 可查看所有委託單與出貨單，但不可執行僅限 Admin 的操作（如修改費率、管理用戶）

### 需求 2：收件地址管理

**使用者故事：** 身為 Buyer，我希望能管理多個收件地址，以便在提交委託單時快速選擇寄送地址。

#### 驗收條件

1. THE Platform SHALL 允許 Buyer 新增、編輯、刪除（軟刪除）收件地址
2. WHEN Buyer 新增地址時，THE Platform SHALL 要求填寫收件人姓名、電話、郵遞區號、縣市、區域、街道地址等必填欄位
3. THE Platform SHALL 允許 Buyer 設定一個預設地址，且同一時間僅能有一個預設地址
4. WHEN Buyer 將某地址設為預設，THE Platform SHALL 自動取消原先的預設地址
5. WHEN Buyer 刪除一個已被出貨單引用的地址，THE Platform SHALL 執行軟刪除，確保歷史出貨單的地址參照不受影響

### 需求 3：委託單建立、提交與付款

**使用者故事：** 身為 Buyer，我希望能建立委託單並填入欲代購的日本商品資訊，提交時直接完成付款，以便 Staff 立即開始處理。

#### 驗收條件

1. WHEN Buyer 建立委託單，THE Platform SHALL 要求至少填入一個商品項目，每個項目須包含商品連結（product_url）、數量（quantity，須大於 0）
2. THE Platform SHALL 允許 Buyer 為每個商品項目選填規格（specifications，如顏色、尺寸）、日文商品名稱（product_name_ja）、商品圖片連結、備註
3. THE Platform SHALL 允許 Buyer 選擇加值服務（簡易檢品、詳細檢品、拍照、重新包裝、合併包裝）
4. THE Platform SHALL 允許 Buyer 填寫委託備註（buyer_note）供 Staff 參考
5. WHEN Buyer 提交委託單，THE Platform SHALL 根據當前費率計算預付金額明細（商品成本、服務費、加值服務費），並導向付款流程
6. THE Platform SHALL 透過 ECPay 金流整合支援信用卡、ATM 虛擬帳號、超商代碼等付款方式
7. WHEN Buyer 完成付款，THE Platform SHALL 將委託單狀態設為 PAID，記錄提交時間（submitted_at）與付款時間（paid_at），並建立 Payment 紀錄（類型為 PREPAY，payment_number 格式為 P + 13位時間戳）
8. IF 付款失敗，THEN THE Platform SHALL 將 Payment 狀態設為 FAILED，並允許 Buyer 重新發起付款，委託單不會成立
9. WHEN 使用 ATM 虛擬帳號付款，THE Platform SHALL 儲存 ECPay 回傳的虛擬帳號資訊（銀行代碼、帳號、繳費期限）並顯示給 Buyer
10. THE Platform SHALL 確保同一委託單的預付款紀錄唯一（UNIQUE constraint on commission_id + payment_type）
11. WHEN 委託單狀態變更為 PAID，THE Platform SHALL 使該委託單出現在 Staff 後台的待處理列表中

### 需求 4：商品代購與物流追蹤

**使用者故事：** 身為 Buyer，我希望能即時追蹤委託單的處理進度，以便掌握商品從購買到抵台的每個階段。

#### 驗收條件

1. WHEN Staff 開始處理已付款的委託單，THE Platform SHALL 將委託單狀態從 PAID 變更為 PURCHASING，記錄指派的 Staff（assigned_staff_id）與開始購買時間（purchasing_started_at）
2. WHEN Staff 完成購買，THE Platform SHALL 允許 Staff 填入每個商品的實際購買價格（unit_price_actual_jpy）
3. WHEN 商品抵達日本倉庫，THE Platform SHALL 將委託單狀態變更為 IN_JP_WAREHOUSE，記錄到倉時間（arrived_warehouse_at），並允許 Staff 填入實際重量（weight_actual_g）
4. WHEN 委託單狀態變更為 IN_JP_WAREHOUSE，THE Platform SHALL 自動將狀態推進為 WAITING_SHIPMENT，表示商品可被納入出貨單
5. WHEN 委託單狀態發生變更，THE Platform SHALL 發送對應的站內通知給 Buyer
6. THE Platform SHALL 在 Buyer 的委託單詳情頁面顯示完整的狀態時間軸，包含每個階段的時間戳

### 需求 5：出貨單建立與合併出貨

**使用者故事：** 身為 Buyer，我希望能將多筆已抵台的委託商品合併為一張出貨單，以便節省運費並一次收貨。

#### 驗收條件

1. WHEN Buyer 申請出貨，THE Platform SHALL 列出該 Buyer 所有狀態為 WAITING_SHIPMENT 的委託商品，供 Buyer 勾選要合併出貨的項目
2. THE Platform SHALL 允許 Buyer 選擇收件地址與運送方式（AIR_STANDARD、AIR_EXPRESS、SEA、EMS）
3. WHEN Buyer 提交出貨申請，THE Platform SHALL 建立 Shipment 紀錄，狀態設為 PREPARING
4. THE Platform SHALL 建立對應的 Shipment_Item 紀錄，關聯選中的 Commission_Item
5. THE Platform SHALL 確保同一個 Commission_Item 不能被加入多張出貨單（UNIQUE constraint）
6. THE Platform SHALL 將被納入出貨單的委託單狀態變更為 SHIPPED

### 需求 6：出貨單處理與運費結算

**使用者故事：** 身為 Staff，我希望能處理出貨單的打包、運費計算與出貨流程，以便將商品寄送給 Buyer。

#### 驗收條件

1. WHEN Staff 完成打包，THE Platform SHALL 將出貨單狀態從 PREPARING 變更為 PACKED，記錄總重量（total_weight_g）
2. WHEN 出貨單狀態變更為 PACKED，THE Platform SHALL 計算尾款費用明細（日本境內運費、國際運費、關稅估算、尾款總額），並將狀態變更為 PENDING_PAYMENT
3. WHEN 出貨單狀態為 PENDING_PAYMENT，THE Platform SHALL 通知 Buyer 需補繳運費尾款，並顯示運費明細
4. WHEN Buyer 完成尾款付款，THE Platform SHALL 將出貨單狀態變更為 PAID，建立 Payment 紀錄（類型為 FINAL）
5. WHEN Staff 將包裹交付物流，THE Platform SHALL 將狀態變更為 SHIPPED，記錄出貨時間（shipped_at）、追蹤號碼（tracking_number）、物流商（carrier）
6. THE Platform SHALL 支援出貨單的後續狀態追蹤：SHIPPED → IN_TRANSIT → CUSTOMS → DELIVERED
7. IF 出貨過程發生異常（遺失、損毀），THEN THE Platform SHALL 允許 Staff 將狀態變更為 EXCEPTION，並記錄異常說明

### 需求 7：委託單取消與退款

**使用者故事：** 身為 Buyer，我希望能在適當的時機取消委託單，並依據取消階段獲得相應的退款。

#### 驗收條件

1. WHEN Buyer 在 Staff 開始購買前（PAID 狀態）取消委託，THE Platform SHALL 將狀態變更為 CANCELLED，cancel_stage 設為 BEFORE_PURCHASING，退款金額扣除手續費（依 CANCEL_FEE_RATE_AFTER_PAID 費率）
2. IF Buyer 在 Staff 已開始購買後（PURCHASING 及之後狀態）要求取消，THEN THE Platform SHALL 將 cancel_stage 設為 AFTER_PURCHASING，標記為需個案處理
3. WHEN 委託單取消時，THE Platform SHALL 記錄取消原因（cancel_reason）與取消時間（cancelled_at）
4. WHEN 退款執行時，THE Platform SHALL 更新對應 Payment 的狀態為 REFUNDED 或 PARTIALLY_REFUNDED，記錄退款金額與退款時間

### 需求 8：通知系統

**使用者故事：** 身為 Buyer，我希望在委託單或出貨單狀態變更時收到即時通知，以便掌握最新進度。

#### 驗收條件

1. WHEN 委託單或出貨單狀態發生變更，THE Platform SHALL 建立站內通知紀錄，包含通知類型、標題、內容、關聯實體
2. THE Platform SHALL 透過 Application Layer（Spring Boot Event / Queue）發送 Email 通知，Email 發送狀態不在資料庫追蹤
3. THE Platform SHALL 提供未讀通知數量的即時查詢 API
4. WHEN Buyer 點擊通知，THE Platform SHALL 根據 metadata 中的路由資訊導向對應的詳情頁面
5. THE Platform SHALL 允許 Buyer 將通知標記為已讀，並記錄已讀時間（read_at）
6. WHEN 委託商品進入 WAITING_SHIPMENT 狀態，THE Platform SHALL 發送囤貨期倒數通知，提醒 Buyer 申請出貨

### 需求 9：站內訊息系統

**使用者故事：** 身為 Buyer，我希望能針對特定委託單與 Staff 進行溝通，以便討論商品細節或處理問題。

#### 驗收條件

1. THE Platform SHALL 提供以委託單為單位的訊息聊天功能，Buyer 與 Staff 可互相傳送文字訊息
2. THE Platform SHALL 允許訊息附帶圖片附件（attachments），以 URL 陣列形式儲存
3. WHEN 收到新訊息，THE Platform SHALL 發送站內通知給對方（MESSAGE_RECEIVED 類型）
4. THE Platform SHALL 支援訊息的軟刪除（撤回），記錄刪除時間
5. THE Platform SHALL 提供訊息已讀狀態追蹤，記錄已讀時間（read_at）

### 需求 10：費率管理

**使用者故事：** 身為 Admin，我希望能管理平台各項費率設定，以便靈活調整營運策略。

#### 驗收條件

1. THE Platform SHALL 支援以下費率項目的設定：服務費率、最低服務費、檢品費、日本境內運費、空運費率、海運費率、關稅預估率、免費檢品門檻、取消手續費率
2. WHEN Admin 修改費率，THE Platform SHALL 直接更新該費率項目的值（直接 UPDATE，不做版本控制）
3. THE Platform SHALL 確保每個費率項目只有一筆紀錄（config_key 為 Primary Key）

### 需求 11：後台管理儀表板

**使用者故事：** 身為 Staff，我希望有一個後台管理介面，以便高效處理委託單、出貨單與客戶溝通。

#### 驗收條件

1. THE Platform SHALL 提供 Staff 專用的後台介面，顯示待處理的委託單列表（PAID 狀態，尚未被 Staff 接手），依付款時間排序
2. THE Platform SHALL 提供委託單篩選功能，支援依狀態、日期範圍、Buyer 進行篩選
3. THE Platform SHALL 提供出貨單管理介面，顯示各狀態的出貨單列表
4. THE Platform SHALL 提供 Staff 與 Buyer 的訊息管理介面，可快速查看未讀訊息
5. WHEN Staff 登入後台，THE Platform SHALL 顯示待處理事項的數量摘要（待審核委託、待出貨、待核帳付款等）

### 需求 12：AI 智慧輔助功能

**使用者故事：** 身為 Buyer，我希望平台提供 AI 輔助功能，以便更便利地填寫委託單與了解費用。

#### 驗收條件

1. WHEN Buyer 貼入日本購物網站的商品連結，THE AI_Assistant SHALL 自動擷取商品名稱、價格、圖片、規格等資訊，預填至委託單表單
2. WHEN Buyer 填寫委託單時，THE AI_Assistant SHALL 提供日文商品名稱的中文翻譯建議
3. WHEN Buyer 填寫委託單時，THE AI_Assistant SHALL 根據商品類型與數量提供預估費用試算（含服務費、預估運費、預估關稅）
4. WHEN Buyer 查看委託單列表，THE AI_Assistant SHALL 提供智慧摘要，以簡潔的文字描述每筆委託的當前狀態與下一步行動
5. IF AI_Assistant 無法擷取商品資訊，THEN THE Platform SHALL 顯示提示訊息，引導 Buyer 手動填寫商品資訊
