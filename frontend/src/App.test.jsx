import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from './App'

const mocks = vi.hoisted(() => ({
  healthCheck: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  bootstrapAdmin: vi.fn(),
  bookAppointment: vi.fn(),
  getBranches: vi.fn(),
  getDepartments: vi.fn(),
  getDoctors: vi.fn(),
  getDoctorAvailability: vi.fn(),
  joinQueue: vi.fn(),
  getAppointments: vi.fn(),
  getUnreadCount: vi.fn(),
}))

vi.mock('./api', () => ({
  adminApi: {},
  appointmentApi: { getMyAppointments: mocks.getAppointments, book: mocks.bookAppointment },
  authApi: { login: mocks.login, register: mocks.register, bootstrapAdmin: mocks.bootstrapAdmin },
  healthApi: { check: mocks.healthCheck },
  hospitalApi: {
    getBranches: mocks.getBranches,
    getDepartments: mocks.getDepartments,
    getDoctors: mocks.getDoctors,
    getDoctorAvailability: mocks.getDoctorAvailability,
  },
  notificationApi: { unreadCount: mocks.getUnreadCount },
  profileApi: {},
  queueApi: { join: mocks.joinQueue },
  staffApi: {},
}))

beforeEach(() => {
  localStorage.clear()
  mocks.healthCheck.mockResolvedValue({ data: { status: 'UP' } })
  mocks.getAppointments.mockResolvedValue({ data: [] })
  mocks.getUnreadCount.mockResolvedValue({ data: { count: 0 } })
  mocks.getBranches.mockResolvedValue({ data: [] })
  mocks.getDepartments.mockResolvedValue({ data: [] })
  mocks.getDoctors.mockResolvedValue({ data: [] })
  mocks.getDoctorAvailability.mockResolvedValue({ data: { slots: [] } })
  mocks.joinQueue.mockResolvedValue({ data: {} })
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('QueueLess application', () => {
  it('renders the public landing page and its service status', async () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: /skip the waiting room/i })).toBeInTheDocument()
    expect(await screen.findByText('Care platform online')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /get started/i })[0]).toHaveAttribute('href', '/register')
  })

  it('shows a service-unavailable status without breaking the landing page', async () => {
    mocks.healthCheck.mockRejectedValueOnce(new Error('Network unavailable'))

    render(<App />)

    expect(await screen.findByText('Service connection unavailable')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /skip the waiting room/i })).toBeInTheDocument()
  })

  it('submits a patient registration form and stores the authenticated session', async () => {
    const user = userEvent.setup()
    mocks.register.mockResolvedValueOnce({
      data: {
        accessToken: 'test-token',
        user: { id: 8, email: 'patient@example.com', role: 'PATIENT', displayName: 'Taylor Patient' },
      },
    })

    render(<App />)
    await user.click(screen.getAllByRole('link', { name: /get started/i })[0])
    await user.type(screen.getByLabelText(/full name/i), 'Taylor Patient')
    await user.type(screen.getByLabelText(/email address/i), 'patient@example.com')
    await user.type(screen.getByLabelText(/password/i), 'secure-pass-123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(await screen.findByRole('heading', { name: /good day, taylor/i })).toBeInTheDocument()
    expect(mocks.register).toHaveBeenCalledWith({
      displayName: 'Taylor Patient',
      email: 'patient@example.com',
      password: 'secure-pass-123',
    })
    expect(localStorage.getItem('ql_token')).toBe('test-token')
  })

  it('renders the one-time administrator setup form', () => {
    window.history.pushState({}, '', '/setup-admin')
    render(<App />)

    expect(screen.getByRole('heading', { name: /create administrator/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/one-time bootstrap token/i)).toHaveAttribute('type', 'password')
  })

  it('books an appointment without issuing a queue token before check-in', async () => {
    const user = userEvent.setup()
    const slot = '2026-10-28T10:00:00'
    localStorage.setItem('ql_token', 'test-token')
    localStorage.setItem('ql_user', JSON.stringify({ id: 8, role: 'PATIENT', displayName: 'Taylor Patient' }))
    window.history.pushState({}, '', '/app/book')
    mocks.getBranches.mockResolvedValueOnce({ data: [{ id: 1, name: 'City Medical Centre', city: 'Bengaluru' }] })
    mocks.getDepartments.mockResolvedValueOnce({ data: [{ id: 2, name: 'General Medicine' }] })
    mocks.getDoctors.mockResolvedValueOnce({ data: [{ id: 5, fullName: 'Dr. Example', specialization: 'General Medicine' }] })
    mocks.getDoctorAvailability.mockResolvedValueOnce({ data: { slots: [slot] } })
    mocks.bookAppointment.mockResolvedValueOnce({
      data: { id: 12, reference: 'QL-20261028-ABC123', doctorId: 5, startsAt: slot, status: 'CONFIRMED' },
    })

    render(<App />)
    await screen.findByRole('option', { name: /dr\. example/i })
    await user.selectOptions(await screen.findByLabelText('Doctor'), '5')
    await user.click(await screen.findByRole('button', { name: /10:00 am/i }))
    await user.click(screen.getByRole('button', { name: /confirm appointment/i }))

    expect(await screen.findByText(/a receptionist will check you in/i)).toBeInTheDocument()
    expect(screen.getByText(/dr\. example/i)).toBeInTheDocument()
    expect(mocks.bookAppointment).toHaveBeenCalledWith({
      doctorId: 5,
      startsAt: slot,
      notes: null,
    })
    expect(mocks.joinQueue).not.toHaveBeenCalled()
  })
})
