import { useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import { divIcon } from 'leaflet'
import { MapContainer, Marker, Popup, Rectangle, TileLayer } from 'react-leaflet'
import ManagerWorkspace from './manager/ManagerWorkspace'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const TOKEN_KEY = 'reserve-web-token'

const authConfig = (token) => ({ headers: { Authorization: `Bearer ${token}` } })
const reserveCenter = (reserve) => [reserve.centerLatitude ?? ((reserve.area.minLatitude + reserve.area.maxLatitude) / 2), reserve.centerLongitude ?? ((reserve.area.minLongitude + reserve.area.maxLongitude) / 2)]
const reserveBounds = (reserve) => [[reserve.area.minLatitude, reserve.area.minLongitude], [reserve.area.maxLatitude, reserve.area.maxLongitude]]
const formatDate = (value) => value ? new Date(value).toLocaleString() : 'Not available'
const priorityStyle = (priority) => priority === 'HIGH'
  ? { badge: 'priority-high' }
  : priority === 'MEDIUM'
    ? { badge: 'priority-medium' }
    : { badge: 'priority-low' }

const adminPinIcon = (active, selected) => divIcon({
  className: 'admin-pin-wrapper',
  html: `<span class="admin-map-pin ${active ? 'admin-map-pin-active' : 'admin-map-pin-inactive'} ${selected ? 'admin-map-pin-selected' : ''}"></span>`,
  iconSize: [24, 32],
  iconAnchor: [12, 28]
})

const emptyLoginForm = { email: '', password: '' }
const emptySignupForm = { name: '', email: '', password: '' }
const emptyAdminRequestFilters = { search: '', status: '', requestedByUserId: '' }

const normalizeReserveName = (value) => (value || '')
  .toLowerCase()
  .replace(/\bnature reserve\b/g, ' ')
  .replace(/\breserve\b/g, ' ')
  .replace(/[^a-z0-9 ]/g, ' ')
  .replace(/\s+/g, ' ')
  .trim()

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) || '')
  const [profile, setProfile] = useState(null)
  const [authMode, setAuthMode] = useState('login')
  const [loginForm, setLoginForm] = useState(emptyLoginForm)
  const [signupForm, setSignupForm] = useState(emptySignupForm)
  const [adminFilters, setAdminFilters] = useState({ search: '', managerUserId: '', region: '', hasOpenEvents: '', active: '' })
  const [adminRequestFilters, setAdminRequestFilters] = useState(emptyAdminRequestFilters)
  const [adminPage, setAdminPage] = useState('catalog')
  const [adminViewMode, setAdminViewMode] = useState('table')
  const [adminReserveDetailView, setAdminReserveDetailView] = useState('map')
  const [adminUsers, setAdminUsers] = useState([])
  const [adminReserves, setAdminReserves] = useState([])
  const [adminReserveRequests, setAdminReserveRequests] = useState([])
  const [adminReserveDetail, setAdminReserveDetail] = useState(null)
  const [adminSelectedReserveId, setAdminSelectedReserveId] = useState(null)
  const [adminRequestAssignments, setAdminRequestAssignments] = useState({})
  const [reserves, setReserves] = useState([])
  const [events, setEvents] = useState([])
  const [reservePoisByReserveId, setReservePoisByReserveId] = useState({})
  const [reservePoiTypesByReserveId, setReservePoiTypesByReserveId] = useState({})
  const [reserveRequests, setReserveRequests] = useState([])
  const [inactiveReserveSuggestions, setInactiveReserveSuggestions] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const regionOptions = useMemo(() => [...new Set(adminReserves.map((reserve) => reserve.region).filter(Boolean))].sort(), [adminReserves])
  const adminRequestUserOptions = useMemo(() => {
    const byId = new Map()
    adminReserveRequests.forEach((request) => {
      if (!byId.has(request.requestedByUserId)) {
        byId.set(request.requestedByUserId, {
          id: request.requestedByUserId,
          name: request.requestedByName,
          email: request.requestedByEmail
        })
      }
    })
    return Array.from(byId.values()).sort((left, right) => left.name.localeCompare(right.name))
  }, [adminReserveRequests])
  const filteredAdminRequests = useMemo(() => adminReserveRequests.filter((request) => {
    const search = adminRequestFilters.search.trim().toLowerCase()
    const matchesSearch = !search
      || request.reserveName.toLowerCase().includes(search)
      || request.requestedByName.toLowerCase().includes(search)
      || request.requestedByEmail.toLowerCase().includes(search)
      || request.message.toLowerCase().includes(search)
    const matchesStatus = !adminRequestFilters.status || request.status === adminRequestFilters.status
    const matchesUser = !adminRequestFilters.requestedByUserId || String(request.requestedByUserId) === adminRequestFilters.requestedByUserId
    return matchesSearch && matchesStatus && matchesUser
  }), [adminRequestFilters, adminReserveRequests])

  async function loadManagerPoiData(activeToken, reserveList) {
    const poiEntries = await Promise.all(reserveList.map(async (reserve) => {
      const [poisResponse, poiTypesResponse] = await Promise.all([
        axios.get(`${API_BASE}/api/reserves/${reserve.id}/pois`, authConfig(activeToken)),
        axios.get(`${API_BASE}/api/reserves/${reserve.id}/poi-types`, authConfig(activeToken))
      ])
      return [reserve.id, { pois: poisResponse.data, poiTypes: poiTypesResponse.data }]
    }))

    const poisByReserveId = {}
    const poiTypesByReserveId = {}

    poiEntries.forEach(([reserveId, data]) => {
      poisByReserveId[reserveId] = data.pois
      poiTypesByReserveId[reserveId] = data.poiTypes
    })

    setReservePoisByReserveId(poisByReserveId)
    setReservePoiTypesByReserveId(poiTypesByReserveId)
  }

  async function loadManagerDashboard(activeToken) {
    const [profileResponse, reservesResponse, eventsResponse, requestsResponse, inactiveSuggestionsResponse] = await Promise.all([
      axios.get(`${API_BASE}/api/auth/me`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/reserves`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/events`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/reserve-requests/mine`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/reserves/inactive-suggestions`, authConfig(activeToken))
    ])
    setProfile(profileResponse.data)
    setReserves(reservesResponse.data)
    setEvents(eventsResponse.data)
    setReserveRequests(requestsResponse.data)
    setInactiveReserveSuggestions(inactiveSuggestionsResponse.data)
    await loadManagerPoiData(activeToken, reservesResponse.data)
  }

  async function loadAdminDashboard(activeToken) {
    const params = {}
    if (adminFilters.search.trim()) params.search = adminFilters.search.trim()
    if (adminFilters.managerUserId) params.managerUserId = Number(adminFilters.managerUserId)
    if (adminFilters.region.trim()) params.region = adminFilters.region.trim()
    if (adminFilters.hasOpenEvents) params.hasOpenEvents = adminFilters.hasOpenEvents === 'true'
    if (adminFilters.active) params.active = adminFilters.active === 'true'

    const [profileResponse, usersResponse, requestsResponse, reservesResponse] = await Promise.all([
      axios.get(`${API_BASE}/api/auth/me`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/admin/users`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/admin/reserve-requests`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/admin/reserves`, { ...authConfig(activeToken), params })
    ])

    setProfile(profileResponse.data)
    setAdminUsers(usersResponse.data)
    setAdminReserveRequests(requestsResponse.data)
    setAdminReserves(reservesResponse.data)
    setAdminSelectedReserveId((current) => reservesResponse.data.some((reserve) => reserve.id === current) ? current : reservesResponse.data[0]?.id ?? null)
  }

  async function loadAdminReserveDetail(activeToken, reserveId) {
    const response = await axios.get(`${API_BASE}/api/admin/reserves/${reserveId}`, authConfig(activeToken))
    setAdminReserveDetail(response.data)
  }

  useEffect(() => {
    if (!token) {
      setProfile(null)
      setReserves([])
      setEvents([])
      setReservePoisByReserveId({})
      setReservePoiTypesByReserveId({})
      setReserveRequests([])
      setInactiveReserveSuggestions([])
      setAdminUsers([])
      setAdminReserves([])
      setAdminReserveRequests([])
      setAdminReserveDetail(null)
      setAdminRequestAssignments({})
      return
    }

    setLoading(true)
    axios.get(`${API_BASE}/api/auth/me`, authConfig(token))
      .then(({ data }) => data.role === 'ADMIN' ? loadAdminDashboard(token) : loadManagerDashboard(token))
      .catch((requestError) => {
        setError(requestError.response?.data?.message || 'Failed to load your dashboard.')
        if (requestError.response?.status === 401) handleLogout()
      })
      .finally(() => setLoading(false))
  }, [token])

  useEffect(() => {
    if (token && profile?.role === 'ADMIN') {
      loadAdminDashboard(token).catch((requestError) => setError(requestError.response?.data?.message || 'Failed to refresh admin data.'))
    }
  }, [token, profile?.role, adminFilters])

  useEffect(() => {
    if (token && profile?.role === 'ADMIN' && adminSelectedReserveId) {
      loadAdminReserveDetail(token, adminSelectedReserveId)
        .catch((requestError) => setError(requestError.response?.data?.message || 'Failed to load reserve details.'))
    }
  }, [token, profile?.role, adminSelectedReserveId])

  useEffect(() => {
    if (profile?.role !== 'ADMIN' || adminReserves.length === 0 || adminReserveRequests.length === 0) {
      return
    }

    setAdminRequestAssignments((current) => {
      const next = { ...current }
      let changed = false

      adminReserveRequests.forEach((request) => {
        if (request.status !== 'OPEN' || next[request.id]) {
          return
        }

        const matchedReserve = adminReserves.find((reserve) => normalizeReserveName(reserve.displayName || reserve.name) === normalizeReserveName(request.reserveName))
        if (matchedReserve) {
          next[request.id] = String(matchedReserve.id)
          changed = true
        }
      })

      return changed ? next : current
    })
  }, [adminReserveRequests, adminReserves, profile?.role])

  function handleLogout() {
    localStorage.removeItem(TOKEN_KEY)
    setToken('')
    setProfile(null)
    setError('')
    setNotice('')
  }

  async function handleLogin(event) {
    event.preventDefault()
    setError('')
    try {
      const response = await axios.post(`${API_BASE}/api/auth/login`, loginForm)
      localStorage.setItem(TOKEN_KEY, response.data.token)
      setToken(response.data.token)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Sign-in failed.')
    }
  }

  async function handleSignup(event) {
    event.preventDefault()
    setError('')
    try {
      const response = await axios.post(`${API_BASE}/api/auth/signup`, signupForm)
      localStorage.setItem(TOKEN_KEY, response.data.token)
      setToken(response.data.token)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Sign-up failed.')
    }
  }

  async function handleCreateReserveRequest(requestForm) {
    await axios.post(`${API_BASE}/api/reserve-requests`, requestForm, authConfig(token))
    await loadManagerDashboard(token)
  }

  async function handleCreateEvent(reserveId, eventForm) {
    await axios.post(`${API_BASE}/api/events`, {
      reserveId,
      ...eventForm,
      status: 'OPEN',
      latitude: Number(eventForm.latitude),
      longitude: Number(eventForm.longitude)
    }, authConfig(token))
    await loadManagerDashboard(token)
  }

  async function updateRequestStatus(id, status) {
    setError('')
    try {
      await axios.patch(`${API_BASE}/api/admin/reserve-requests/${id}`, null, { ...authConfig(token), params: { status } })
      await loadAdminDashboard(token)
      setNotice(`Request marked as ${status.toLowerCase()}.`)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Failed to update request.')
    }
  }

  async function approveReserveRequest(request) {
    const selectedReserveId = adminRequestAssignments[request.id]
    if (!selectedReserveId) {
      setError('Select a reserve before approving this request.')
      return
    }

    setError('')
    try {
      await axios.patch(`${API_BASE}/api/admin/reserves/${Number(selectedReserveId)}/assignment`, {
        managerUserId: request.requestedByUserId,
        reserveRequestId: request.id
      }, authConfig(token))
      await loadAdminDashboard(token)
      setNotice(`Assigned ${request.reserveName} to ${request.requestedByName}.`)
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Failed to approve reserve request.')
    }
  }

  async function updateEventStatus(id, status) {
    await axios.patch(`${API_BASE}/api/events/${id}/status`, null, { ...authConfig(token), params: { status } })
    await loadManagerDashboard(token)
  }

  async function updateEventPriority(id, priority) {
    await axios.patch(`${API_BASE}/api/events/${id}/priority`, null, { ...authConfig(token), params: { priority } })
    await loadManagerDashboard(token)
  }

  async function updateEventPublish(id, published) {
    await axios.patch(`${API_BASE}/api/events/${id}/publish`, null, { ...authConfig(token), params: { published } })
    await loadManagerDashboard(token)
  }

  async function refreshCurrentView() {
    if (!token || !profile?.role) {
      return
    }

    setError('')
    try {
      if (profile.role === 'ADMIN') {
        await loadAdminDashboard(token)
        if (adminSelectedReserveId) {
          await loadAdminReserveDetail(token, adminSelectedReserveId)
        }
      } else {
        await loadManagerDashboard(token)
      }
      setNotice('Dashboard refreshed.')
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Failed to refresh the current view.')
      if (requestError.response?.status === 401) {
        handleLogout()
      }
    }
  }

  async function createReservePoi(reserveId, poiForm) {
    await axios.post(`${API_BASE}/api/reserves/${reserveId}/pois`, {
      ...poiForm,
      typeId: Number(poiForm.typeId),
      latitude: Number(poiForm.latitude),
      longitude: Number(poiForm.longitude)
    }, authConfig(token))
    await loadManagerDashboard(token)
  }

  async function updateReservePoi(reserveId, poiId, poiForm) {
    await axios.put(`${API_BASE}/api/reserves/${reserveId}/pois/${poiId}`, {
      ...poiForm,
      typeId: Number(poiForm.typeId),
      latitude: Number(poiForm.latitude),
      longitude: Number(poiForm.longitude)
    }, authConfig(token))
    await loadManagerDashboard(token)
  }

  async function deleteReservePoi(reserveId, poiId) {
    await axios.delete(`${API_BASE}/api/reserves/${reserveId}/pois/${poiId}`, authConfig(token))
    await loadManagerDashboard(token)
  }

  async function createReservePoiType(reserveId, typeForm) {
    const response = await axios.post(`${API_BASE}/api/reserves/${reserveId}/poi-types`, {
      name: typeForm.name
    }, authConfig(token))
    await loadManagerDashboard(token)
    return response.data
  }

  async function updateReservePoiType(reserveId, typeId, typeForm) {
    await axios.put(`${API_BASE}/api/reserves/${reserveId}/poi-types/${typeId}`, {
      name: typeForm.name
    }, authConfig(token))
    await loadManagerDashboard(token)
  }

  async function deleteReservePoiType(reserveId, typeId) {
    await axios.delete(`${API_BASE}/api/reserves/${reserveId}/poi-types/${typeId}`, authConfig(token))
    await loadManagerDashboard(token)
  }

  if (!token) {
    return (
      <main className="shell shell-login">
        <section className="login-card">
          <p className="eyebrow">Reserve Control Platform</p>
          <h1>Secure sign in</h1>
          <p className="muted">Managers handle reserve events. The admin manages the static reserve catalog and assignments.</p>
          <div className="auth-switch">
            <button type="button" className={authMode === 'login' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setAuthMode('login')}>Sign in</button>
            <button type="button" className={authMode === 'signup' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setAuthMode('signup')}>Sign up</button>
          </div>
          {authMode === 'login' ? (
            <form className="login-form" onSubmit={handleLogin}>
              <label>Email<input type="email" value={loginForm.email} onChange={(event) => setLoginForm((current) => ({ ...current, email: event.target.value }))} required /></label>
              <label>Password<input type="password" value={loginForm.password} onChange={(event) => setLoginForm((current) => ({ ...current, password: event.target.value }))} required /></label>
              <button type="submit">Sign in</button>
            </form>
          ) : (
            <form className="login-form" onSubmit={handleSignup}>
              <label>Full name<input type="text" value={signupForm.name} onChange={(event) => setSignupForm((current) => ({ ...current, name: event.target.value }))} required /></label>
              <label>Email<input type="email" value={signupForm.email} onChange={(event) => setSignupForm((current) => ({ ...current, email: event.target.value }))} required /></label>
              <label>Password<input type="password" minLength="8" value={signupForm.password} onChange={(event) => setSignupForm((current) => ({ ...current, password: event.target.value }))} required /></label>
              <button type="submit">Create manager account</button>
            </form>
          )}
          {error ? <p className="error-banner">{error}</p> : null}
        </section>
      </main>
    )
  }

  if (profile?.role !== 'ADMIN') {
    return (
      <ManagerWorkspace
        apiBase={API_BASE}
        profile={profile}
        loading={loading}
        error={error}
        notice={notice}
        reserves={reserves}
        events={events}
        reservePoisByReserveId={reservePoisByReserveId}
        reservePoiTypesByReserveId={reservePoiTypesByReserveId}
        reserveRequests={reserveRequests}
        inactiveReserveSuggestions={inactiveReserveSuggestions}
        onClearError={() => setError('')}
        onClearNotice={() => setNotice('')}
        onLogout={handleLogout}
        onRefresh={refreshCurrentView}
        onCreateReserveRequest={async (requestForm) => {
          setError('')
          try {
            await handleCreateReserveRequest(requestForm)
            setNotice('Reserve request sent to the admin.')
          } catch (requestError) {
            setError(requestError.response?.data?.message || 'Failed to send reserve request.')
          }
        }}
        onCreateEvent={async (reserveId, eventForm) => {
          setError('')
          try {
            await handleCreateEvent(reserveId, eventForm)
            setNotice('Event created successfully.')
          } catch (requestError) {
            setError(requestError.response?.data?.message || 'Failed to create event.')
          }
        }}
        onUpdateEventStatus={updateEventStatus}
        onUpdateEventPriority={updateEventPriority}
        onUpdateEventPublish={updateEventPublish}
        onCreateReservePoi={async (reserveId, poiForm) => {
          setError('')
          try {
            await createReservePoi(reserveId, poiForm)
            setNotice('POI created successfully.')
          } catch (requestError) {
            setError(requestError.response?.data?.message || 'Failed to create POI.')
          }
        }}
        onUpdateReservePoi={async (reserveId, poiId, poiForm) => {
          setError('')
          try {
            await updateReservePoi(reserveId, poiId, poiForm)
            setNotice('POI updated successfully.')
          } catch (requestError) {
            setError(requestError.response?.data?.message || 'Failed to update POI.')
          }
        }}
        onDeleteReservePoi={async (reserveId, poiId) => {
          setError('')
          try {
            await deleteReservePoi(reserveId, poiId)
            setNotice('POI deleted successfully.')
          } catch (requestError) {
            setError(requestError.response?.data?.message || 'Failed to delete POI.')
          }
        }}
        onCreateReservePoiType={async (reserveId, typeForm) => {
          setError('')
          try {
            await createReservePoiType(reserveId, typeForm)
            setNotice('POI type created successfully.')
          } catch (requestError) {
            setError(requestError.response?.data?.message || 'Failed to create POI type.')
          }
        }}
        onUpdateReservePoiType={async (reserveId, typeId, typeForm) => {
          setError('')
          try {
            await updateReservePoiType(reserveId, typeId, typeForm)
            setNotice('POI type updated successfully.')
          } catch (requestError) {
            setError(requestError.response?.data?.message || 'Failed to update POI type.')
          }
        }}
        onDeleteReservePoiType={async (reserveId, typeId) => {
          setError('')
          try {
            await deleteReservePoiType(reserveId, typeId)
            setNotice('POI type deleted successfully.')
          } catch (requestError) {
            setError(requestError.response?.data?.message || 'Failed to delete POI type.')
          }
        }}
      />
    )
  }

  function openAdminReserve(reserveId) {
    setAdminSelectedReserveId(reserveId)
    setAdminPage('reserve-details')
  }

  return (
    <main className="shell">
      <header className="topbar">
        <div><p className="eyebrow">Administrator Console</p><h1>Reserve Catalog</h1><p className="muted">Signed in as <strong>{profile.name}</strong> ({profile.email})</p></div>
        <div className="topbar-center-action">
          <button className="refresh-button" type="button" onClick={refreshCurrentView}>Refresh</button>
        </div>
        <div className="topbar-actions">
          <div className="summary-pill"><span>{adminReserves.length}</span><small>Catalog reserves</small></div>
          <div className="summary-pill"><span>{adminReserves.filter((reserve) => reserve.active).length}</span><small>Active reserves</small></div>
          <div className="summary-pill"><span>{adminReserveRequests.filter((request) => request.status === 'OPEN').length}</span><small>Open requests</small></div>
          <button className="secondary-button" type="button" onClick={handleLogout}>Sign out</button>
        </div>
      </header>
      {error ? <p className="error-banner">{error}</p> : null}
      {notice ? <p className="status-note">{notice}</p> : null}
      {loading ? <p className="status-note">Loading the admin console...</p> : null}
      <nav className="manager-nav admin-nav">
        <button type="button" className={adminPage === 'catalog' ? 'manager-nav-button manager-nav-button-active' : 'manager-nav-button'} onClick={() => setAdminPage('catalog')}>
          <strong>Catalog</strong>
          <small>Reserves and request history</small>
        </button>
        <button type="button" className={adminPage === 'reserve-details' ? 'manager-nav-button manager-nav-button-active' : 'manager-nav-button'} onClick={() => setAdminPage('reserve-details')} disabled={!adminSelectedReserveId}>
          <strong>Reserve details</strong>
          <small>{adminReserveDetail ? adminReserveDetail.displayName || adminReserveDetail.name : 'Open from the catalog'}</small>
        </button>
      </nav>
      {adminPage === 'catalog' ? (
        <section className="content-grid admin-dashboard-layout">
          <article className="panel stack">
            <section>
              <div className="panel-heading">
                <div><p className="eyebrow">Inventory</p><h2>Reserve catalog</h2><p className="muted">Click any reserve to open its dedicated details tab.</p></div>
                <div className="view-toggle">
                  <button type="button" className={adminViewMode === 'table' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setAdminViewMode('table')}>Table view</button>
                  <button type="button" className={adminViewMode === 'map' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setAdminViewMode('map')}>Map view</button>
                </div>
              </div>
              <form className="event-form admin-reserve-filters" onSubmit={(event) => event.preventDefault()}>
                <label>Search<input type="text" value={adminFilters.search} onChange={(event) => setAdminFilters((current) => ({ ...current, search: event.target.value }))} placeholder="Reserve, manager, or email" /></label>
                <label>Manager<select value={adminFilters.managerUserId} onChange={(event) => setAdminFilters((current) => ({ ...current, managerUserId: event.target.value }))}><option value="">All managers</option>{adminUsers.map((user) => <option key={user.id} value={user.id}>{user.name}</option>)}</select></label>
                <label>Region<select value={adminFilters.region} onChange={(event) => setAdminFilters((current) => ({ ...current, region: event.target.value }))}><option value="">All regions</option>{regionOptions.map((region) => <option key={region} value={region}>{region}</option>)}</select></label>
                <label>Activity<select value={adminFilters.active} onChange={(event) => setAdminFilters((current) => ({ ...current, active: event.target.value }))}><option value="">Active and inactive</option><option value="true">Active only</option><option value="false">Inactive only</option></select></label>
                <label>Open events<select value={adminFilters.hasOpenEvents} onChange={(event) => setAdminFilters((current) => ({ ...current, hasOpenEvents: event.target.value }))}><option value="">All reserves</option><option value="true">Only reserves with open events</option><option value="false">Only reserves with no open events</option></select></label>
              </form>
              {adminViewMode === 'table' ? (
                <div className="table-shell">
                  <table className="reserve-table">
                    <thead><tr><th>Name</th><th>Region</th><th>Status</th><th>Manager</th><th>Open events</th></tr></thead>
                    <tbody>{adminReserves.map((reserve) => <tr key={reserve.id} className={reserve.id === adminSelectedReserveId ? 'reserve-row reserve-row-active' : 'reserve-row'} onClick={() => openAdminReserve(reserve.id)}><td>{reserve.displayName || reserve.name}</td><td>{reserve.region}</td><td><span className={reserve.active ? 'status-chip status-active' : 'status-chip status-inactive'}>{reserve.active ? 'Active' : 'Inactive'}</span></td><td>{reserve.managerName || 'Unassigned'}</td><td>{reserve.openEvents}</td></tr>)}</tbody>
                  </table>
                </div>
              ) : (
                <div className="map-wrap map-wrap-full">
                  <div className="map-legend"><span><i className="legend-dot legend-dot-active" /> Active reserve</span><span><i className="legend-dot legend-dot-inactive" /> Inactive reserve</span></div>
                  <MapContainer key={`admin-map-${adminSelectedReserveId ?? 'all'}`} center={adminReserveDetail ? reserveCenter(adminReserveDetail) : [31.5, 35.0]} zoom={8} scrollWheelZoom className="reserve-map reserve-map-focus">
                    <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                    {adminReserves.map((reserve) => <Marker key={reserve.id} position={reserveCenter(reserve)} icon={adminPinIcon(reserve.active, reserve.id === adminSelectedReserveId)} eventHandlers={{ click: () => openAdminReserve(reserve.id) }}><Popup><strong>{reserve.displayName || reserve.name}</strong><br />{reserve.active ? `Assigned to ${reserve.managerName}` : 'Inactive and unassigned'}</Popup></Marker>)}
                  </MapContainer>
                </div>
              )}
            </section>
          </article>
          <article className="panel stack">
            <section>
              <div className="panel-heading"><div><p className="eyebrow">Request history</p><h2>Manager assignment requests</h2><p className="muted">Approve a request by assigning the requester to a reserve, or reject it directly from here.</p></div></div>
              <form className="event-form request-filter-form" onSubmit={(event) => event.preventDefault()}>
                <label className="event-form-wide">Search<input type="text" value={adminRequestFilters.search} onChange={(event) => setAdminRequestFilters((current) => ({ ...current, search: event.target.value }))} placeholder="Reserve name, requester, email, or message" /></label>
                <label>Status<select value={adminRequestFilters.status} onChange={(event) => setAdminRequestFilters((current) => ({ ...current, status: event.target.value }))}><option value="">All statuses</option><option value="OPEN">Open</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option></select></label>
                <label>Requester<select value={adminRequestFilters.requestedByUserId} onChange={(event) => setAdminRequestFilters((current) => ({ ...current, requestedByUserId: event.target.value }))}><option value="">All managers</option>{adminRequestUserOptions.map((user) => <option key={user.id} value={user.id}>{user.name} ({user.email})</option>)}</select></label>
              </form>
              <div className="event-list">{filteredAdminRequests.length === 0 ? <p className="empty-state">No requests match the current filters.</p> : filteredAdminRequests.map((request) => {
                const selectedReserveId = adminRequestAssignments[request.id] || ''
                return (
                  <article key={request.id} className="event-card admin-request-card">
                    <div className="event-card-header">
                      <div><strong>{request.reserveName}</strong><span>{request.requestedByName} ({request.requestedByEmail})</span></div>
                      <span className={`status-chip status-${request.status.toLowerCase()}`}>{request.status}</span>
                    </div>
                    <p>{request.message}</p>
                    <div className="event-meta">
                      <span>Requested at: {formatDate(request.createdAt)}</span>
                      {request.resolvedAt ? <span>Resolved at: {formatDate(request.resolvedAt)}</span> : null}
                    </div>
                    {request.status === 'OPEN' ? (
                      <div className="admin-request-actions">
                        <label>
                          Assign reserve
                          <select value={selectedReserveId} onChange={(event) => setAdminRequestAssignments((current) => ({ ...current, [request.id]: event.target.value }))}>
                            <option value="">Choose a reserve</option>
                            {adminReserves.map((reserve) => <option key={reserve.id} value={reserve.id}>{reserve.displayName || reserve.name} ({reserve.active ? reserve.managerName || 'Active' : 'Inactive'})</option>)}
                          </select>
                        </label>
                        <div className="event-actions">
                          <button type="button" className="save-button" onClick={() => approveReserveRequest(request)}>Approve and assign</button>
                          <button type="button" className="secondary-button" onClick={() => {
                            if (selectedReserveId) {
                              openAdminReserve(Number(selectedReserveId))
                            }
                          }} disabled={!selectedReserveId}>Open reserve</button>
                          <button type="button" className="danger-button" onClick={() => updateRequestStatus(request.id, 'REJECTED')}>Reject</button>
                        </div>
                      </div>
                    ) : null}
                  </article>
                )
              })}</div>
            </section>
          </article>
        </section>
      ) : (
        <section className="content-grid admin-detail-layout">
          <article className="panel stack">
            <section>
              <div className="panel-heading">
                <div><p className="eyebrow">Reserve details</p><h2>{adminReserveDetail ? adminReserveDetail.displayName || adminReserveDetail.name : 'Select a reserve'}</h2><p className="muted">This tab opens automatically when you click a reserve from the catalog.</p></div>
                <div className="view-toggle">
                  <button type="button" className={adminReserveDetailView === 'map' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setAdminReserveDetailView('map')}>Map view</button>
                  <button type="button" className={adminReserveDetailView === 'text' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setAdminReserveDetailView('text')}>Text view</button>
                </div>
              </div>
              {adminReserveDetail ? (
                <>
                  <div className="event-meta">
                    <span>Status: {adminReserveDetail.active ? 'Active' : 'Inactive'}</span>
                    <span>Manager: {adminReserveDetail.managerName || 'Unassigned'}</span>
                    <span>Region: {adminReserveDetail.region}</span>
                    <span>Created at: {formatDate(adminReserveDetail.createdAt)}</span>
                  </div>
                  {adminReserveDetailView === 'map' ? (
                    <div className="map-wrap map-wrap-full">
                      <MapContainer key={`admin-detail-map-${adminReserveDetail.id}`} center={reserveCenter(adminReserveDetail)} zoom={10} scrollWheelZoom className="reserve-map reserve-map-focus">
                        <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                        <Rectangle bounds={reserveBounds(adminReserveDetail)} pathOptions={{ color: adminReserveDetail.active ? '#1b7a46' : '#8e9a92', weight: 3, dashArray: '10 8', fillOpacity: 0.08 }} />
                        <Marker position={reserveCenter(adminReserveDetail)} icon={adminPinIcon(adminReserveDetail.active, true)}>
                          <Popup><strong>{adminReserveDetail.displayName || adminReserveDetail.name}</strong><br />{adminReserveDetail.region}</Popup>
                        </Marker>
                      </MapContainer>
                    </div>
                  ) : (
                    <div className="admin-detail-copy">
                      <div className="event-meta">
                        <span>Latitudes: {adminReserveDetail.area.minLatitude} to {adminReserveDetail.area.maxLatitude}</span>
                        <span>Longitudes: {adminReserveDetail.area.minLongitude} to {adminReserveDetail.area.maxLongitude}</span>
                      </div>
                      <p className="muted">Center point: {adminReserveDetail.centerLatitude ?? 'N/A'}, {adminReserveDetail.centerLongitude ?? 'N/A'}</p>
                      <p className="empty-state">This reserve uses stored bounding coordinates for its map footprint.</p>
                    </div>
                  )}
                </>
              ) : <p className="empty-state">Go back to the catalog and click a reserve to inspect it here.</p>}
            </section>
          </article>
          <article className="panel stack">
            <section>
              <div className="panel-heading"><div><p className="eyebrow">Reserve events</p><h2>{adminReserveDetail ? `${adminReserveDetail.displayName || adminReserveDetail.name} event log` : 'Reserve event log'}</h2></div></div>
              {!adminReserveDetail ? (
                <p className="empty-state">Select a reserve to inspect its event history.</p>
              ) : !adminReserveDetail.active ? (
                <p className="empty-state">This reserve is inactive, so its event log stays hidden until it is assigned to a manager.</p>
              ) : adminReserveDetail.events.length === 0 ? (
                <p className="empty-state">This active reserve does not have any events yet.</p>
              ) : (
                <div className="event-list">{adminReserveDetail.events.map((event) => { const badge = priorityStyle(event.priority); return <article key={event.id} className="event-card"><div className="event-card-header"><div><strong>{event.type}</strong><span>{event.reserveName}</span></div><div className="event-chip-group"><span className={`priority-chip ${badge.badge}`}>{event.priority}</span><span className={`status-chip status-${event.status.toLowerCase()}`}>{event.status}</span></div></div><p>{event.description || 'No description provided.'}</p></article> })}</div>
              )}
            </section>
          </article>
        </section>
      )}
    </main>
  )
}
