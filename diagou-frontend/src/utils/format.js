/**
 * Format a date string or Date object to locale string.
 * @param {string|Date} date
 * @param {object} options - Intl.DateTimeFormat options
 * @returns {string}
 */
export function formatDate(date, options = {}) {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...options,
  })
}

/**
 * Format a date string or Date object to include time.
 * @param {string|Date} date
 * @returns {string}
 */
export function formatDateTime(date) {
  if (!date) return ''
  const d = typeof date === 'string' ? new Date(date) : date
  return d.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * Format currency amount with TWD symbol.
 * @param {number} amount
 * @returns {string}
 */
export function formatCurrency(amount) {
  if (amount == null) return ''
  return `NT$ ${Number(amount).toLocaleString('zh-TW')}`
}

/**
 * Format JPY amount.
 * @param {number} amount
 * @returns {string}
 */
export function formatJPY(amount) {
  if (amount == null) return ''
  return `¥ ${Number(amount).toLocaleString('ja-JP')}`
}
