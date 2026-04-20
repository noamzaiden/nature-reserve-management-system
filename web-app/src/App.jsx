import { useEffect, useMemo, useState } from 'react'
import axios from 'axios'
import { divIcon } from 'leaflet'
import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import ManagerWorkspace from './manager/ManagerWorkspace'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const TOKEN_KEY = 'reserve-web-token'

const authConfig = (token) => ({ headers: { Authorization: `Bearer ${token}` } })
const reserveCenter = (reserve) => [reserve.centerLatitude ?? ((reserve.area.minLatitude + reserve.area.maxLatitude) / 2), reserve.centerLongitude ?? ((reserve.area.minLongitude + reserve.area.maxLongitude) / 2)]
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
const emptyAssignmentForm = { managerUserId: '', reserveRequestId: '' }

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) || '')
  const [profile, setProfile] = useState(null)
  const [authMode, setAuthMode] = useState('login')
  const [loginForm, setLoginForm] = useState(emptyLoginForm)
  const [signupForm, setSignupForm] = useState(emptySignupForm)
  const [assignmentForm, setAssignmentForm] = useState(emptyAssignmentForm)
  const [adminFilters, setAdminFilters] = useState({ search: '', managerUserId: '', region: '', hasOpenEvents: '', active: '' })
  const [adminViewMode, setAdminViewMode] = useState('table')
  const [adminUsers, setAdminUsers] = useState([])
  const [adminReserves, setAdminReserves] = useState([])
  const [adminReserveRequests, setAdminReserveRequests] = useState([])
  const [adminReserveDetail, setAdminReserveDetail] = useState(null)
  const [adminSelectedReserveId, setAdminSelectedReserveId] = useState(null)
  const [reserves, setReserves] = useState([])
  const [events, setEvents] = useState([])
  const [reservePoisByReserveId, setReservePoisByReserveId] = useState({})
  const [reservePoiTypesByReserveId, setReservePoiTypesByReserveId] = useState({})
  const [reserveRequests, setReserveRequests] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const regionOptions = useMemo(() => [...new Set(adminReserves.map((reserve) => reserve.region).filter(Boolean))].sort(), [adminReserves])

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
    const [profileResponse, reservesResponse, eventsResponse, requestsResponse] = await Promise.all([
      axios.get(`${API_BASE}/api/auth/me`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/reserves`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/events`, authConfig(activeToken)),
      axios.get(`${API_BASE}/api/reserve-requests/mine`, authConfig(activeToken))
    ])
    setProfile(profileResponse.data)
    setReserves(reservesResponse.data)
    setEvents(eventsResponse.data)
    setReserveRequests(requestsResponse.data)
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

  useEffect(() => {
    if (!token) {
      setProfile(null)
      setReserves([])
      setEvents([])
      setReservePoisByReserveId({})
      setReservePoiTypesByReserveId({})
      setReserveRequests([])
      setAdminUsers([])
      setAdminReserves([])
      setAdminReserveRequests([])
      setAdminReserveDetail(null)
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
      axios.get(`${API_BASE}/api/admin/reserves/${adminSelectedReserveId}`, authConfig(token))
        .then((response) => {
          setAdminReserveDetail(response.data)
          setAssignmentForm((current) => ({
            managerUserId: response.data.managerUserId ? String(response.data.managerUserId) : current.managerUserId,
            reserveRequestId: current.reserveRequestId
          }))
        })
        .catch((requestError) => setError(requestError.response?.data?.message || 'Failed to load reserve details.'))
    }
  }, [token, profile?.role, adminSelectedReserveId])

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

  async function handleAssignmentSubmit(event) {
    event.preventDefault()
    if (!adminReserveDetail) return
    setError('')
    try {
      await axios.patch(`${API_BASE}/api/admin/reserves/${adminReserveDetail.id}/assignment`, {
        managerUserId: assignmentForm.managerUserId ? Number(assignmentForm.managerUserId) : null,
        reserveRequestId: assignmentForm.reserveRequestId ? Number(assignmentForm.reserveRequestId) : null
      }, authConfig(token))
      await loadAdminDashboard(token)
      setNotice(assignmentForm.managerUserId ? 'Reserve assignment updated.' : 'Reserve marked inactive and unassigned.')
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Failed to update reserve assignment.')
    }
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
        profile={profile}
        loading={loading}
        error={error}
        notice={notice}
        reserves={reserves}
        events={events}
        reservePoisByReserveId={reservePoisByReserveId}
        reservePoiTypesByReserveId={reservePoiTypesByReserveId}
        reserveRequests={reserveRequests}
        onClearError={() => setError('')}
        onClearNotice={() => setNotice('')}
        onLogout={handleLogout}
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

  return (
    <main className="shell">
      <header className="topbar">
        <div><p className="eyebrow">Administrator Console</p><h1>Reserve Catalog</h1><p className="muted">Signed in as <strong>{profile.name}</strong> ({profile.email})</p></div>
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
      <section className="content-grid">
        <article className="panel stack">
          <section>
            <div className="panel-heading">
              <div><p className="eyebrow">Inventory</p><h2>All seeded reserves</h2></div>
              <div className="view-toggle">
                <button type="button" className={adminViewMode === 'table' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setAdminViewMode('table')}>Table view</button>
                <button type="button" className={adminViewMode === 'map' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setAdminViewMode('map')}>Map view</button>
              </div>
            </div>
            <form className="event-form" onSubmit={(event) => event.preventDefault()}>
              <label className="event-form-wide">Search<input type="text" value={adminFilters.search} onChange={(event) => setAdminFilters((current) => ({ ...current, search: event.target.value }))} placeholder="Reserve, manager, or email" /></label>
              <label>Manager<select value={adminFilters.managerUserId} onChange={(event) => setAdminFilters((current) => ({ ...current, managerUserId: event.target.value }))}><option value="">All managers</option>{adminUsers.map((user) => <option key={user.id} value={user.id}>{user.name}</option>)}</select></label>
              <label>Region<select value={adminFilters.region} onChange={(event) => setAdminFilters((current) => ({ ...current, region: event.target.value }))}><option value="">All regions</option>{regionOptions.map((region) => <option key={region} value={region}>{region}</option>)}</select></label>
              <label>Activity<select value={adminFilters.active} onChange={(event) => setAdminFilters((current) => ({ ...current, active: event.target.value }))}><option value="">Active and inactive</option><option value="true">Active only</option><option value="false">Inactive only</option></select></label>
              <label className="event-form-wide">Open events<select value={adminFilters.hasOpenEvents} onChange={(event) => setAdminFilters((current) => ({ ...current, hasOpenEvents: event.target.value }))}><option value="">All reserves</option><option value="true">Only reserves with open events</option><option value="false">Only reserves with no open events</option></select></label>
            </form>
            {adminViewMode === 'table' ? (
              <div className="table-shell">
                <table className="reserve-table">
                  <thead><tr><th>Name</th><th>Region</th><th>Status</th><th>Manager</th><th>Open events</th></tr></thead>
                  <tbody>{adminReserves.map((reserve) => <tr key={reserve.id} className={reserve.id === adminSelectedReserveId ? 'reserve-row reserve-row-active' : 'reserve-row'} onClick={() => setAdminSelectedReserveId(reserve.id)}><td>{reserve.name}</td><td>{reserve.region}</td><td><span className={reserve.active ? 'status-chip status-active' : 'status-chip status-inactive'}>{reserve.active ? 'Active' : 'Inactive'}</span></td><td>{reserve.managerName || 'Unassigned'}</td><td>{reserve.openEvents}</td></tr>)}</tbody>
                </table>
              </div>
            ) : (
              <div className="map-wrap">
                <div className="map-legend"><span><i className="legend-dot legend-dot-active" /> Active reserve</span><span><i className="legend-dot legend-dot-inactive" /> Inactive reserve</span></div>
                <MapContainer key={`admin-map-${adminSelectedReserveId ?? 'all'}`} center={adminReserveDetail ? reserveCenter(adminReserveDetail) : [31.5, 35.0]} zoom={8} scrollWheelZoom className="reserve-map">
                  <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                  {adminReserves.map((reserve) => <Marker key={reserve.id} position={reserveCenter(reserve)} icon={adminPinIcon(reserve.active, reserve.id === adminSelectedReserveId)} eventHandlers={{ click: () => setAdminSelectedReserveId(reserve.id) }}><Popup><strong>{reserve.name}</strong><br />{reserve.active ? `Assigned to ${reserve.managerName}` : 'Inactive and unassigned'}</Popup></Marker>)}
                </MapContainer>
              </div>
            )}
          </section>
          <section>
            <div className="panel-heading"><div><p className="eyebrow">Manager requests</p><h2>Incoming assignment requests</h2></div></div>
            <div className="event-list">{adminReserveRequests.map((request) => <article key={request.id} className="event-card"><div className="event-card-header"><div><strong>{request.reserveName}</strong><span>{request.requestedByName} ({request.requestedByEmail})</span></div><span className={`status-chip status-${request.status.toLowerCase()}`}>{request.status}</span></div><p>{request.message}</p><div className="event-meta"><span>Requested at: {formatDate(request.createdAt)}</span></div>{request.status === 'OPEN' ? <div className="event-actions"><button type="button" className="secondary-button" onClick={() => setAssignmentForm((current) => ({ ...current, managerUserId: String(request.requestedByUserId), reserveRequestId: String(request.id) }))}>Use for assignment</button><button type="button" className="secondary-button" onClick={() => updateRequestStatus(request.id, 'REJECTED')}>Reject request</button></div> : null}</article>)}</div>
          </section>
        </article>
        <article className="panel stack">
          <section>
            <div className="panel-heading"><div><p className="eyebrow">Reserve details</p><h2>{adminReserveDetail ? adminReserveDetail.name : 'Select a reserve'}</h2></div></div>
            {adminReserveDetail ? <>
              <div className="event-meta"><span>Status: {adminReserveDetail.active ? 'Active' : 'Inactive'}</span><span>Manager: {adminReserveDetail.managerName || 'Unassigned'}</span><span>Region: {adminReserveDetail.region}</span></div>
              <div className="event-meta"><span>Latitudes: {adminReserveDetail.area.minLatitude} to {adminReserveDetail.area.maxLatitude}</span><span>Longitudes: {adminReserveDetail.area.minLongitude} to {adminReserveDetail.area.maxLongitude}</span></div>
              <form className="event-form assignment-form" onSubmit={handleAssignmentSubmit}>
                <label>Assigned manager<select value={assignmentForm.managerUserId} onChange={(event) => setAssignmentForm((current) => ({ ...current, managerUserId: event.target.value }))}><option value="">No manager / inactive reserve</option>{adminUsers.map((user) => <option key={user.id} value={user.id}>{user.name} ({user.email})</option>)}</select></label>
                <label>Resolve request<select value={assignmentForm.reserveRequestId} onChange={(event) => setAssignmentForm((current) => ({ ...current, reserveRequestId: event.target.value }))}><option value="">No linked request</option>{adminReserveRequests.filter((request) => request.status === 'OPEN').map((request) => <option key={request.id} value={request.id}>{request.reserveName} - {request.requestedByName}</option>)}</select></label>
                <button type="submit">{assignmentForm.managerUserId ? 'Save assignment' : 'Mark inactive'}</button>
              </form>
            </> : <p className="empty-state">Select a reserve to manage its assignment.</p>}
          </section>
          <section>
            <div className="panel-heading"><div><p className="eyebrow">Reserve events</p><h2>{adminReserveDetail ? `${adminReserveDetail.name} event log` : 'Select a reserve'}</h2></div></div>
            {adminReserveDetail ? <div className="event-list">{adminReserveDetail.events.map((event) => { const badge = priorityStyle(event.priority); return <article key={event.id} className="event-card"><div className="event-card-header"><div><strong>{event.type}</strong><span>{event.reserveName}</span></div><div className="event-chip-group"><span className={`priority-chip ${badge.badge}`}>{event.priority}</span><span className={`status-chip status-${event.status.toLowerCase()}`}>{event.status}</span></div></div><p>{event.description || 'No description provided.'}</p></article> })}</div> : <p className="empty-state">Select a reserve to inspect its events.</p>}
          </section>
        </article>
      </section>
    </main>
  )
}
