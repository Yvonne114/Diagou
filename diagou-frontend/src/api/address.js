import client from './client'

export const addressApi = {
  list: () => client.get('/addresses'),
  create: (data) => client.post('/addresses', data),
  update: (id, data) => client.put(`/addresses/${id}`, data),
  remove: (id) => client.delete(`/addresses/${id}`),
  setDefault: (id) => client.patch(`/addresses/${id}/default`),
}
