import { useCallback, useEffect, useMemo, useState } from 'react'
import { BrowserRouter, Link, NavLink, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import {
  Activity, ArrowDownRight, ArrowRight, ArrowUpRight, Bell, CalendarDays, Check,
  ChevronDown, Clock3, HeartPulse, LayoutDashboard, LogOut, MapPin, Menu,
  ShieldCheck, Sparkles, Stethoscope, Users, X, Building2, Pencil, Trash2,
} from 'lucide-react'
import {
  adminApi, appointmentApi, authApi, healthApi, hospitalApi, notificationApi,
  profileApi, queueApi, staffApi, waitingListApi, connectQueueUpdates,
} from './api'
import './App.css'

const readStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem('ql_user') || 'null')
  } catch {
    localStorage.removeItem('ql_user')
    return null
  }
}

const getErrorMessage = (error, fallback) => {
  const data = error?.response?.data
  if (data?.message) return data.message
  if (data?.fields) return Object.values(data.fields).join(' ')
  if (data?.error && typeof data.error === 'string') return data.error
  if (!error?.response) return 'Cannot reach the server. Check that the API is running and try again.'
  return fallback
}

const greetingName = (user) => user?.displayName?.trim()?.split(/\s+/)[0] || 'there'
const localDateString = (date) => {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}
const formatDateTime = (value) => value
  ? new Date(value).toLocaleString(undefined, { weekday: 'short', month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })
  : 'Time to be confirmed'

function useSession() {
  const [user, setUser] = useState(readStoredUser)

  const signIn = (response) => {
    localStorage.setItem('ql_token', response.accessToken)
    localStorage.setItem('ql_user', JSON.stringify(response.user))
    setUser(response.user)
  }

  const signOut = () => {
    localStorage.removeItem('ql_token')
    localStorage.removeItem('ql_user')
    setUser(null)
  }

  return { user, signIn, signOut }
}

