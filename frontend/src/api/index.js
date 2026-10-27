import axios from 'axios'
import { Client } from '@stomp/stompjs'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// Request interceptor: attach JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ql_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Response interceptor: handle 401 globally
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('ql_token')
      localStorage.removeItem('ql_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// ── Auth
export const authApi = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  bootstrapAdmin: (data) => api.post('/auth/bootstrap-admin', data),
}

// ── Profile
export const profileApi = {
  get: () => api.get('/me'),
  update: (data) => api.put('/me', data),
}

// ── Hospital
export const hospitalApi = {
  getBranches: () => api.get('/branches'),
  getBranch: (id) => api.get(`/branches/${id}`),
  getDepartments: (branchId) => api.get(`/branches/${branchId}/departments`),
  getAllDepartments: () => api.get('/departments'),
  getDoctors: (params) => api.get('/doctors', { params }),
  getDoctor: (id) => api.get(`/doctors/${id}`),
  getDoctorAvailability: (id, date) => api.get(`/doctors/${id}/availability`, { params: { date } }),
}

// ── Appointments
export const appointmentApi = {
  book: (data) => api.post('/appointments', data),
  getMyAppointments: () => api.get('/appointments/my'),
  getById: (id) => api.get(`/appointments/${id}`),
  cancel: (id) => api.patch(`/appointments/${id}/cancel`),
}

// ── Queue
export const queueApi = {
  join: (data) => api.post('/queues/join', data),
  getMyTokens: () => api.get('/queues/my'),
  getStatus: (queueId) => api.get(`/queues/${queueId}/status`),
  getEstimate: (queueId, tokenId) => api.get(`/queues/${queueId}/tokens/${tokenId}/estimate`),
  resolveQueue: (branchId, departmentId) => api.post('/queues/resolve', { branchId, departmentId }),
}

// ── Waiting list
export const waitingListApi = {
  list: () => api.get('/waiting-list'),
  join: (data) => api.post('/waiting-list', data),
  cancel: (id) => api.delete(`/waiting-list/${id}`),
}

// ── Staff
export const staffApi = {
  getCheckInAppointments: (branchId, departmentId, date) => api.get('/staff/appointments', { params: { branchId, departmentId, date } }),
  checkInAppointment: (appointmentId) => api.post(`/staff/appointments/${appointmentId}/check-in`),
  callNext: (queueId) => api.post(`/staff/queues/${queueId}/call-next`),
  startConsultation: (tokenId) => api.post(`/staff/tokens/${tokenId}/start`),
  completeConsultation: (tokenId) => api.post(`/staff/tokens/${tokenId}/complete`),
  skipToken: (tokenId, reason) => api.post(`/staff/tokens/${tokenId}/skip`, { reason }),
  markNoShow: (tokenId) => api.post(`/staff/tokens/${tokenId}/no-show`),
  cancelToken: (tokenId, reason) => api.post(`/staff/tokens/${tokenId}/cancel`, { reason }),
  recallToken: (tokenId) => api.post(`/staff/tokens/${tokenId}/recall`),
  getWaiting: (queueId) => api.get(`/staff/queues/${queueId}/waiting`),
  getWaitingList: (departmentId) => api.get(`/staff/departments/${departmentId}/waiting-list`),
  offerWaitingListOpening: (departmentId, entryId) => api.post(`/staff/departments/${departmentId}/waiting-list/${entryId}/offer-opening`),
}

// ── Notifications
export const notificationApi = {
  list: () => api.get('/notifications'),
  unreadCount: () => api.get('/notifications/unread-count'),
  markAllRead: () => api.post('/notifications/mark-all-read'),
  markRead: (id) => api.post(`/notifications/${id}/read`),
}

// ── Admin
export const adminApi = {
  // Analytics
  getAnalytics: () => api.get('/admin/analytics'),
  getAuditLogs: (page = 0, size = 50) => api.get('/admin/audit-logs', { params: { page, size } }),
  getAppointments: () => api.get('/admin/appointments'),

  // Branches
  getBranches: () => api.get('/admin/branches'),
  createBranch: (data) => api.post('/admin/branches', data),
  updateBranch: (id, data) => api.put(`/admin/branches/${id}`, data),
  deleteBranch: (id) => api.delete(`/admin/branches/${id}`),

  // Departments
  getDepartments: () => api.get('/admin/departments'),
  createDepartment: (data) => api.post('/admin/departments', data),
  updateDepartment: (id, data) => api.put(`/admin/departments/${id}`, data),
  deleteDepartment: (id) => api.delete(`/admin/departments/${id}`),

  // Doctors
  getDoctors: () => api.get('/admin/doctors'),
  createDoctor: (data) => api.post('/admin/doctors', data),
  updateDoctor: (id, data) => api.put(`/admin/doctors/${id}`, data),
  deleteDoctor: (id) => api.delete(`/admin/doctors/${id}`),

  // Staff
  getStaff: () => api.get('/admin/staff'),
  createStaff: (data) => api.post('/admin/staff', data),
  updateStaff: (id, data) => api.put(`/admin/staff/${id}`, data),
  deleteStaff: (id) => api.delete(`/admin/staff/${id}`),
}

export const connectQueueUpdates = (destination, onUpdate) => {
  const token = localStorage.getItem('ql_token')
  if (!token) return () => {}
  const apiBase = import.meta.env.VITE_API_BASE_URL
  const url = new URL('/ws-native', apiBase?.startsWith('http') ? apiBase : window.location.origin)
  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  const client = new Client({
    brokerURL: url.toString(),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    onConnect: () => client.subscribe(destination, onUpdate),
  })
  client.activate()
  return () => { void client.deactivate() }
}

// ── Health
export const healthApi = {
  check: () => api.get('/health'),
}

export default api
