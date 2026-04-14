import client from './client'

export const authApi = {
  register: (data) => client.post('/auth/register', data),
  login: (data) => client.post('/auth/login', data),
  logout: () => client.post('/auth/logout'),
  refresh: () => client.post('/auth/refresh'),
  me: () => client.get('/auth/me'),
  updateProfile: (data) => client.put('/auth/me', data),
  sendCode: (type, target) => client.post('/auth/verification/send', { type, target }),
  verifyCode: (type, target, code) => client.post('/auth/verification/verify', { type, target, code }),
}