function ProtectedRoute({ user, roles, children }) {
  if (!localStorage.getItem('ql_token') || !user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={homeForRole(user.role)} replace />
  return children
}

function homeForRole(role) {
  if (role === 'ADMIN') return '/admin'
  if (role === 'STAFF') return '/staff'
  return '/app'
}

function Brand({ inverse = false }) {
  return (
    <Link className={`ql-brand${inverse ? ' ql-brand--inverse' : ''}`} to="/" aria-label="QueueLess AI home">
      <span className="ql-brand-mark"><HeartPulse size={19} aria-hidden="true" /></span>
      <span>QueueLess<span className="ql-brand-ai">AI</span></span>
    </Link>
  )
}

function LandingPage({ user, health }) {
  const [menuOpen, setMenuOpen] = useState(false)
  return (
    <div className="ql-public">
      <header className="ql-public-nav">
        <div className="ql-container ql-nav-inner">
          <Brand />
          <button className="ql-mobile-menu" type="button" onClick={() => setMenuOpen((open) => !open)} aria-label="Toggle navigation">
            {menuOpen ? <X size={21} /> : <Menu size={21} />}
          </button>
          <nav className={`ql-nav-links${menuOpen ? ' is-open' : ''}`} aria-label="Main navigation">
            <a href="#how-it-works">How it works</a>
            <a href="#features">Features</a>
            <a href="#care">Care network</a>
            {user
              ? <Link className="ql-button ql-button--primary ql-nav-cta" to={homeForRole(user.role)}>Open dashboard <ArrowRight size={16} /></Link>
              : <>
                  <Link className="ql-nav-login" to="/login">Sign in</Link>
                  <Link className="ql-button ql-button--primary ql-nav-cta" to="/register">Get started <ArrowRight size={16} /></Link>
                </>}
          </nav>
        </div>
      </header>

      <main>
        <section className="ql-hero">
          <div className="ql-hero-glow" aria-hidden="true" />
          <div className="ql-container ql-hero-grid">
            <motion.div className="ql-hero-copy" initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.55 }}>
              <div className="ql-eyebrow"><span className="ql-pulse" /> SMARTER HOSPITAL VISITS</div>
              <h1>Skip the waiting room.<br /><span>Not your appointment.</span></h1>
              <p>Book a visit, follow your place in line, and spend less time wondering when you’ll be seen. Care that keeps you in the loop.</p>
              <div className="ql-hero-actions">
                <Link className="ql-button ql-button--primary ql-button--large" to={user ? homeForRole(user.role) : '/register'}>
                  Book an appointment <ArrowRight size={17} />
                </Link>
                <a className="ql-button ql-button--quiet ql-button--large" href="#how-it-works">Explore how it works <span className="ql-play">↓</span></a>
              </div>
              <div className="ql-trust-row"><ShieldCheck size={16} /> Private by design <span /> Built for better care</div>
            </motion.div>

            <motion.div className="ql-hero-art" initial={{ opacity: 0, scale: 0.96 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.15, duration: 0.65 }}>
              <div className="ql-art-orbit ql-art-orbit--one" />
              <div className="ql-art-orbit ql-art-orbit--two" />
              <div className="ql-art-cross"><HeartPulse size={67} strokeWidth={1.4} /></div>
              <div className="ql-floating-chip ql-floating-chip--top"><span className="ql-chip-icon"><Check size={14} /></span><span><b>Appointment booked</b><small>Your visit is all set</small></span></div>
              <div className="ql-floating-chip ql-floating-chip--bottom"><span className="ql-live-dot" /><span><b>Queue status</b><small>Live updates when it matters</small></span><ArrowUpRight size={16} /></div>
              <div className="ql-art-caption"><Sparkles size={14} /> Thoughtful care, less waiting</div>
            </motion.div>
          </div>
          <div className="ql-container ql-hero-bottom">
            <div className="ql-service-pill"><span className={`ql-service-indicator ql-service-indicator--${health}`} />{health === 'online' ? 'Care platform online' : health === 'offline' ? 'Service connection unavailable' : 'Checking care platform'}</div>
            <span className="ql-demo-label">A calmer way to navigate your care</span>
          </div>
        </section>

        <section id="how-it-works" className="ql-section ql-section--white">
          <div className="ql-container">
            <div className="ql-section-heading">
              <div className="ql-eyebrow ql-eyebrow--center">CARE, WITHOUT THE GUESSWORK</div>
              <h2>From booking to better <span>peace of mind.</span></h2>
              <p>Every step of your visit, made easier to understand.</p>
            </div>
            <div className="ql-step-grid">
              {[
                { number: '01', icon: CalendarDays, title: 'Choose your care', text: 'Find a department and a time that works for you. Your appointment details stay in one place.' },
                { number: '02', icon: Activity, title: 'Follow your queue', text: 'See your token and an approximate wait so you can plan your arrival with less uncertainty.' },
                { number: '03', icon: HeartPulse, title: 'Focus on feeling better', text: 'Get timely updates when your turn approaches, and keep your visit details close at hand.' },
              ].map(({ number, icon: Icon, title, text }) => (
                <article className="ql-step-card" key={number}>
                  <span className="ql-step-number">{number}</span><span className="ql-step-icon"><Icon size={21} /></span>
                  <h3>{title}</h3><p>{text}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section id="features" className="ql-section ql-feature-section">
          <div className="ql-container ql-feature-grid">
            <div className="ql-feature-copy">
              <div className="ql-eyebrow">YOUR VISIT, MORE VISIBLE</div>
              <h2>More clarity.<br /><span>Less waiting around.</span></h2>
              <p>QueueLess AI brings your appointment and queue information together, so you can make informed plans without needing to stay by the waiting room door.</p>
              <ul className="ql-check-list">
                <li><Check size={16} /> Appointment details, all in one place</li>
                <li><Check size={16} /> A live view of your queue progress</li>
                <li><Check size={16} /> Estimated waits with clear uncertainty</li>
              </ul>
              <Link className="ql-text-link" to={user ? homeForRole(user.role) : '/register'}>Get started <ArrowRight size={16} /></Link>
            </div>
            <div className="ql-queue-preview" aria-label="Illustrative queue preview">
              <div className="ql-preview-top"><div><small>QUEUE OVERVIEW</small><h3>Your visit, at a glance</h3></div><span className="ql-preview-badge"><span /> Sample</span></div>
              <div className="ql-preview-token"><div><small>YOUR TOKEN</small><strong>GM<span>025</span></strong></div><div className="ql-preview-wait"><small>ESTIMATED WAIT</small><b>About 25 min</b></div></div>
              <div className="ql-preview-progress"><div className="ql-progress-label"><span>Queue progress</span><b>4 ahead</b></div><div className="ql-progress-track"><span /></div></div>
              <div className="ql-preview-footer"><span className="ql-live-dot" /> Illustrative preview · waits may change</div>
            </div>
          </div>
        </section>

        <section id="care" className="ql-care-strip">
          <div className="ql-container ql-care-inner">
            <div><div className="ql-eyebrow ql-eyebrow--light">CARE THAT CONNECTS</div><h2>A smoother visit starts<br />with a clearer picture.</h2></div>
            <div className="ql-care-stats">
              <div><Stethoscope size={21} /><b>Find your fit</b><span>Explore care by specialty</span></div>
              <div><Clock3 size={21} /><b>Know what’s next</b><span>Keep track of queue updates</span></div>
              <div><ShieldCheck size={21} /><b>Care with confidence</b><span>Your details stay protected</span></div>
            </div>
          </div>
        </section>
      </main>
      <footer className="ql-footer"><div className="ql-container ql-footer-inner"><Brand /><span>© {new Date().getFullYear()} QueueLess AI · Thoughtful care, better flow.</span><a href="#how-it-works">How it works</a></div></footer>
    </div>
  )
}

function AuthPage({ mode, signIn }) {
  const isRegister = mode === 'register'
  const navigate = useNavigate()
  const [form, setForm] = useState({ displayName: '', email: '', password: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const { data } = isRegister ? await authApi.register(form) : await authApi.login(form)
      signIn(data)
      navigate(homeForRole(data.user.role), { replace: true })
    } catch (requestError) {
      setError(getErrorMessage(requestError, isRegister ? 'We could not create your account.' : 'Email or password is incorrect.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="ql-auth">
      <aside className="ql-auth-aside">
        <Brand inverse />
        <div className="ql-auth-quote">
          <span className="ql-auth-spark"><HeartPulse size={28} /></span>
          <h1>Care feels better<br />when the wait<br /><span>makes sense.</span></h1>
          <p>Keep your appointments, queue updates, and next steps together in one calm place.</p>
          <div className="ql-auth-aside-foot"><ShieldCheck size={16} /> Secure access to your care</div>
        </div>
        <div className="ql-auth-orb ql-auth-orb--one" /><div className="ql-auth-orb ql-auth-orb--two" />
      </aside>
      <section className="ql-auth-main">
        <Link className="ql-back-home" to="/"><ArrowDownRight size={16} /> Back to home</Link>
        <div className="ql-auth-card">
          <span className="ql-auth-icon">{isRegister ? <HeartPulse size={22} /> : <ShieldCheck size={22} />}</span>
          <div className="ql-eyebrow">{isRegister ? 'YOUR CARE JOURNEY STARTS HERE' : 'WELCOME BACK'}</div>
          <h2>{isRegister ? 'Create your account' : 'Sign in to QueueLess'}</h2>
          <p className="ql-auth-subtitle">{isRegister ? 'A few details and you’re ready to go.' : 'Your appointments are right where you left them.'}</p>
          <form className="ql-form" onSubmit={submit}>
            {isRegister && <label className="ql-field"><span>Full name</span><input autoComplete="name" required minLength={2} maxLength={120} value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} placeholder="How should we address you?" /></label>}
            <label className="ql-field"><span>Email address</span><input autoComplete="email" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="you@example.com" /></label>
            <label className="ql-field"><span>Password</span><input autoComplete={isRegister ? 'new-password' : 'current-password'} type="password" required minLength={isRegister ? 8 : undefined} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder={isRegister ? 'At least 8 characters' : 'Enter your password'} /></label>
            {error && <div className="ql-alert" role="alert">{error}</div>}
            <button className="ql-button ql-button--primary ql-button--full ql-button--large" type="submit" disabled={busy}>
              {busy ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'} {!busy && <ArrowRight size={17} />}
            </button>
          </form>
          <p className="ql-auth-switch">{isRegister ? 'Already have an account?' : 'New to QueueLess?'} <Link to={isRegister ? '/login' : '/register'}>{isRegister ? 'Sign in' : 'Create an account'}</Link></p>
          <p className="ql-auth-privacy"><ShieldCheck size={14} /> Your account information is protected.</p>
        </div>
      </section>
    </main>
  )
}

function AdminSetupPage({ signIn }) {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', bootstrapToken: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (event) => {
    event.preventDefault()
    setBusy(true); setError('')
    try {
      const { data } = await authApi.bootstrapAdmin(form)
      signIn(data)
      navigate('/admin', { replace: true })
    } catch (e) { setError(getErrorMessage(e, 'Could not create the administrator account.')) }
    finally { setBusy(false) }
  }
  return <main className="ql-setup-admin"><Brand /><section className="ql-auth-card">
    <div className="ql-eyebrow">ONE-TIME INITIAL SETUP</div><h2>Create administrator</h2>
    <p className="ql-auth-subtitle">Available only before the first admin account exists. Configure the bootstrap token in the backend environment before using this form.</p>
    {error && <ErrorState message={error} />}
    <form className="ql-form" onSubmit={submit}>
      <label className="ql-field"><span>Administrator email</span><input type="email" autoComplete="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></label>
      <label className="ql-field"><span>Password</span><input type="password" autoComplete="new-password" minLength={12} maxLength={72} required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /><small>Use at least 12 characters.</small></label>
      <label className="ql-field"><span>One-time bootstrap token</span><input type="password" autoComplete="off" required value={form.bootstrapToken} onChange={(e) => setForm({ ...form, bootstrapToken: e.target.value })} /></label>
      <button className="ql-button ql-button--primary ql-button--full" disabled={busy}>{busy ? 'Creating account…' : 'Create administrator'}</button>
    </form>
  </section></main>
}

const patientNav = [
  { to: '/app', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/book', label: 'Book a visit', icon: CalendarDays },
  { to: '/app/appointments', label: 'My appointments', icon: Clock3 },
  { to: '/app/queue', label: 'Queue tracker', icon: Activity },
  { to: '/app/waiting-list', label: 'Waiting list', icon: Clock3 },
  { to: '/app/notifications', label: 'Notifications', icon: Bell },
  { to: '/app/profile', label: 'My profile', icon: Users },
]

function PortalLayout({ user, signOut, variant = 'patient' }) {
  const navigate = useNavigate()
  const [mobileNav, setMobileNav] = useState(false)
  const links = variant === 'patient'
    ? patientNav
    : variant === 'staff'
      ? [
          { to: '/staff', label: 'Queue control', icon: Activity, end: true },
          { to: '/staff/notifications', label: 'Notifications', icon: Bell },
        ]
      : [
          { to: '/admin', label: 'Operations overview', icon: LayoutDashboard, end: true },
          { to: '/admin/manage', label: 'Manage hospital', icon: Building2 },
          { to: '/admin/notifications', label: 'Notifications', icon: Bell },
        ]
  const notificationPath = variant === 'patient' ? '/app/notifications' : variant === 'staff' ? '/staff/notifications' : '/admin/notifications'

  const logout = () => {
    signOut()
    navigate('/', { replace: true })
  }

  return (
    <div className="ql-portal">
      <aside className={`ql-sidebar${mobileNav ? ' ql-sidebar--open' : ''}`}>
        <div className="ql-sidebar-brand"><Brand /></div>
        <div className="ql-workspace"><span className="ql-workspace-mark"><HeartPulse size={17} /></span><span><b>QueueLess Care</b><small>{variant === 'patient' ? 'PATIENT SPACE' : variant === 'staff' ? 'CARE TEAM' : 'ADMIN CONSOLE'}</small></span><ChevronDown size={15} /></div>
        <div className="ql-sidebar-label">WORKSPACE</div>
        <nav className="ql-side-links">
          {links.map(({ to, label, icon: Icon, end }) => <NavLink key={to} to={to} end={end} onClick={() => setMobileNav(false)}><Icon size={18} /><span>{label}</span></NavLink>)}
        </nav>
        {variant === 'patient' && <div className="ql-sidebar-help"><span className="ql-help-icon"><ShieldCheck size={17} /></span><b>Here for you</b><p>Your care information is private and accessible only to authorized users.</p></div>}
        <div className="ql-sidebar-bottom"><button className="ql-logout" type="button" onClick={logout}><LogOut size={17} /> Sign out</button></div>
      </aside>
      <div className="ql-portal-main">
        <header className="ql-portal-topbar">
          <button className="ql-mobile-menu ql-portal-menu" type="button" onClick={() => setMobileNav((open) => !open)} aria-label="Toggle menu"><Menu size={20} /></button>
          <div className="ql-breadcrumb"><span>Workspace</span><span>/</span><b>{variant === 'patient' ? 'Patient portal' : variant === 'staff' ? 'Queue management' : 'Operations'}</b></div>
          <div className="ql-topbar-right"><button className="ql-icon-button" type="button" aria-label="Notifications" onClick={() => navigate(notificationPath)}><Bell size={18} /></button><div className="ql-user-chip"><span>{greetingName(user).slice(0, 1).toUpperCase()}</span><div><b>{user?.displayName || user?.email}</b><small>{user?.role?.toLowerCase()}</small></div></div></div>
        </header>
        <main className="ql-page-content"><Outlet /></main>
      </div>
    </div>
  )
}

function PageTitle({ eyebrow, title, subtitle, action }) {
  return <div className="ql-page-title"><div><div className="ql-eyebrow">{eyebrow}</div><h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div>{action}</div>
}

function LoadingState({ label = 'Loading your information…' }) {
  return <div className="ql-state"><span className="ql-spinner" /><span>{label}</span></div>
}

function ErrorState({ message, retry }) {
  return <div className="ql-alert ql-alert--block" role="alert"><span>{message}</span>{retry && <button className="ql-text-link" onClick={retry} type="button">Try again</button>}</div>
}

function EmptyState({ icon: Icon = CalendarDays, title, text, action }) {
  return <div className="ql-empty"><span><Icon size={23} /></span><b>{title}</b><p>{text}</p>{action}</div>
}

function PatientOverview({ user }) {
  const [appointments, setAppointments] = useState(null)
  const [notifications, setNotifications] = useState(0)
  const [error, setError] = useState('')
  const load = () => {
    setError('')
    appointmentApi.getMyAppointments()
      .then(({ data }) => setAppointments(data))
      .catch((e) => setError(getErrorMessage(e, 'Could not load your appointments.')))
    notificationApi.unreadCount().then(({ data }) => setNotifications(data.count || 0)).catch(() => {})
  }
  useEffect(load, [])
  const upcoming = useMemo(() => (appointments || []).filter((item) => item.status === 'CONFIRMED' && new Date(item.startsAt) >= new Date()).sort((a, b) => new Date(a.startsAt) - new Date(b.startsAt)), [appointments])

  return (
    <>
      <PageTitle eyebrow="YOUR CARE, IN ONE PLACE" title={`Good day, ${greetingName(user)}.`} subtitle="Here’s a clear view of what’s next in your care." action={<Link className="ql-button ql-button--primary" to="/app/book"><CalendarDays size={17} /> Book a visit</Link>} />
      <div className="ql-metric-grid">
        <MetricCard icon={CalendarDays} label="Upcoming visits" value={appointments ? upcoming.length : '—'} note="Confirmed appointments" color="teal" />
        <MetricCard icon={Clock3} label="Past visits" value={appointments ? appointments.filter((item) => new Date(item.startsAt) < new Date()).length : '—'} note="In your appointment history" color="blue" />
        <MetricCard icon={Bell} label="New updates" value={notifications} note="Unread notifications" color="violet" />
      </div>
      {error && <ErrorState message={error} retry={load} />}
      <section className="ql-panel ql-upcoming-panel">
        <div className="ql-panel-heading"><div><div className="ql-eyebrow">UP NEXT</div><h2>Your upcoming visits</h2></div><Link className="ql-text-link" to="/app/appointments">View all <ArrowRight size={15} /></Link></div>
        {appointments === null && !error ? <LoadingState /> : upcoming.length ? <div className="ql-appointment-list">{upcoming.slice(0, 3).map((appointment) => <AppointmentRow key={appointment.id} appointment={appointment} />)}</div> : <EmptyState title="Nothing on your calendar yet" text="Find the right care and book a time that works for you." action={<Link className="ql-button ql-button--primary" to="/app/book">Find a time <ArrowRight size={16} /></Link>} />}
      </section>
      <div className="ql-dashboard-note"><ShieldCheck size={17} /><span><b>Your care details stay private.</b> Only you and authorized care staff can access your personal information.</span></div>
    </>
  )
}

function MetricCard({ icon: Icon, label, value, note, color }) {
  return <article className="ql-metric-card"><span className={`ql-metric-icon ql-metric-icon--${color}`}><Icon size={19} /></span><span className="ql-metric-label">{label}</span><strong>{value}</strong><small>{note}</small></article>
}

function AppointmentRow({ appointment, onCancel, busy }) {
  return (
    <article className="ql-appointment-row">
      <div className="ql-date-block"><CalendarDays size={18} /><b>{new Date(appointment.startsAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}</b><small>{new Date(appointment.startsAt).toLocaleDateString(undefined, { weekday: 'short' })}</small></div>
      <div className="ql-appointment-detail"><b>Appointment {appointment.reference}</b><span>{formatDateTime(appointment.startsAt)}</span><small>Doctor #{appointment.doctorId} · Department #{appointment.departmentId}</small></div>
      <span className={`ql-status ql-status--${appointment.status?.toLowerCase()}`}>{appointment.status?.replace('_', ' ')}</span>
      {onCancel && appointment.status === 'CONFIRMED' && <button type="button" className="ql-button ql-button--outline ql-button--small" disabled={busy} onClick={() => onCancel(appointment.reference)}>{busy ? 'Cancelling…' : 'Cancel'}</button>}
    </article>
  )
}

function BookingPage() {
  const [branches, setBranches] = useState([])
  const [departments, setDepartments] = useState([])
  const [doctors, setDoctors] = useState([])
  const [branchId, setBranchId] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [doctorId, setDoctorId] = useState('')
  const [date, setDate] = useState(localDateString(new Date(Date.now() + 86400000)))
  const [slots, setSlots] = useState([])
  const [selectedSlot, setSelectedSlot] = useState('')
  const [notes, setNotes] = useState('')
  const [busy, setBusy] = useState(false)
  const [loadingSlots, setLoadingSlots] = useState(false)
  const [error, setError] = useState('')
  const [confirmation, setConfirmation] = useState(null)

  useEffect(() => {
    hospitalApi.getBranches().then(({ data }) => {
      setBranches(data)
      if (data.length) setBranchId(String(data[0].id))
    }).catch((e) => setError(getErrorMessage(e, 'Could not load hospital branches.')))
  }, [])

  useEffect(() => {
    if (!branchId) return
    setDepartmentId('')
    setDoctorId('')
    setDepartments([])
    setDoctors([])
    hospitalApi.getDepartments(branchId).then(({ data }) => {
      setDepartments(data)
      if (data.length) setDepartmentId(String(data[0].id))
    }).catch((e) => setError(getErrorMessage(e, 'Could not load departments.')))
  }, [branchId])

  useEffect(() => {
    if (!departmentId) return
    setDoctorId('')
    hospitalApi.getDoctors({ branchId, departmentId }).then(({ data }) => setDoctors(data))
      .catch((e) => setError(getErrorMessage(e, 'Could not load doctors.')))
  }, [branchId, departmentId])

  useEffect(() => {
    if (!doctorId || !date) { setSlots([]); return }
    setLoadingSlots(true)
    setSelectedSlot('')
    hospitalApi.getDoctorAvailability(doctorId, date)
      .then(({ data }) => setSlots(data.slots || []))
      .catch((e) => setError(getErrorMessage(e, 'Could not load available times.')))
      .finally(() => setLoadingSlots(false))
  }, [doctorId, date])

  const submit = async (event) => {
    event.preventDefault()
    if (!selectedSlot) return
    setBusy(true)
    setError('')
    try {
      const { data } = await appointmentApi.book({ doctorId: Number(doctorId), startsAt: selectedSlot, notes: notes.trim() || null })
      setConfirmation({ ...data, doctor: doctors.find((doctor) => doctor.id === data.doctorId) })
    } catch (e) {
      setError(getErrorMessage(e, 'We could not book this time. Please choose another slot.'))
    } finally {
      setBusy(false)
    }
  }

  if (confirmation) return <div className="ql-confirmation">
    <span><Check size={28} /></span><div className="ql-eyebrow">APPOINTMENT CONFIRMED</div><h1>You’re all set.</h1>
    <p>Your reference is <b>{confirmation.reference}</b>. Your appointment is saved to your account.</p>
    <div className="ql-confirm-card"><CalendarDays size={18} /><span>{formatDateTime(confirmation.startsAt)}</span></div>
    <div className="ql-confirm-card"><Stethoscope size={18} /><span>{confirmation.doctor?.fullName || `Doctor #${confirmation.doctorId}`} · {confirmation.doctor?.specialization || 'Hospital appointment'}</span></div>
    <p className="ql-hint">A receptionist will check you in and issue your queue token when you arrive for your appointment.</p>
    <div className="ql-confirm-actions"><Link className="ql-button ql-button--primary" to="/app/appointments">View appointments <ArrowRight size={16} /></Link><button className="ql-button ql-button--outline" type="button" onClick={() => { setConfirmation(null); setSelectedSlot('') }}>Book another</button></div>
  </div>

  return (
    <>
      <PageTitle eyebrow="MAKE TIME FOR YOUR HEALTH" title="Book an appointment" subtitle="Choose your hospital, specialty, and a time that suits you." />
      <div className="ql-booking-layout">
        <form className="ql-panel ql-booking-form" onSubmit={submit}>
          <div className="ql-panel-heading ql-panel-heading--compact"><div><h2>Visit details</h2><p>All fields are required unless marked optional.</p></div><span className="ql-step-counter">01 <span>/</span> 03</span></div>
          {error && <ErrorState message={error} />}
          <div className="ql-field"><label htmlFor="branch">Hospital branch</label><select id="branch" required value={branchId} onChange={(e) => setBranchId(e.target.value)}><option value="">Choose a branch</option>{branches.map((branch) => <option value={branch.id} key={branch.id}>{branch.name} · {branch.city}</option>)}</select></div>
          <div className="ql-form-grid">
            <div className="ql-field"><label htmlFor="department">Department</label><select id="department" required value={departmentId} onChange={(e) => setDepartmentId(e.target.value)}><option value="">Choose a department</option>{departments.map((dept) => <option value={dept.id} key={dept.id}>{dept.name}</option>)}</select></div>
            <div className="ql-field"><label htmlFor="doctor">Doctor</label><select id="doctor" required value={doctorId} onChange={(e) => setDoctorId(e.target.value)}><option value="">Choose a doctor</option>{doctors.map((doctor) => <option value={doctor.id} key={doctor.id}>{doctor.fullName}</option>)}</select></div>
          </div>
          <div className="ql-field"><label htmlFor="date">Preferred date</label><input id="date" type="date" min={localDateString(new Date())} required value={date} onChange={(e) => setDate(e.target.value)} /></div>
          <div className="ql-field"><label>Available times</label>
            {loadingSlots ? <LoadingState label="Checking availability…" /> : slots.length ? <div className="ql-slot-grid">{slots.map((slot) => { const time = new Date(slot).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' }); return <button key={slot} type="button" className={`ql-slot${selectedSlot === slot ? ' ql-slot--selected' : ''}`} onClick={() => setSelectedSlot(slot)} aria-pressed={selectedSlot === slot}>{time}</button> })}</div> : <p className="ql-hint">{doctorId ? 'No appointment times are available on this date.' : 'Choose a doctor to see available times.'}</p>}
          </div>
          <div className="ql-field"><label htmlFor="notes">Notes <span>(optional)</span></label><textarea id="notes" maxLength={1000} rows={3} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Anything you would like the clinic to know?" /></div>
          <button className="ql-button ql-button--primary ql-button--full ql-button--large" type="submit" disabled={busy || !selectedSlot}>{busy ? 'Confirming appointment…' : 'Confirm appointment'} <ArrowRight size={17} /></button>
        </form>
        <aside className="ql-booking-aside">
          <div className="ql-booking-help"><span><ShieldCheck size={19} /></span><h3>Your visit, your information</h3><p>Your appointment details are visible only to you and authorized care staff.</p></div>
          <div className="ql-booking-help ql-booking-help--blue"><span><Clock3 size={19} /></span><h3>Plans change</h3><p>You can manage or cancel a confirmed visit from your appointments page. Cancellation rules may apply.</p></div>
          <div className="ql-booking-note"><MapPin size={16} /><span>Choose your branch carefully. Appointment availability varies by location and doctor.</span></div>
        </aside>
      </div>
    </>
  )
}

function QueueTracker() {
  const [tokens, setTokens] = useState(null)
  const [estimates, setEstimates] = useState({})
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    const load = () => queueApi.getMyTokens()
      .then(async ({ data }) => {
        if (!active) return
        setError('')
        setTokens(data)
        const current = data.filter((token) => ['WAITING', 'CALLED', 'IN_SERVICE'].includes(token.status))
        const updates = await Promise.all(current.map(async (token) => {
          try {
            const { data: estimate } = await queueApi.getEstimate(token.queueId, token.id)
            return [token.id, estimate]
          } catch (estimateError) {
            if (active) setError(getErrorMessage(estimateError, 'A wait estimate is temporarily unavailable.'))
            return [token.id, null]
          }
        }))
        if (active) setEstimates(Object.fromEntries(updates.filter(([, estimate]) => estimate)))
      })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError, 'Could not load your queue tokens.')) })

    load()
    const disconnect = connectQueueUpdates('/user/queue/updates', () => { void load() })
    const intervalId = window.setInterval(load, 20000)
    return () => { active = false; window.clearInterval(intervalId); disconnect() }
  }, [])

  const activeTokens = (tokens || []).filter((token) => ['WAITING', 'CALLED', 'IN_SERVICE'].includes(token.status))
  const displayTokens = (tokens || []).slice(0, 10)

  return <>
    <PageTitle eyebrow="YOUR PLACE IN LINE" title="Queue tracker" subtitle="Keep your token and approximate wait close by. Estimates can change as the queue moves." action={<span className="ql-refresh-label"><span className="ql-live-dot" /> Live updates with periodic refresh fallback</span>} />
    {error && <ErrorState message={error} />}
    {!tokens && !error ? <LoadingState label="Checking your queue…" /> : displayTokens.length ? <div className="ql-queue-tracker-grid">
      {displayTokens.map((token) => {
        const estimate = estimates[token.id]
        return <article className="ql-ticket-card" key={token.id}>
          <div className="ql-ticket-top"><span className="ql-eyebrow">DIGITAL QUEUE TOKEN</span><span className={`ql-status ql-status--${token.status.toLowerCase()}`}>{token.status.replace('_', ' ')}</span></div>
          <div className="ql-ticket-number">{token.tokenNumber}</div>
          <p>Appointment #{token.appointmentId ?? 'walk-in'} · Doctor #{token.doctorId ?? 'to be assigned'}</p>
          <div className="ql-ticket-stats">
            <div><small>PATIENTS AHEAD</small><b>{estimate?.patientsAhead ?? (activeTokens.some((item) => item.id === token.id) ? 'Updating' : '—')}</b></div>
            <div><small>ESTIMATED WAIT</small><b>{estimate ? `${estimate.lowerMinutes}–${estimate.upperMinutes} min` : token.status === 'WAITING' ? 'Updating' : '—'}</b></div>
            <div><small>ESTIMATE QUALITY</small><b>{estimate?.confidence || 'Initial'}</b></div>
          </div>
          <div className="ql-ticket-foot">Currently serving: {estimate?.currentlyServingToken || 'No patient'}{estimate?.estimatedArrival ? ` · Approx. arrival ${new Date(estimate.estimatedArrival).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}` : ''}{estimate?.updatedAt ? ` · Updated ${new Date(estimate.updatedAt).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' })}` : ''}</div>
          <div className="ql-ticket-explainer"><Clock3 size={15} /><span>{estimate?.summary || 'Your estimate updates as queue information changes. It is not an exact appointment time.'}</span></div>
          <small className="ql-ticket-foot">Token status: {token.status.replace('_', ' ').toLowerCase()} · Please follow staff instructions.</small>
        </article>
      })}
      <div className="ql-queue-private"><ShieldCheck size={18} /><span>Only your queue tokens are shown here. Queue estimates are approximate and may change.</span></div>
    </div> : <section className="ql-panel"><EmptyState icon={Activity} title="No queue tokens yet" text="After booking, a receptionist checks you in and issues a token for the live queue." action={<Link className="ql-button ql-button--primary" to="/app/appointments">View appointments <ArrowRight size={16} /></Link>} /></section>}
  </>
}

function WaitingListPage() {
  const [departments, setDepartments] = useState([])
  const [doctors, setDoctors] = useState([])
  const [entries, setEntries] = useState(null)
  const [departmentId, setDepartmentId] = useState('')
  const [doctorId, setDoctorId] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const load = () => waitingListApi.list().then(({ data }) => setEntries(data))
    .catch((e) => setError(getErrorMessage(e, 'Could not load your waiting list.')))

  useEffect(() => {
    hospitalApi.getAllDepartments().then(({ data }) => setDepartments(data))
      .catch((e) => setError(getErrorMessage(e, 'Could not load departments.')))
    hospitalApi.getDoctors().then(({ data }) => setDoctors(data))
      .catch((e) => setError(getErrorMessage(e, 'Could not load doctors.')))
    void load()
  }, [])

  const save = async (event) => {
    event.preventDefault()
    setBusy(true); setError(''); setMessage('')
    try {
      await waitingListApi.join({ departmentId: Number(departmentId), doctorId: doctorId ? Number(doctorId) : null })
      setMessage('You have joined the waiting list.')
      setDoctorId('')
      await load()
    } catch (e) { setError(getErrorMessage(e, 'Could not join the waiting list.')) }
    finally { setBusy(false) }
  }
  const cancel = async (id) => {
    setBusy(true); setError(''); setMessage('')
    try { await waitingListApi.cancel(id); setMessage('Waiting-list request cancelled.'); await load() }
    catch (e) { setError(getErrorMessage(e, 'Could not cancel this request.')) }
    finally { setBusy(false) }
  }

  return <>
    <PageTitle eyebrow="FIND AN EARLIER OPENING" title="Waiting list" subtitle="Join a department or doctor’s list. A future opening is not guaranteed." />
    {error && <ErrorState message={error} />}
    {message && <div className="ql-success" role="status"><Check size={16} />{message}</div>}
    <section className="ql-panel">
      <div className="ql-panel-heading"><div><div className="ql-eyebrow">REQUEST AN OPENING</div><h2>Join a waiting list</h2></div></div>
      <form className="ql-queue-select" onSubmit={save}>
        <div className="ql-field"><label htmlFor="waiting-department">Department</label><select required id="waiting-department" value={departmentId} onChange={(e) => { setDepartmentId(e.target.value); setDoctorId('') }}><option value="">Choose a department</option>{departments.map((dept) => <option value={dept.id} key={dept.id}>{dept.name}</option>)}</select></div>
        <div className="ql-field"><label htmlFor="waiting-doctor">Doctor <span>(optional)</span></label><select id="waiting-doctor" value={doctorId} onChange={(e) => setDoctorId(e.target.value)}><option value="">Any doctor</option>{doctors.filter((doctor) => String(doctor.departmentId) === departmentId && doctor.active).map((doctor) => <option value={doctor.id} key={doctor.id}>{doctor.fullName}</option>)}</select></div>
        <button className="ql-button ql-button--primary" disabled={busy || !departmentId}>{busy ? 'Saving…' : 'Join waiting list'} <ArrowRight size={16} /></button>
      </form>
    </section>
    <section className="ql-panel ql-admin-summary">
      <div className="ql-panel-heading"><div><div className="ql-eyebrow">YOUR REQUESTS</div><h2>Waiting-list entries</h2></div></div>
      {!entries ? <LoadingState /> : entries.length ? <div className="ql-waiting-list">{entries.map((entry) => <div className="ql-waiting-row" key={entry.id}><b>{departments.find((d) => d.id === entry.departmentId)?.name || `Department #${entry.departmentId}`}</b><span>{entry.doctorId ? doctors.find((d) => d.id === entry.doctorId)?.fullName || `Doctor #${entry.doctorId}` : 'Any doctor'}</span><span className={`ql-status ql-status--${entry.status.toLowerCase()}`}>{entry.status}</span>{entry.status === 'PENDING' && <button className="ql-button ql-button--outline ql-button--small" type="button" disabled={busy} onClick={() => cancel(entry.id)}>Cancel</button>}</div>)}</div> : <EmptyState icon={Clock3} title="No waiting-list requests" text="Join a department’s list when you are looking for an earlier opening." />}
    </section>
  </>
}

function AppointmentsPage() {
  const [appointments, setAppointments] = useState(null)
  const [error, setError] = useState('')
  const [busyReference, setBusyReference] = useState('')
  const [message, setMessage] = useState('')
  const load = () => {
    setError('')
    appointmentApi.getMyAppointments().then(({ data }) => setAppointments(data))
      .catch((e) => setError(getErrorMessage(e, 'Could not load appointments.')))
  }
  useEffect(load, [])

  const cancel = async (reference) => {
    if (!window.confirm('Cancel this appointment? This action may not be reversible.')) return
    setBusyReference(reference)
    setMessage('')
    setError('')
    try {
      await appointmentApi.cancel(reference)
      setMessage('Your appointment was cancelled.')
      load()
    } catch (e) {
      setError(getErrorMessage(e, 'Could not cancel this appointment.'))
    } finally {
      setBusyReference('')
    }
  }
  const sorted = [...(appointments || [])].sort((a, b) => new Date(b.startsAt) - new Date(a.startsAt))

  return (
    <>
      <PageTitle eyebrow="YOUR VISIT HISTORY" title="My appointments" subtitle="Review upcoming visits and keep track of past appointments." action={<Link className="ql-button ql-button--primary" to="/app/book"><CalendarDays size={17} /> Book a visit</Link>} />
      {message && <div className="ql-success" role="status"><Check size={17} />{message}</div>}
      {error && <ErrorState message={error} retry={load} />}
      <section className="ql-panel">
        <div className="ql-panel-heading"><div><div className="ql-eyebrow">APPOINTMENT HISTORY</div><h2>Your visits</h2></div><span className="ql-record-count">{appointments?.length ?? '—'} records</span></div>
        {appointments === null && !error ? <LoadingState /> : sorted.length ? <div className="ql-appointment-list">{sorted.map((appointment) => <AppointmentRow key={appointment.id} appointment={appointment} onCancel={cancel} busy={busyReference === appointment.reference} />)}</div> : <EmptyState title="No appointments yet" text="Your booked appointments will appear here." action={<Link className="ql-button ql-button--primary" to="/app/book">Book your first visit</Link>} />}
      </section>
    </>
  )
}

function ProfilePage({ user }) {
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  useEffect(() => { profileApi.get().then(({ data }) => setProfile(data)).catch((e) => setError(getErrorMessage(e, 'Could not load profile.'))) }, [])
  const save = async (event) => {
    event.preventDefault()
    setBusy(true); setError(''); setMessage('')
    try {
      const { data } = await profileApi.update({ displayName: profile.displayName, phone: profile.phone })
      setProfile(data); setMessage('Your profile was updated.')
    } catch (e) { setError(getErrorMessage(e, 'Could not update your profile.')) }
    finally { setBusy(false) }
  }
  return <>
    <PageTitle eyebrow="YOUR INFORMATION" title="My profile" subtitle="Manage the details associated with your account." />
    <section className="ql-panel ql-profile-panel">
      <div className="ql-panel-heading"><div><div className="ql-eyebrow">PERSONAL DETAILS</div><h2>Account information</h2></div><span className="ql-profile-avatar">{greetingName(user).slice(0, 1).toUpperCase()}</span></div>
      {error && <ErrorState message={error} />}
      {message && <div className="ql-success" role="status"><Check size={16} />{message}</div>}
      {!profile && !error ? <LoadingState /> : profile && <form className="ql-profile-form" onSubmit={save}>
        <div className="ql-field"><label htmlFor="profile-name">Full name</label><input id="profile-name" minLength={2} maxLength={120} required value={profile.displayName || ''} onChange={(e) => setProfile({ ...profile, displayName: e.target.value })} /></div>
        <div className="ql-field"><label htmlFor="profile-email">Email address</label><input id="profile-email" value={profile.email || ''} disabled /><small>Email address cannot be changed here.</small></div>
        <div className="ql-field"><label htmlFor="profile-phone">Phone number <span>(optional)</span></label><input id="profile-phone" type="tel" value={profile.phone || ''} onChange={(e) => setProfile({ ...profile, phone: e.target.value })} /></div>
        <button className="ql-button ql-button--primary" type="submit" disabled={busy}>{busy ? 'Saving…' : 'Save changes'} <Check size={16} /></button>
      </form>}
    </section>
  </>
}

function StaffDashboard() {
  const [branches, setBranches] = useState([])
  const [departments, setDepartments] = useState([])
  const [branchId, setBranchId] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [queueId, setQueueId] = useState('')
  const [waiting, setWaiting] = useState([])
  const [checkInAppointments, setCheckInAppointments] = useState(null)
  const [waitingEntries, setWaitingEntries] = useState([])
  const [active, setActive] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [checkingInId, setCheckingInId] = useState(null)
  const [notice, setNotice] = useState('')

  useEffect(() => { hospitalApi.getBranches().then(({ data }) => { setBranches(data); if (data.length) setBranchId(String(data[0].id)) }).catch((e) => setError(getErrorMessage(e, 'Could not load branches.'))) }, [])
  useEffect(() => {
    if (!branchId) return
    setDepartments([])
    hospitalApi.getDepartments(branchId).then(({ data }) => { setDepartments(data); if (data.length) setDepartmentId(String(data[0].id)) })
      .catch((e) => setError(getErrorMessage(e, 'Could not load departments.')))
  }, [branchId])
  useEffect(() => {
    if (!departmentId) { setWaitingEntries([]); return }
    staffApi.getWaitingList(departmentId).then(({ data }) => setWaitingEntries(data))
      .catch((e) => setError(getErrorMessage(e, 'Could not load department waiting-list requests.')))
  }, [departmentId])
  const refresh = useCallback(async (id = queueId) => {
    if (!id) return
    try { const { data } = await staffApi.getWaiting(id); setWaiting(data) }
    catch (e) { setError(getErrorMessage(e, 'Could not load the queue.')) }
  }, [queueId])
  const refreshCheckInAppointments = useCallback(async () => {
    if (!branchId || !departmentId) return
    try {
      const { data } = await staffApi.getCheckInAppointments(
        Number(branchId), Number(departmentId), localDateString(new Date()))
      setCheckInAppointments(data)
    } catch (e) {
      setError(getErrorMessage(e, 'Could not load today’s appointments for check-in.'))
    }
  }, [branchId, departmentId])
  useEffect(() => { void refreshCheckInAppointments() }, [refreshCheckInAppointments])
  const openQueue = async (event) => {
    event.preventDefault(); setError(''); setBusy(true)
    try {
      const { data } = await queueApi.resolveQueue(Number(branchId), Number(departmentId))
      setQueueId(data.queueId); await refresh(data.queueId)
      await refreshCheckInAppointments()
    } catch (e) { setError(getErrorMessage(e, 'Could not open this queue.')) }
    finally { setBusy(false) }
  }
  const checkIn = async (appointmentId) => {
    setCheckingInId(appointmentId); setError(''); setNotice('')
    try {
      const { data } = await staffApi.checkInAppointment(appointmentId)
      setNotice(`Checked in successfully. Queue token ${data.tokenNumber} was issued.`)
      await Promise.all([refresh(), refreshCheckInAppointments()])
    } catch (e) {
      setError(getErrorMessage(e, 'Could not check in this appointment.'))
    } finally {
      setCheckingInId(null)
    }
  }
  const action = async (fn, success) => {
    setBusy(true); setError(''); setNotice('')
    try { const { data } = await fn(); if (data?.tokenNumber) setActive(data); setNotice(success); await refresh() }
    catch (e) { setError(getErrorMessage(e, 'Queue action could not be completed.')) }
    finally { setBusy(false) }
  }
  const offerOpening = async (entryId) => {
    setBusy(true); setError(''); setNotice('')
    try {
      await staffApi.offerWaitingListOpening(departmentId, entryId)
      setNotice('The patient has been notified to check for an opening.')
      const { data } = await staffApi.getWaitingList(departmentId)
      setWaitingEntries(data)
    } catch (e) { setError(getErrorMessage(e, 'Could not notify this waiting-list patient.')) }
    finally { setBusy(false) }
  }
  useEffect(() => {
    if (!queueId) return undefined
    const disconnect = connectQueueUpdates(`/topic/queues/${queueId}/updates`, () => {
      void refresh(queueId)
      void refreshCheckInAppointments()
    })
    const intervalId = window.setInterval(() => {
      void refresh(queueId)
      void refreshCheckInAppointments()
    }, 20000)
    return () => { disconnect(); window.clearInterval(intervalId) }
  }, [queueId, refresh, refreshCheckInAppointments])

  return <>
    <PageTitle eyebrow="CARE TEAM WORKSPACE" title="Queue management" subtitle="Keep your department moving with clear, controlled queue actions." />
    {error && <ErrorState message={error} />}
    {notice && <div className="ql-success" role="status"><Check size={16} />{notice}</div>}
    <section className="ql-panel ql-queue-control">
      <div className="ql-panel-heading"><div><div className="ql-eyebrow">ACTIVE SERVICE LANE</div><h2>Choose a queue</h2></div><span className="ql-live-pill"><span /> LIVE CONTROL</span></div>
      <form className="ql-queue-select" onSubmit={openQueue}>
        <div className="ql-field"><label htmlFor="staff-branch">Branch</label><select id="staff-branch" value={branchId} required onChange={(e) => setBranchId(e.target.value)}>{branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}</select></div>
        <div className="ql-field"><label htmlFor="staff-department">Department</label><select id="staff-department" value={departmentId} required onChange={(e) => setDepartmentId(e.target.value)}>{departments.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}</select></div>
        <button className="ql-button ql-button--primary" type="submit" disabled={busy || !departmentId}>{busy ? 'Opening…' : 'Open queue'} <ArrowRight size={16} /></button>
      </form>
    </section>
    <section className="ql-panel ql-admin-summary">
      <div className="ql-panel-heading"><div><div className="ql-eyebrow">ARRIVING PATIENTS</div><h2>Today’s appointments for check-in</h2></div><button className="ql-button ql-button--outline ql-button--small" type="button" disabled={!departmentId} onClick={refreshCheckInAppointments}>Refresh</button></div>
      <p className="ql-hint">Check-in is available starting 60 minutes before the appointment and ends at its scheduled time.</p>
      {!checkInAppointments ? <LoadingState label="Loading appointments…" /> : checkInAppointments.length ? <div className="ql-waiting-list">
        {checkInAppointments.map((appointment) => <div className="ql-waiting-row ql-checkin-row" key={appointment.id}>
          <span className="ql-position"><CalendarDays size={16} /></span>
          <b>{appointment.reference}</b>
          <span>{formatDateTime(appointment.startsAt)}</span>
          <span>Doctor #{appointment.doctorId}</span>
          <button className="ql-button ql-button--primary ql-button--small" type="button" disabled={checkingInId !== null || busy} onClick={() => checkIn(appointment.id)}>{checkingInId === appointment.id ? 'Checking in…' : 'Check in'}</button>
        </div>)}
      </div> : <EmptyState title="No confirmed appointments today" text="Confirmed appointments for this branch and department will appear here." />}
    </section>
    {queueId && <div className="ql-staff-grid">
      <section className="ql-panel ql-current-token"><div className="ql-eyebrow">CURRENT TOKEN</div><h2>{active?.tokenNumber || '— —'}</h2><p>{active?.status ? active.status.replace('_', ' ') : 'Call the next patient when you are ready.'}</p><button className="ql-button ql-button--primary ql-button--full" disabled={busy || !waiting.length} onClick={() => action(() => staffApi.callNext(queueId), 'Next patient called.')}>Call next patient <ArrowRight size={16} /></button>
        {active && <div className="ql-active-actions">
          <button className="ql-button ql-button--outline" disabled={busy || active.status !== 'CALLED'} onClick={() => action(() => staffApi.startConsultation(active.id), 'Consultation started.')}>Start consultation</button>
          <button className="ql-button ql-button--outline" disabled={busy || active.status !== 'IN_SERVICE'} onClick={() => action(() => staffApi.completeConsultation(active.id), 'Consultation completed.')}>Complete</button>
          <button className="ql-button ql-button--outline" disabled={busy || !['CALLED', 'IN_SERVICE'].includes(active.status)} onClick={() => action(() => staffApi.markNoShow(active.id), 'Patient marked as a no-show.')}>Mark no-show</button>
          <button className="ql-button ql-button--outline" disabled={busy || active.status !== 'CALLED'} onClick={() => action(() => staffApi.cancelToken(active.id, 'Cancelled by staff'), 'Queue token cancelled.')}>Cancel token</button>
        </div>}
      </section>
      <section className="ql-panel"><div className="ql-panel-heading"><div><div className="ql-eyebrow">WAITING ROOM</div><h2>Waiting patients <span className="ql-count-badge">{waiting.length}</span></h2></div><button className="ql-button ql-button--outline ql-button--small" onClick={() => refresh()} type="button">Refresh</button></div>
        {waiting.length ? <div className="ql-waiting-list">{waiting.map((token, index) => <div className="ql-waiting-row ql-staff-token-row" key={token.id}>
          <span className="ql-position">{String(index + 1).padStart(2, '0')}</span><b>{token.tokenNumber}</b>
          <span>{token.estimatedWaitMinutes ?? '—'} min est.</span>
          <span className={`ql-status ql-status--${token.status.toLowerCase()}`}>{token.status}</span>
          <span className="ql-token-actions">
            <button className="ql-button ql-button--outline ql-button--small" type="button" disabled={busy} onClick={() => action(() => staffApi.markNoShow(token.id), `${token.tokenNumber} marked as a no-show.`)}>No-show</button>
            <button className="ql-button ql-button--outline ql-button--small" type="button" disabled={busy} onClick={() => action(() => staffApi.cancelToken(token.id, 'Cancelled by staff'), `${token.tokenNumber} cancelled.`)}>Cancel</button>
          </span>
        </div>)}</div> : <EmptyState title="The queue is clear" text="Waiting patients will appear here when they join the queue." />}
      </section>
    </div>}
    {departmentId && <section className="ql-panel ql-admin-summary">
      <div className="ql-panel-heading"><div><div className="ql-eyebrow">EARLIER APPOINTMENT REQUESTS</div><h2>Department waiting list</h2></div><span className="ql-record-count">{waitingEntries.length} pending</span></div>
      <p className="ql-hint">Notify a patient when a slot may have opened. This message does not reserve a slot; the patient still needs to book an available time.</p>
      {waitingEntries.length ? <div className="ql-waiting-list">{waitingEntries.map((entry, index) => <div className="ql-waiting-row" key={entry.id}><span className="ql-position">{String(index + 1).padStart(2, '0')}</span><b>{entry.doctorId ? `Doctor #${entry.doctorId}` : 'Any doctor'}</b><span>Requested {new Date(entry.requestedAt).toLocaleDateString()}</span><button className="ql-button ql-button--outline ql-button--small" type="button" disabled={busy} onClick={() => offerOpening(entry.id)}>Notify patient</button></div>)}</div> : <EmptyState icon={Clock3} title="No pending requests" text="Patients who request an earlier appointment will appear here." />}
    </section>}
  </>
}

function NotificationsPage() {
  const [notifications, setNotifications] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const load = () => notificationApi.list().then(({ data }) => setNotifications(data))
    .catch((e) => setError(getErrorMessage(e, 'Could not load notifications.')))
  useEffect(() => { void load() }, [])
  const markRead = async (id) => {
    setBusy(true); setError('')
    try { await notificationApi.markRead(id); await load() }
    catch (e) { setError(getErrorMessage(e, 'Could not update this notification.')) }
    finally { setBusy(false) }
  }
  const markAllRead = async () => {
    setBusy(true); setError('')
    try { await notificationApi.markAllRead(); await load() }
    catch (e) { setError(getErrorMessage(e, 'Could not mark notifications as read.')) }
    finally { setBusy(false) }
  }
  return <>
    <PageTitle eyebrow="ACCOUNT UPDATES" title="Notifications" subtitle="Important appointment, queue, and waiting-list updates for your account." action={<button className="ql-button ql-button--outline" type="button" disabled={busy || !notifications?.some((item) => !item.read)} onClick={markAllRead}>Mark all read</button>} />
    {error && <ErrorState message={error} retry={load} />}
    <section className="ql-panel">
      {notifications === null && !error ? <LoadingState /> : notifications?.length ? <div className="ql-notification-list">{notifications.map((item) => <article className={`ql-notification-row${item.read ? '' : ' is-unread'}`} key={item.id}>
        <span className="ql-notification-icon"><Bell size={17} /></span><div><b>{item.title}</b><p>{item.body}</p><small>{new Date(item.createdAt).toLocaleString()}</small></div>
        {!item.read && <button className="ql-button ql-button--outline ql-button--small" type="button" disabled={busy} onClick={() => markRead(item.id)}>Mark read</button>}
      </article>)}</div> : <EmptyState icon={Bell} title="You’re all caught up" text="New updates will appear here." />}
    </section>
  </>
}

function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')
  const load = () => adminApi.getAnalytics().then(({ data }) => setStats(data)).catch((e) => setError(getErrorMessage(e, 'Could not load operations analytics.')))
  useEffect(load, [])
  const metrics = stats ? [
    { icon: CalendarDays, label: 'Total appointments', value: stats.totalAppointments, color: 'teal', note: `${stats.confirmedAppointments} confirmed` },
    { icon: Users, label: 'Waiting patients', value: stats.waitingPatients, color: 'blue', note: 'Across active queues' },
    { icon: Stethoscope, label: 'Active doctors', value: stats.activeDoctors, color: 'violet', note: 'Available in directory' },
    { icon: Activity, label: 'Completion rate', value: `${stats.appointmentCompletionRate}%`, color: 'amber', note: 'Based on recorded visits' },
  ] : []
  return <>
    <PageTitle eyebrow="HOSPITAL OPERATIONS" title="Operations overview" subtitle="A real-time summary of activity recorded in QueueLess AI." action={<button className="ql-button ql-button--outline" type="button" onClick={load}>Refresh data <Activity size={16} /></button>} />
    {error && <ErrorState message={error} retry={load} />}
    {!stats && !error ? <LoadingState label="Loading operational data…" /> : stats && <>
      <div className="ql-metric-grid">{metrics.map((metric) => <MetricCard key={metric.label} {...metric} />)}</div>
      <section className="ql-panel ql-admin-summary"><div className="ql-panel-heading"><div><div className="ql-eyebrow">SERVICE HEALTH</div><h2>Appointments & queue activity</h2></div><span className="ql-record-count">Database-backed totals</span></div>
        <div className="ql-summary-bars">{[
          ['Confirmed appointments', stats.confirmedAppointments, 'teal'],
          ['Completed appointments', stats.completedAppointments, 'blue'],
          ['Cancelled appointments', stats.cancelledAppointments, 'amber'],
          ['Completed consultations', stats.completedConsultations, 'violet'],
          ['No-show tokens', stats.noShows, 'rose'],
        ].map(([label, value, color]) => <div className="ql-summary-bar-row" key={label}><div><span>{label}</span><b>{value}</b></div><div className="ql-summary-track"><span className={`ql-summary-fill ql-summary-fill--${color}`} style={{ width: `${Math.min(100, stats.totalAppointments ? Number(value) / Number(stats.totalAppointments) * 100 : 0)}%` }} /></div></div>)}</div>
      </section>
      <div className="ql-dashboard-note"><ShieldCheck size={17} /><span>Analytics use records currently stored in the database. Percentages show 0 when there are no eligible records.</span></div>
    </>}
  </>
}

const managementFields = {
  branches: [
    { key: 'name', label: 'Branch name', required: true },
    { key: 'address', label: 'Street address', required: true },
    { key: 'city', label: 'City', required: true },
    { key: 'timezone', label: 'Timezone', required: true, placeholder: 'Asia/Kolkata' },
    { key: 'phone', label: 'Phone', required: false },
  ],
  departments: [
    { key: 'branchId', label: 'Branch', type: 'branch', required: true },
    { key: 'name', label: 'Department name', required: true },
    { key: 'description', label: 'Description', required: false },
  ],
  doctors: [
    { key: 'branchId', label: 'Branch', type: 'branch', required: true },
    { key: 'departmentId', label: 'Department', type: 'department', required: true },
    { key: 'fullName', label: 'Doctor name', required: true },
    { key: 'specialization', label: 'Specialization', required: true },
    { key: 'qualification', label: 'Qualification', required: false },
    { key: 'consultationMinutes', label: 'Consultation minutes', type: 'number', required: true },
    { key: 'active', label: 'Active', type: 'checkbox' },
  ],
  staff: [
    { key: 'email', label: 'Staff email', type: 'email', required: true },
    { key: 'password', label: 'Temporary password', type: 'password', required: true, minLength: 12 },
    { key: 'branchId', label: 'Branch', type: 'branch', required: true },
    { key: 'departmentId', label: 'Department', type: 'department', required: true },
  ],
}

const managementTitles = { branches: 'Branches', departments: 'Departments', doctors: 'Doctors', staff: 'Staff accounts' }
const managementSingular = { branches: 'Branch', departments: 'Department', doctors: 'Doctor', staff: 'Staff' }

function AdminManagementPage() {
  const [tab, setTab] = useState('branches')
  const [records, setRecords] = useState([])
  const [branches, setBranches] = useState([])
  const [departments, setDepartments] = useState([])
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState({})
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  const loadSupportingOptions = async () => {
    try {
      const [branchResult, departmentResult] = await Promise.all([
        adminApi.getBranches(), adminApi.getDepartments(),
      ])
      setBranches(branchResult.data)
      setDepartments(departmentResult.data)
    } catch (e) { setError(getErrorMessage(e, 'Could not load hospital options.')) }
  }
  const loadTab = async (target = tab) => {
    setError('')
    try {
      const loaders = {
        branches: adminApi.getBranches,
        departments: adminApi.getDepartments,
        doctors: adminApi.getDoctors,
        staff: adminApi.getStaff,
      }
      const { data } = await loaders[target]()
      setRecords(data)
    } catch (e) { setError(getErrorMessage(e, `Could not load ${managementTitles[target].toLowerCase()}.`)) }
  }
  useEffect(() => { void loadTab(tab); void loadSupportingOptions() }, [tab])

  const startCreate = (target = tab) => {
    setEditingId(null)
    setForm(target === 'doctors' ? { active: true, consultationMinutes: 15 } : {})
    setNotice('')
  }
  const startEdit = (record) => {
    setEditingId(record.id)
    const editable = Object.fromEntries(managementFields[tab].map(({ key }) => [key, record[key] ?? '']))
    setForm(editable)
    setNotice('')
  }
  const save = async (event) => {
    event.preventDefault()
    setBusy(true); setError(''); setNotice('')
    const payload = { ...form }
    if (payload.branchId) payload.branchId = Number(payload.branchId)
    if (payload.departmentId) payload.departmentId = Number(payload.departmentId)
    if (payload.consultationMinutes) payload.consultationMinutes = Number(payload.consultationMinutes)
    try {
      if (tab === 'staff' && editingId) {
        await adminApi.updateStaff(editingId, { branchId: payload.branchId, departmentId: payload.departmentId })
      } else if (tab === 'staff') {
        await adminApi.createStaff(payload)
      } else if (editingId) {
        await adminApi[`update${managementSingular[tab]}`](editingId, payload)
      } else {
        await adminApi[`create${managementSingular[tab]}`](payload)
      }
      const successMessage = editingId ? 'Changes saved.' : `${managementSingular[tab]} added.`
      startCreate(tab)
      setNotice(successMessage)
      await loadTab()
      await loadSupportingOptions()
    } catch (e) { setError(getErrorMessage(e, 'Could not save this record.')) }
    finally { setBusy(false) }
  }
  const remove = async (record) => {
    if (!window.confirm(`Delete ${record.name || record.fullName || record.email}? This may be blocked if the record is in use.`)) return
    setBusy(true); setError(''); setNotice('')
    try {
      await adminApi[`delete${managementSingular[tab]}`](record.id)
      setNotice('Record deleted.')
      await loadTab()
      await loadSupportingOptions()
    } catch (e) { setError(getErrorMessage(e, 'Could not delete this record.')) }
    finally { setBusy(false) }
  }
  const toggleStaff = async (record) => {
    setBusy(true); setError('')
    try {
      await adminApi.updateStaff(record.id, { active: !record.active })
      await loadTab()
    } catch (e) { setError(getErrorMessage(e, 'Could not update staff access.')) }
    finally { setBusy(false) }
  }
  const submitLabel = editingId ? 'Save changes' : tab === 'staff' ? 'Create staff account' : `Add ${managementTitles[tab].slice(0, -1).toLowerCase()}`

  return <>
    <PageTitle eyebrow="HOSPITAL DIRECTORY & ACCESS" title="Manage hospital" subtitle="Maintain the directory and staff access used by patient booking and queue operations." />
    <div className="ql-admin-tabs" role="tablist" aria-label="Hospital management">
      {Object.keys(managementTitles).map((key) => <button key={key} type="button" role="tab" aria-selected={tab === key} className={tab === key ? 'is-active' : ''} onClick={() => { setTab(key); startCreate(key); setError('') }}>{managementTitles[key]}</button>)}
    </div>
    {error && <ErrorState message={error} />}
    {notice && <div className="ql-success" role="status"><Check size={16} />{notice}</div>}
    <section className="ql-panel">
      <div className="ql-panel-heading"><div><div className="ql-eyebrow">{editingId ? 'EDIT RECORD' : 'ADD A RECORD'}</div><h2>{editingId ? `Edit ${managementTitles[tab].toLowerCase().replace(/s$/, '')}` : `Create ${managementTitles[tab].toLowerCase().replace(/s$/, '')}`}</h2></div>{editingId && <button className="ql-button ql-button--outline ql-button--small" type="button" onClick={startCreate}>Cancel edit</button>}</div>
      <form className="ql-admin-form" onSubmit={save}>
        {managementFields[tab].map((field) => <div className="ql-field" key={field.key}>
          {field.type === 'checkbox'
            ? <label className="ql-checkbox"><input type="checkbox" checked={Boolean(form[field.key])} onChange={(e) => setForm({ ...form, [field.key]: e.target.checked })} />{field.label}</label>
            : <>
              <label htmlFor={`manage-${field.key}`}>{field.label}</label>
              {field.type === 'branch' || field.type === 'department'
                ? <select id={`manage-${field.key}`} required={field.required} value={form[field.key] || ''} onChange={(e) => setForm({ ...form, [field.key]: e.target.value, ...(field.key === 'branchId' && ['doctors', 'staff'].includes(tab) ? { departmentId: '' } : {}) })}><option value="">Choose {field.label.toLowerCase()}</option>{(field.type === 'branch' ? branches : departments.filter((item) => !['doctors', 'staff'].includes(tab) || !form.branchId || String(item.branchId) === String(form.branchId))).map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}</select>
                : <input id={`manage-${field.key}`} type={field.type || 'text'} required={field.required && !(tab === 'staff' && editingId && field.key === 'password')} disabled={tab === 'staff' && editingId && field.key === 'email'} minLength={field.minLength} min={field.type === 'number' ? 1 : undefined} value={form[field.key] ?? ''} placeholder={field.placeholder} onChange={(e) => setForm({ ...form, [field.key]: e.target.value })} />}
            </>}
        </div>)}
        <button className="ql-button ql-button--primary" disabled={busy}>{busy ? 'Saving…' : submitLabel} <Check size={16} /></button>
      </form>
      {tab === 'staff' && !editingId && <p className="ql-hint">Share temporary staff credentials through a secure channel and have the staff member change their password outside this screen. Password reset is not currently available in the app.</p>}
    </section>
    <section className="ql-panel ql-admin-summary">
      <div className="ql-panel-heading"><div><div className="ql-eyebrow">CURRENT RECORDS</div><h2>{managementTitles[tab]}</h2></div><span className="ql-record-count">{records.length} records</span></div>
      {!records.length ? <EmptyState icon={Building2} title={`No ${managementTitles[tab].toLowerCase()} yet`} text="Create a record above to add it to the hospital workspace." /> : <div className="ql-management-list">{records.map((record) => <div className="ql-management-row" key={record.id}>
        <div><b>{record.name || record.fullName || record.email}</b><small>{tab === 'branches' ? `${record.city || ''} · ${record.timezone || ''}` : tab === 'departments' ? `${branches.find((b) => b.id === record.branchId)?.name || `Branch #${record.branchId}`} · ${record.description || ''}` : tab === 'doctors' ? `${record.specialization} · ${record.consultationMinutes} min` : `${record.role} · ${record.active ? 'Active' : 'Inactive'} · ${record.branchId ? `Branch #${record.branchId} · Department #${record.departmentId}` : 'No assignment'}`}</small></div>
        <div className="ql-management-actions">
          {tab === 'staff' && <button className="ql-button ql-button--outline ql-button--small" type="button" disabled={busy} onClick={() => toggleStaff(record)}>{record.active ? 'Deactivate' : 'Reactivate'}</button>}
          <button className="ql-icon-button" type="button" aria-label={`Edit ${record.name || record.fullName || record.email}`} disabled={busy} onClick={() => startEdit(record)}><Pencil size={16} /></button>
          <button className="ql-icon-button ql-icon-button--danger" type="button" aria-label={`Delete ${record.name || record.fullName || record.email}`} disabled={busy} onClick={() => remove(record)}><Trash2 size={16} /></button>
        </div>
      </div>)}</div>}
    </section>
  </>
}

function AppRoutes() {
  const { user, signIn, signOut } = useSession()
  const [health, setHealth] = useState('checking')
  useEffect(() => { let active = true; healthApi.check().then(() => active && setHealth('online')).catch(() => active && setHealth('offline')); return () => { active = false } }, [])

  return (
    <Routes>
      <Route path="/" element={<LandingPage user={user} health={health} />} />
      <Route path="/login" element={user ? <Navigate to={homeForRole(user.role)} replace /> : <AuthPage mode="login" signIn={signIn} />} />
      <Route path="/register" element={user ? <Navigate to={homeForRole(user.role)} replace /> : <AuthPage mode="register" signIn={signIn} />} />
      <Route path="/setup-admin" element={user ? <Navigate to={homeForRole(user.role)} replace /> : <AdminSetupPage signIn={signIn} />} />
      <Route path="/app" element={<ProtectedRoute user={user} roles={['PATIENT']}><PortalLayout user={user} signOut={signOut} /></ProtectedRoute>}>
        <Route index element={<PatientOverview user={user} />} />
        <Route path="book" element={<BookingPage />} />
        <Route path="appointments" element={<AppointmentsPage />} />
        <Route path="queue" element={<QueueTracker />} />
        <Route path="waiting-list" element={<WaitingListPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="profile" element={<ProfilePage user={user} />} />
      </Route>
      <Route path="/staff" element={<ProtectedRoute user={user} roles={['STAFF', 'ADMIN']}><PortalLayout user={user} signOut={signOut} variant="staff" /></ProtectedRoute>}>
        <Route index element={<StaffDashboard />} />
        <Route path="notifications" element={<NotificationsPage />} />
      </Route>
      <Route path="/admin" element={<ProtectedRoute user={user} roles={['ADMIN']}><PortalLayout user={user} signOut={signOut} variant="admin" /></ProtectedRoute>}>
        <Route index element={<AdminDashboard />} />
        <Route path="manage" element={<AdminManagementPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return <BrowserRouter><AppRoutes /></BrowserRouter>
}
