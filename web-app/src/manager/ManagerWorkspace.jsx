import { useEffect, useMemo, useState } from 'react'
import { divIcon } from 'leaflet'
import { CircleMarker, MapContainer, Marker, Popup, Rectangle, TileLayer, useMap, useMapEvents } from 'react-leaflet'

const emptyRequestForm = { reserveName: '', message: '' }
const emptyEventForm = { type: 'OTHER', priority: 'MEDIUM', description: '', latitude: '', longitude: '', publishedToTravelers: false }
const emptyPoiForm = { typeId: '', customTypeName: '', name: '', description: '', latitude: '', longitude: '' }

const reserveBounds = (reserve) => [[reserve.area.minLatitude, reserve.area.minLongitude], [reserve.area.maxLatitude, reserve.area.maxLongitude]]
const reserveCenter = (reserve) => [reserve.centerLatitude ?? ((reserve.area.minLatitude + reserve.area.maxLatitude) / 2), reserve.centerLongitude ?? ((reserve.area.minLongitude + reserve.area.maxLongitude) / 2)]
const pointInsideReserve = (reserve, latitude, longitude) => latitude >= reserve.area.minLatitude
  && latitude <= reserve.area.maxLatitude
  && longitude >= reserve.area.minLongitude
  && longitude <= reserve.area.maxLongitude
const formatDate = (value) => value ? new Date(value).toLocaleString() : 'Not available'
const formatRelativeTime = (value) => {
  if (!value) return 'Just now'
  const diff = Math.round((Date.now() - new Date(value).getTime()) / 60000)
  if (diff < 1) return 'Just now'
  if (diff < 60) return `${diff}m ago`
  if (diff < 1440) return `${Math.round(diff / 60)}h ago`
  return `${Math.round(diff / 1440)}d ago`
}
const priorityStyle = (priority) => priority === 'HIGH'
  ? { accent: '#c0392b', fill: '#f7b3a8', badge: 'priority-high', markerRadius: 9 }
  : priority === 'MEDIUM'
    ? { accent: '#d68910', fill: '#fde3a7', badge: 'priority-medium', markerRadius: 8 }
    : { accent: '#2471a3', fill: '#b9ddff', badge: 'priority-low', markerRadius: 7 }
const poiPinIcon = (typeName) => divIcon({
  className: 'poi-pin-wrapper',
  html: `<span class="poi-map-pin"><span class="poi-map-pin-label">${(typeName || 'P').slice(0, 1).toUpperCase()}</span></span>`,
  iconSize: [30, 30],
  iconAnchor: [15, 26],
  popupAnchor: [0, -20]
})

function FitToReserve({ reserve }) {
  const map = useMap()
  useEffect(() => {
    if (reserve) {
      const bounds = reserveBounds(reserve)
      map.setMaxBounds(bounds)
      map.fitBounds(bounds, { padding: [18, 18] })
      map.setMinZoom(map.getBoundsZoom(bounds))
    }
  }, [map, reserve])
  return null
}

function ReserveMapInteraction({ reserve, onMapClick }) {
  useMapEvents({
    click(event) {
      if (!reserve) {
        return
      }

      const { lat, lng } = event.latlng
      onMapClick(lat, lng)
    }
  })

  return null
}

function PriorityBreakdown({ counts }) {
  return (
    <div className="priority-breakdown">
      {['HIGH', 'MEDIUM', 'LOW'].map((priority) => counts[priority] ? <span key={priority} className={`priority-chip ${priorityStyle(priority).badge}`}>{counts[priority]} {priority.toLowerCase()}</span> : null)}
    </div>
  )
}

function ManagerNav({ page, onNavigate, selectedReserve }) {
  const items = [
    ['overview', 'Overview'],
    ['reserves', 'Active reserves'],
    ['control', selectedReserve ? selectedReserve.displayName : 'Control center'],
    ['requests', 'Requests']
  ]

  return (
    <nav className="manager-nav">
      {items.map(([id, label]) => (
        <button key={id} type="button" className={page === id ? 'manager-nav-button manager-nav-button-active' : 'manager-nav-button'} onClick={() => onNavigate(id)} disabled={id === 'control' && !selectedReserve}>
          <span>{label}</span>
          {id === 'control' && !selectedReserve ? <small>Select a reserve first</small> : null}
        </button>
      ))}
    </nav>
  )
}

function NotificationPanel({ notifications, unreadCount, seenNotificationIds, onOpenItem, onMarkAllRead, onClose }) {
  return (
    <div className="notification-panel">
      <div className="notification-panel-header">
        <div><p className="eyebrow">Alerts</p><h3>Manager notifications</h3></div>
        <button type="button" className="secondary-button" onClick={onMarkAllRead}>Mark all read</button>
      </div>
      <p className="muted notification-subtitle">{unreadCount} unread items require attention.</p>
      <div className="notification-list">
        {notifications.length === 0 ? <p className="empty-state">No alerts yet.</p> : notifications.map((item) => (
          <button key={item.id} type="button" className={seenNotificationIds.includes(item.id) ? 'notification-item' : 'notification-item notification-item-unread'} onClick={() => onOpenItem(item)}>
            <div className="notification-item-header"><strong>{item.title}</strong><span>{formatRelativeTime(item.createdAt)}</span></div>
            <p>{item.body}</p>
            <div className="notification-item-meta"><span>{item.reserveName || item.type}</span>{item.priority ? <span className={`priority-chip ${priorityStyle(item.priority).badge}`}>{item.priority}</span> : null}</div>
          </button>
        ))}
      </div>
      <button type="button" className="secondary-button notification-close" onClick={onClose}>Close alerts</button>
    </div>
  )
}

export default function ManagerWorkspace({
  apiBase,
  profile,
  loading,
  error,
  notice,
  reserves,
  events,
  reservePoisByReserveId,
  reservePoiTypesByReserveId,
  reserveRequests,
  onClearError,
  onClearNotice,
  onLogout,
  onCreateReserveRequest,
  onCreateEvent,
  onUpdateEventStatus,
  onUpdateEventPriority,
  onUpdateEventPublish,
  onCreateReservePoi,
  onUpdateReservePoi,
  onDeleteReservePoi,
  onCreateReservePoiType,
  onUpdateReservePoiType,
  onDeleteReservePoiType
}) {
  const [page, setPage] = useState('overview')
  const [reserveViewMode, setReserveViewMode] = useState('cards')
  const [selectedReserveId, setSelectedReserveId] = useState(null)
  const [controlFilters, setControlFilters] = useState({ status: 'ACTIVE', priority: '' })
  const [requestFilters, setRequestFilters] = useState({ status: 'ALL', search: '' })
  const [requestForm, setRequestForm] = useState(emptyRequestForm)
  const [eventForm, setEventForm] = useState(emptyEventForm)
  const [poiForm, setPoiForm] = useState(emptyPoiForm)
  const [controlTab, setControlTab] = useState('map')
  const [editingPoiId, setEditingPoiId] = useState(null)
  const [eventFormMessage, setEventFormMessage] = useState('')
  const [mapDisplay, setMapDisplay] = useState({ showEvents: true, showPois: true })
  const [mapCreateMode, setMapCreateMode] = useState('')
  const [mapLocked, setMapLocked] = useState(false)
  const [mapPopup, setMapPopup] = useState(null)
  const [mapEventDraft, setMapEventDraft] = useState(emptyEventForm)
  const [mapPoiDraft, setMapPoiDraft] = useState(emptyPoiForm)
  const [mapPopupMessage, setMapPopupMessage] = useState('')
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [seenNotificationIds, setSeenNotificationIds] = useState([])

  const reserveSummaries = useMemo(() => reserves.map((reserve) => {
    const reserveEvents = events.filter((event) => event.reserveId === reserve.id).sort((left, right) => new Date(right.updatedAt || right.createdAt) - new Date(left.updatedAt || left.createdAt))
    const activeEvents = reserveEvents.filter((event) => event.status !== 'CLOSED')
    return {
      ...reserve,
      displayName: reserve.displayName || reserve.name,
      reserveEvents,
      totalActiveEvents: activeEvents.length,
      priorityCounts: activeEvents.reduce((counts, event) => ({ ...counts, [event.priority]: counts[event.priority] + 1 }), { HIGH: 0, MEDIUM: 0, LOW: 0 }),
      statusCounts: reserveEvents.reduce((counts, event) => ({ ...counts, [event.status]: (counts[event.status] || 0) + 1 }), { OPEN: 0, IN_PROGRESS: 0, CLOSED: 0 }),
      travelerReports: activeEvents.filter((event) => event.origin === 'TRAVELER').length,
      publishedEvents: activeEvents.filter((event) => event.publishedToTravelers).length
    }
  }), [reserves, events])

  const selectedReserve = useMemo(() => reserveSummaries.find((reserve) => reserve.id === selectedReserveId) || reserveSummaries[0] || null, [reserveSummaries, selectedReserveId])
  const selectedReservePois = useMemo(() => selectedReserve ? reservePoisByReserveId[selectedReserve.id] || [] : [], [selectedReserve, reservePoisByReserveId])
  const selectedReservePoiTypes = useMemo(() => selectedReserve ? reservePoiTypesByReserveId[selectedReserve.id] || [] : [], [selectedReserve, reservePoiTypesByReserveId])
  const poiTypeOptions = useMemo(() => [...selectedReservePoiTypes, { id: '__custom__', name: 'Create new type...' }], [selectedReservePoiTypes])

  const notifications = useMemo(() => {
    const requestItems = reserveRequests.map((request) => ({
      id: `request-${request.id}`,
      type: 'Request',
      title: request.status === 'OPEN' ? 'Reserve request awaiting review' : `Reserve request ${request.status.toLowerCase()}`,
      body: `${request.reserveName}: ${request.message}`,
      reserveId: null,
      reserveName: request.reserveName,
      priority: request.status === 'REJECTED' ? 'HIGH' : request.status === 'APPROVED' ? 'LOW' : 'MEDIUM',
      createdAt: request.resolvedAt || request.createdAt
    }))
    const eventItems = events.filter((event) => event.status !== 'CLOSED' || event.priority === 'HIGH' || event.origin === 'TRAVELER').map((event) => ({
      id: `event-${event.id}`,
      type: 'Event',
      title: event.origin === 'TRAVELER' ? 'Traveler report needs review' : `${event.priority} priority event`,
      body: `${event.reserveName}: ${event.description || event.type}`,
      reserveId: event.reserveId,
      reserveName: event.reserveName,
      priority: event.priority,
      createdAt: event.updatedAt || event.createdAt
    }))
    return [...requestItems, ...eventItems].sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt))
  }, [events, reserveRequests])

  const filteredControlEvents = useMemo(() => {
    if (!selectedReserve) return []
    return selectedReserve.reserveEvents.filter((event) => {
      const matchesStatus = controlFilters.status === 'ACTIVE' ? event.status !== 'CLOSED' : !controlFilters.status || event.status === controlFilters.status
      const matchesPriority = !controlFilters.priority || event.priority === controlFilters.priority
      return matchesStatus && matchesPriority
    })
  }, [selectedReserve, controlFilters])

  const filteredRequests = useMemo(() => reserveRequests.filter((request) => {
    const matchesStatus = requestFilters.status === 'ALL' || request.status === requestFilters.status
    const query = requestFilters.search.trim().toLowerCase()
    const matchesSearch = !query || request.reserveName.toLowerCase().includes(query) || request.message.toLowerCase().includes(query) || request.status.toLowerCase().includes(query)
    return matchesStatus && matchesSearch
  }), [reserveRequests, requestFilters])

  useEffect(() => {
    setSelectedReserveId((current) => reserveSummaries.some((reserve) => reserve.id === current) ? current : reserveSummaries[0]?.id ?? null)
  }, [reserveSummaries])

  useEffect(() => {
    if (!profile?.email) return
    const stored = localStorage.getItem(`manager-seen-alerts-${profile.email}`)
    setSeenNotificationIds(stored ? JSON.parse(stored) : [])
  }, [profile?.email])

  useEffect(() => {
    if (!profile?.email) return
    localStorage.setItem(`manager-seen-alerts-${profile.email}`, JSON.stringify(seenNotificationIds))
  }, [profile?.email, seenNotificationIds])

  useEffect(() => {
    if (!selectedReserve) return
    setEventFormMessage('')
    setMapCreateMode('')
    setMapPopup(null)
    setMapPopupMessage('')
    setEventForm((current) => ({ ...current, latitude: selectedReserve.centerLatitude?.toFixed(4) ?? '', longitude: selectedReserve.centerLongitude?.toFixed(4) ?? '' }))
  }, [selectedReserve?.id])

  useEffect(() => {
    if (!selectedReserve || editingPoiId) return
    const fallbackTypeId = selectedReservePoiTypes[0] ? String(selectedReservePoiTypes[0].id) : ''
    setPoiForm((current) => ({
      ...emptyPoiForm,
      typeId: selectedReservePoiTypes.some((type) => String(type.id) === current.typeId) ? current.typeId : fallbackTypeId,
      latitude: selectedReserve.centerLatitude?.toFixed(4) ?? '',
      longitude: selectedReserve.centerLongitude?.toFixed(4) ?? ''
    }))
  }, [selectedReserve?.id, selectedReservePoiTypes, editingPoiId])

  useEffect(() => {
    if (!selectedReserve) {
      return
    }

    const defaultTypeId = selectedReservePoiTypes[0] ? String(selectedReservePoiTypes[0].id) : ''
    setMapEventDraft({
      ...emptyEventForm,
      latitude: selectedReserve.centerLatitude?.toFixed(4) ?? '',
      longitude: selectedReserve.centerLongitude?.toFixed(4) ?? ''
    })
    setMapPoiDraft({
      ...emptyPoiForm,
      typeId: defaultTypeId,
      latitude: selectedReserve.centerLatitude?.toFixed(4) ?? '',
      longitude: selectedReserve.centerLongitude?.toFixed(4) ?? ''
    })
  }, [selectedReserve?.id, selectedReservePoiTypes])

  useEffect(() => {
    setEditingPoiId(null)
    setPoiForm(emptyPoiForm)
  }, [selectedReserve?.id])

  function openReserveControlCenter(reserveId) {
    setSelectedReserveId(reserveId)
    setPage('control')
    setControlTab('map')
    setNotificationsOpen(false)
  }

  async function submitReserveRequest(event) {
    event.preventDefault()
    await onCreateReserveRequest(requestForm)
    setRequestForm(emptyRequestForm)
  }

  async function submitEvent(event) {
    event.preventDefault()
    if (!selectedReserve) return
    const latitude = Number(eventForm.latitude)
    const longitude = Number(eventForm.longitude)

    if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
      setEventFormMessage('Enter valid latitude and longitude values before creating the event.')
      return
    }

    if (!pointInsideReserve(selectedReserve, latitude, longitude)) {
      setEventFormMessage('Event coordinates must stay inside the selected reserve boundary.')
      return
    }

    setEventFormMessage('')
    await onCreateEvent(selectedReserve.id, eventForm)
    setEventForm((current) => ({ ...emptyEventForm, latitude: current.latitude, longitude: current.longitude }))
  }

  function resetPoiForm() {
    setEditingPoiId(null)
    setPoiForm({
      ...emptyPoiForm,
      typeId: selectedReservePoiTypes[0] ? String(selectedReservePoiTypes[0].id) : '',
      customTypeName: '',
      latitude: selectedReserve?.centerLatitude?.toFixed(4) ?? '',
      longitude: selectedReserve?.centerLongitude?.toFixed(4) ?? ''
    })
  }

  async function submitPoi(event) {
    event.preventDefault()
    if (!selectedReserve) return
    let typeId = poiForm.typeId

    if (typeId === '__custom__') {
      const createdType = await onCreateReservePoiType(selectedReserve.id, { name: poiForm.customTypeName })
      typeId = String(createdType.id)
    }

    const poiPayload = {
      ...poiForm,
      typeId
    }

    if (editingPoiId) {
      await onUpdateReservePoi(selectedReserve.id, editingPoiId, poiPayload)
    } else {
      await onCreateReservePoi(selectedReserve.id, poiPayload)
    }
    resetPoiForm()
  }

  function startPoiEdit(poi) {
    setPoiMapPickMode(false)
    setEditingPoiId(poi.id)
    setPoiForm({
      typeId: String(poi.typeId),
      customTypeName: '',
      name: poi.name,
      description: poi.description || '',
      latitude: String(poi.latitude ?? ''),
      longitude: String(poi.longitude ?? '')
    })
  }

  function closeMapPopup() {
    setMapCreateMode('')
    setMapPopup(null)
    setMapPopupMessage('')
  }

  function handleReserveMapClick(latitude, longitude) {
    const popupState = { latitude, longitude }
    setMapPopupMessage('')

    if (mapCreateMode === 'event') {
      setMapEventDraft((current) => ({
        ...current,
        latitude: latitude.toFixed(4),
        longitude: longitude.toFixed(4)
      }))
      setMapPopup({ ...popupState, type: 'event' })
      return
    }

    if (mapCreateMode === 'poi') {
      setMapPoiDraft((current) => ({
        ...current,
        latitude: latitude.toFixed(4),
        longitude: longitude.toFixed(4)
      }))
      setMapPopup({ ...popupState, type: 'poi' })
      return
    }

    setMapPopup({ ...popupState, type: 'coordinates' })
  }

  async function submitMapEvent(event) {
    event.preventDefault()
    if (!selectedReserve) return

    const latitude = Number(mapEventDraft.latitude)
    const longitude = Number(mapEventDraft.longitude)

    if (Number.isNaN(latitude) || Number.isNaN(longitude) || !pointInsideReserve(selectedReserve, latitude, longitude)) {
      setMapPopupMessage('Choose a point inside the reserve before creating the event.')
      return
    }

    await onCreateEvent(selectedReserve.id, mapEventDraft)
    setMapEventDraft((current) => ({
      ...emptyEventForm,
      latitude: current.latitude,
      longitude: current.longitude
    }))
    closeMapPopup()
  }

  async function submitMapPoi(event) {
    event.preventDefault()
    if (!selectedReserve) return

    let typeId = mapPoiDraft.typeId

    if (typeId === '__custom__') {
      const createdType = await onCreateReservePoiType(selectedReserve.id, { name: mapPoiDraft.customTypeName })
      typeId = String(createdType.id)
    }

    await onCreateReservePoi(selectedReserve.id, {
      ...mapPoiDraft,
      typeId
    })

    closeMapPopup()
  }

  function openNotification(item) {
    setSeenNotificationIds((current) => current.includes(item.id) ? current : [...current, item.id])
    if (item.reserveId) openReserveControlCenter(item.reserveId)
    else setPage('requests')
  }

  const unreadCount = notifications.filter((item) => !seenNotificationIds.includes(item.id)).length
  const openRequests = filteredRequests.filter((request) => request.status === 'OPEN')
  const historyRequests = filteredRequests.filter((request) => request.status !== 'OPEN')

  function resolveMediaUrl(mediaUrl) {
    if (!mediaUrl) {
      return ''
    }
    if (/^https?:\/\//i.test(mediaUrl)) {
      return mediaUrl
    }
    return `${apiBase}${mediaUrl.startsWith('/') ? mediaUrl : `/${mediaUrl}`}`
  }

  function imageAttachmentsForEvent(event) {
    if (!Array.isArray(event.media)) {
      return []
    }
    return event.media.filter((mediaItem) => mediaItem?.mediaType === 'IMAGE' && mediaItem.mediaUrl)
  }

  function controlEventCardClass(event) {
    if (event.status === 'CLOSED') {
      return 'event-card control-event-card control-event-card-closed'
    }

    if (event.priority === 'HIGH') {
      return 'event-card control-event-card control-event-card-high'
    }

    if (event.priority === 'MEDIUM') {
      return 'event-card control-event-card control-event-card-medium'
    }

    return 'event-card control-event-card control-event-card-low'
  }

  return (
    <main className="shell">
      <header className="topbar topbar-manager">
        <div><p className="eyebrow">Manager Console</p><h1>Reserve operations center</h1><p className="muted">Signed in as <strong>{profile?.name}</strong> ({profile?.email})</p></div>
        <div className="topbar-actions">
          <div className="summary-pill"><span>{reserveSummaries.length}</span><small>Managed reserves</small></div>
          <div className="summary-pill"><span>{events.filter((event) => event.status !== 'CLOSED').length}</span><small>Active events</small></div>
          <button type="button" className="notification-bell" onClick={() => setNotificationsOpen((current) => !current)}><span>Alerts</span>{unreadCount ? <strong>{unreadCount}</strong> : null}</button>
          <button className="danger-button" type="button" onClick={onLogout}>Sign out</button>
        </div>
        {notificationsOpen ? <NotificationPanel notifications={notifications} unreadCount={unreadCount} seenNotificationIds={seenNotificationIds} onOpenItem={openNotification} onMarkAllRead={() => setSeenNotificationIds(notifications.map((item) => item.id))} onClose={() => setNotificationsOpen(false)} /> : null}
      </header>

      <ManagerNav page={page} onNavigate={setPage} selectedReserve={selectedReserve} />
      {error ? <p className="error-banner">{error}</p> : null}
      {notice ? <p className="status-note">{notice}</p> : null}
      {loading ? <p className="status-note">Loading your manager workspace...</p> : null}

      {page === 'overview' ? (
        <section className="manager-page stack">
          <article className="hero-panel">
            <div><p className="eyebrow">Manager dashboard</p><h2>Welcome back, {profile?.name}</h2><p className="muted">Start here for a broad view across every reserve you manage before opening a focused control center.</p></div>
            <div className="hero-stats"><div className="hero-stat"><span>{reserveSummaries.length}</span><small>Active reserves</small></div><div className="hero-stat"><span>{events.filter((event) => event.priority === 'HIGH' && event.status !== 'CLOSED').length}</span><small>High-priority events</small></div><div className="hero-stat"><span>{events.filter((event) => event.origin === 'TRAVELER' && event.status !== 'CLOSED').length}</span><small>Traveler reports</small></div></div>
          </article>
          <div className="metric-grid">
            <article className="panel metric-card"><p className="eyebrow">Operations</p><strong>{events.filter((event) => event.status !== 'CLOSED').length}</strong><span>Active events across all reserves</span></article>
            <article className="panel metric-card"><p className="eyebrow">Mobile app</p><strong>{events.filter((event) => event.publishedToTravelers && event.status !== 'CLOSED').length}</strong><span>Events visible to travelers</span></article>
            <article className="panel metric-card"><p className="eyebrow">Requests</p><strong>{reserveRequests.filter((request) => request.status === 'OPEN').length}</strong><span>Open reserve requests</span></article>
            <article className="panel metric-card"><p className="eyebrow">Alerts</p><strong>{notifications.length}</strong><span>Recent updates needing attention</span></article>
          </div>
          <div className="content-grid content-grid-wide">
            <article className="panel stack">
              <div className="panel-heading"><div><p className="eyebrow">Reserve focus</p><h2>Your busiest reserves</h2></div><button type="button" className="info-button" onClick={() => setPage('requests')}>Open requests page</button></div>
              <div className="overview-reserve-grid">{reserveSummaries.length === 0 ? <p className="empty-state">No active reserves yet.</p> : reserveSummaries.slice().sort((left, right) => (right.priorityCounts.HIGH - left.priorityCounts.HIGH) || (right.totalActiveEvents - left.totalActiveEvents)).slice(0, 3).map((reserve) => <article key={reserve.id} className="overview-reserve-card"><div className="overview-reserve-header"><div><strong>{reserve.displayName}</strong><span>{reserve.region}</span></div><button type="button" className="info-button" onClick={() => openReserveControlCenter(reserve.id)}>Open control center</button></div><div className="event-meta"><span>{reserve.totalActiveEvents} active events</span><span>{reserve.travelerReports} traveler reports</span></div><PriorityBreakdown counts={reserve.priorityCounts} /></article>)}</div>
            </article>
            <article className="panel stack">
              <div className="panel-heading"><div><p className="eyebrow">Alert stream</p><h2>Latest updates</h2></div></div>
              <div className="timeline-list">{notifications.length === 0 ? <p className="empty-state">No notifications yet.</p> : notifications.slice(0, 5).map((item) => <button key={item.id} type="button" className="timeline-entry" onClick={() => openNotification(item)}><div className="timeline-entry-header"><strong>{item.title}</strong><span>{formatRelativeTime(item.createdAt)}</span></div><p>{item.body}</p><div className="timeline-entry-meta"><span>{item.reserveName || item.type}</span>{item.priority ? <span className={`priority-chip ${priorityStyle(item.priority).badge}`}>{item.priority}</span> : null}</div></button>)}</div>
            </article>
          </div>
        </section>
      ) : null}

      {page === 'reserves' ? (
        <section className="manager-page stack">
          <article className="panel">
            <div className="panel-heading"><div><p className="eyebrow">Active reserves</p><h2>Operational reserve list</h2><p className="muted">Choose a reserve to move into its focused control center.</p></div><div className="view-toggle"><button type="button" className={reserveViewMode === 'cards' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setReserveViewMode('cards')}>Cards</button><button type="button" className={reserveViewMode === 'map' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setReserveViewMode('map')}>Map</button></div></div>
            {reserveSummaries.length === 0 ? <p className="empty-state">No reserves are assigned to this account yet.</p> : reserveViewMode === 'cards' ? <div className="reserve-dashboard-grid">{reserveSummaries.map((reserve) => <article key={reserve.id} className={reserve.id === selectedReserveId ? 'reserve-dashboard-card reserve-dashboard-card-active' : 'reserve-dashboard-card'}><div className="overview-reserve-header"><div><strong>{reserve.displayName}</strong><span>{reserve.region}</span></div><button type="button" onClick={() => openReserveControlCenter(reserve.id)}>Open</button></div><div className="event-meta"><span>{reserve.totalActiveEvents} active events</span><span>{reserve.statusCounts.OPEN} open</span>{reserve.statusCounts.IN_PROGRESS ? <span>{reserve.statusCounts.IN_PROGRESS} in progress</span> : null}</div><PriorityBreakdown counts={reserve.priorityCounts} /></article>)}</div> : <div className="map-wrap map-wrap-tall"><MapContainer center={reserveSummaries[0] ? reserveCenter(reserveSummaries[0]) : [31.5, 35.0]} zoom={8} scrollWheelZoom className="reserve-map reserve-map-tall"><TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />{reserveSummaries.map((reserve) => <Rectangle key={reserve.id} bounds={reserveBounds(reserve)} pathOptions={{ color: reserve.id === selectedReserveId ? '#d14b27' : '#1d5f5b', weight: reserve.id === selectedReserveId ? 3 : 2, dashArray: '12 8', fillOpacity: 0 }} eventHandlers={{ click: () => openReserveControlCenter(reserve.id) }}><Popup><strong>{reserve.displayName}</strong><br />{reserve.region}<br />{reserve.totalActiveEvents} active events</Popup></Rectangle>)}</MapContainer></div>}
          </article>
        </section>
      ) : null}

      {page === 'control' ? (
        <section className="manager-page stack">
          {selectedReserve ? <>
            <article className="control-header-card"><div><p className="eyebrow">Reserve control center</p><h2>{selectedReserve.displayName}</h2><p className="muted">This view stays focused on one reserve with a near full-screen map, dashed reserve boundary, and a filtered event sidebar.</p></div><div className="control-summary-chips"><span className="summary-pill"><span>{selectedReserve.totalActiveEvents}</span><small>Active events</small></span><span className="summary-pill"><span>{selectedReserve.publishedEvents}</span><small>Published</small></span><span className="summary-pill"><span>{selectedReserve.travelerReports}</span><small>Traveler reports</small></span></div></article>
            <article className="panel control-mode-panel">
              <div className="panel-heading"><div><p className="eyebrow">Workspace mode</p><h2>{selectedReserve.displayName}</h2><p className="muted">Switch between a focused reserve map and a separate operations panel.</p></div><div className="view-toggle"><button type="button" className={controlTab === 'map' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setControlTab('map')}>Map</button><button type="button" className={controlTab === 'panel' ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setControlTab('panel')}>Control panel</button></div></div>
            </article>
            {controlTab === 'map' ? (
              <div className="reserve-map-workspace">
                <aside className="panel map-filter-sidebar stack">
                  <section>
                    <div className="panel-heading"><div><p className="eyebrow">Map view</p><h2>Focus controls</h2></div></div>
                    <button type="button" className={mapLocked ? 'danger-button' : 'info-button'} onClick={() => setMapLocked((current) => !current)}>{mapLocked ? 'Unlock map' : 'Lock map'}</button>
                    <p className="muted">{mapLocked ? 'Map movement is locked. Unlock when you need to inspect another part of the reserve.' : 'You can move and zoom the map, but it stays inside this reserve.'}</p>
                  </section>
                  <section>
                    <div className="panel-heading"><div><p className="eyebrow">Map layers</p><h2>Choose what appears</h2></div></div>
                    <div className="map-filter-buttons">
                      <button type="button" className={mapDisplay.showEvents ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setMapDisplay((current) => ({ ...current, showEvents: !current.showEvents }))}>{mapDisplay.showEvents ? 'Hide events' : 'Show events'}</button>
                      <button type="button" className={mapDisplay.showPois ? 'auth-tab auth-tab-active' : 'auth-tab'} onClick={() => setMapDisplay((current) => ({ ...current, showPois: !current.showPois }))}>{mapDisplay.showPois ? 'Hide POIs' : 'Show POIs'}</button>
                    </div>
                  </section>
                  <section>
                    <div className="panel-heading"><div><p className="eyebrow">Event filters</p><h2>Visible events</h2></div></div>
                    <form className="event-form control-filter-form" onSubmit={(event) => event.preventDefault()}>
                      <label>Status<select value={controlFilters.status} onChange={(event) => setControlFilters((current) => ({ ...current, status: event.target.value }))}><option value="ACTIVE">Active only</option><option value="">All statuses</option><option value="OPEN">Open</option><option value="IN_PROGRESS">In progress</option><option value="CLOSED">Closed</option></select></label>
                      <label>Priority<select value={controlFilters.priority} onChange={(event) => setControlFilters((current) => ({ ...current, priority: event.target.value }))}><option value="">All priorities</option><option value="HIGH">High</option><option value="MEDIUM">Medium</option><option value="LOW">Low</option></select></label>
                    </form>
                  </section>
                  <section>
                    <div className="panel-heading"><div><p className="eyebrow">Map actions</p><h2>Pick a location</h2></div></div>
                    <p className="muted">The map is locked to this reserve only. Choose what you want to create, then click a point on the map to open a small creation form right there.</p>
                    <div className="map-filter-buttons">
                      <button type="button" className={mapCreateMode === 'event' ? 'create-button auth-tab-active' : 'create-button'} onClick={() => { setMapCreateMode((current) => current === 'event' ? '' : 'event'); setMapPopup(null); setMapPopupMessage('') }}>{mapCreateMode === 'event' ? 'Cancel create event' : 'Create event'}</button>
                      <button type="button" className={mapCreateMode === 'poi' ? 'map-action-button auth-tab-active' : 'map-action-button'} onClick={() => { setMapCreateMode((current) => current === 'poi' ? '' : 'poi'); setMapPopup(null); setMapPopupMessage('') }}>{mapCreateMode === 'poi' ? 'Cancel create POI' : 'Create POI'}</button>
                    </div>
                  </section>
                  <section className="map-filter-summary">
                    <div className="summary-pill"><span>{filteredControlEvents.length}</span><small>Matching events</small></div>
                    <div className="summary-pill"><span>{selectedReservePois.length}</span><small>Known POIs</small></div>
                  </section>
                </aside>
                <article className="panel reserve-map-stage">
                  <div className="panel-heading"><div><p className="eyebrow">Reserve map</p><h2>{selectedReserve.displayName} area only</h2><p className="muted">Pan and zoom stay constrained to this reserve boundary.</p></div><PriorityBreakdown counts={selectedReserve.priorityCounts} /></div>
                  <div className="map-wrap map-wrap-full">
                    <MapContainer key={`control-${selectedReserve.id}-${controlFilters.status}-${controlFilters.priority}-${mapLocked ? 'locked' : 'free'}`} center={reserveCenter(selectedReserve)} zoom={12} scrollWheelZoom={!mapLocked} dragging={!mapLocked} doubleClickZoom={!mapLocked} touchZoom={!mapLocked} boxZoom={!mapLocked} keyboard={!mapLocked} className="reserve-map reserve-map-focus" maxBounds={reserveBounds(selectedReserve)} maxBoundsViscosity={1.0}>
                      <TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
                      <FitToReserve reserve={selectedReserve} />
                      <ReserveMapInteraction reserve={selectedReserve} onMapClick={handleReserveMapClick} />
                      <Rectangle
                        bounds={reserveBounds(selectedReserve)}
                        pathOptions={{ color: '#d14b27', weight: 3, dashArray: '12 8', fillOpacity: mapCreateMode ? 0.08 : 0 }}
                        eventHandlers={{
                          click: (event) => {
                            handleReserveMapClick(event.latlng.lat, event.latlng.lng)
                          }
                        }}
                      >
                        <Popup><strong>{selectedReserve.displayName}</strong><br />{selectedReserve.region}</Popup>
                      </Rectangle>
                      {mapDisplay.showPois ? selectedReservePois.filter((poi) => typeof poi.latitude === 'number' && typeof poi.longitude === 'number').map((poi) => <Marker key={`poi-${poi.id}`} position={[poi.latitude, poi.longitude]} icon={poiPinIcon(poi.typeName)}><Popup><strong>{poi.name}</strong><br />{poi.typeName}<br />{poi.description || 'No description provided.'}</Popup></Marker>) : null}
                      {mapDisplay.showEvents ? filteredControlEvents.filter((event) => typeof event.latitude === 'number' && typeof event.longitude === 'number').map((event) => <CircleMarker key={event.id} center={[event.latitude, event.longitude]} radius={priorityStyle(event.priority).markerRadius} pathOptions={{ color: priorityStyle(event.priority).accent, fillColor: priorityStyle(event.priority).fill, fillOpacity: 0.92, weight: 2 }}><Popup><strong>{event.type}</strong><br />{event.priority} priority<br />{event.status}<br />{event.description || 'No description provided.'}</Popup></CircleMarker>) : null}
                      {mapPopup ? <CircleMarker center={[mapPopup.latitude, mapPopup.longitude]} radius={7} pathOptions={{ color: '#1d5f5b', fillColor: '#fff8ef', fillOpacity: 1, weight: 3 }} /> : null}
                    </MapContainer>
                    {mapPopup ? <div className="map-floating-card">{mapPopup.type === 'coordinates' ? <>
                        <p className="eyebrow">Coordinates</p>
                        <h3>Clicked point</h3>
                        <p className="map-coordinates-readout">{mapPopup.latitude.toFixed(4)}, {mapPopup.longitude.toFixed(4)}</p>
                        <p className="muted">Use the buttons on the left if you want this click to create an event or a POI.</p>
                        <div className="map-popup-actions"><button type="button" className="secondary-button" onClick={closeMapPopup}>Close</button></div>
                      </> : mapPopup.type === 'event' ? <form className="map-popup-form" onSubmit={submitMapEvent}>
                        <p className="eyebrow">Create event</p>
                        <p className="map-coordinates-readout">{Number(mapEventDraft.latitude).toFixed(4)}, {Number(mapEventDraft.longitude).toFixed(4)}</p>
                        <label>Type<select value={mapEventDraft.type} onChange={(event) => setMapEventDraft((current) => ({ ...current, type: event.target.value }))}><option value="FIRE">Fire</option><option value="BLOCKAGE">Blockage</option><option value="OTHER">Other</option></select></label>
                        <label>Priority<select value={mapEventDraft.priority} onChange={(event) => setMapEventDraft((current) => ({ ...current, priority: event.target.value }))}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></label>
                        <label>Description<textarea value={mapEventDraft.description} onChange={(event) => setMapEventDraft((current) => ({ ...current, description: event.target.value }))} placeholder="Describe the issue." /></label>
                        <label className="checkbox-row"><input type="checkbox" checked={mapEventDraft.publishedToTravelers} onChange={(event) => setMapEventDraft((current) => ({ ...current, publishedToTravelers: event.target.checked }))} />Publish to travelers</label>
                        {mapPopupMessage ? <p className="error-banner">{mapPopupMessage}</p> : null}
                        <div className="map-popup-actions"><button type="button" className="secondary-button" onClick={closeMapPopup}>Cancel</button><button type="submit" className="create-button">Create</button></div>
                      </form> : <form className="map-popup-form" onSubmit={submitMapPoi}>
                        <p className="eyebrow">Create POI</p>
                        <p className="map-coordinates-readout">{Number(mapPoiDraft.latitude).toFixed(4)}, {Number(mapPoiDraft.longitude).toFixed(4)}</p>
                        <label>Type<select value={mapPoiDraft.typeId} onChange={(event) => setMapPoiDraft((current) => ({ ...current, typeId: event.target.value, customTypeName: event.target.value === '__custom__' ? current.customTypeName : '' }))} required><option value="" disabled>Select a type</option>{poiTypeOptions.map((type) => <option key={type.id} value={type.id}>{type.name}</option>)}</select></label>
                        {mapPoiDraft.typeId === '__custom__' ? <label>New type name<input type="text" value={mapPoiDraft.customTypeName} onChange={(event) => setMapPoiDraft((current) => ({ ...current, customTypeName: event.target.value }))} placeholder="Shuttle stop" required /></label> : null}
                        <label>Name<input type="text" value={mapPoiDraft.name} onChange={(event) => setMapPoiDraft((current) => ({ ...current, name: event.target.value }))} placeholder="Forest entrance" required /></label>
                        <label>Description<textarea value={mapPoiDraft.description} onChange={(event) => setMapPoiDraft((current) => ({ ...current, description: event.target.value }))} placeholder="Optional guidance." /></label>
                        {mapPopupMessage ? <p className="error-banner">{mapPopupMessage}</p> : null}
                        <div className="map-popup-actions"><button type="button" className="secondary-button" onClick={closeMapPopup}>Cancel</button><button type="submit" className="create-button">{mapPoiDraft.typeId === '__custom__' ? 'Create type + POI' : 'Create'}</button></div>
                      </form>}</div> : null}
                  </div>
                </article>
              </div>
            ) : (
              <div className="control-panel-workspace stack">
                <div className="control-operations-grid">
                  <article className="panel stack control-operation-card">
                    <div className="panel-heading"><div><p className="eyebrow">Events</p><h2>Create a new event</h2><p className="muted">Set the location first, then finish the event details here.</p></div></div>
                    <form className="event-form control-create-form" onSubmit={submitEvent}>
                      <p className="muted poi-helper-text event-form-wide">Create events here by entering coordinates manually.</p>
                      <label>Latitude<input type="number" step="0.0001" value={eventForm.latitude} onChange={(event) => { setEventForm((current) => ({ ...current, latitude: event.target.value })); setEventFormMessage('') }} required /></label>
                      <label>Longitude<input type="number" step="0.0001" value={eventForm.longitude} onChange={(event) => { setEventForm((current) => ({ ...current, longitude: event.target.value })); setEventFormMessage('') }} required /></label>
                      <p className="muted poi-helper-text event-form-wide">Use the map tab only when you want to create directly from a clicked map point.</p>
                      {eventFormMessage ? <p className="error-banner event-form-wide">{eventFormMessage}</p> : null}
                      <label>Type<select value={eventForm.type} onChange={(event) => setEventForm((current) => ({ ...current, type: event.target.value }))}><option value="FIRE">Fire</option><option value="BLOCKAGE">Blockage</option><option value="OTHER">Other</option></select></label>
                      <label>Priority<select value={eventForm.priority} onChange={(event) => setEventForm((current) => ({ ...current, priority: event.target.value }))}><option value="LOW">Low</option><option value="MEDIUM">Medium</option><option value="HIGH">High</option></select></label>
                      <label className="event-form-wide">Description<textarea value={eventForm.description} onChange={(event) => setEventForm((current) => ({ ...current, description: event.target.value }))} placeholder="Describe the issue inside this reserve." /></label>
                      <label className="checkbox-row event-form-wide"><input type="checkbox" checked={eventForm.publishedToTravelers} onChange={(event) => setEventForm((current) => ({ ...current, publishedToTravelers: event.target.checked }))} />Publish this event to the traveler mobile app</label>
                      <div className="event-actions event-form-wide"><button type="submit" className="create-button">Create event</button></div>
                    </form>
                  </article>
                  <article className="panel stack control-operation-card">
                    <div className="panel-heading"><div><p className="eyebrow">POIs</p><h2>{editingPoiId ? 'Edit a map pin' : 'Add a map pin'}</h2><p className="muted">Keep visitor-facing locations organized without crowding the map workspace.</p></div></div>
                    <form className="event-form control-create-form" onSubmit={submitPoi}>
                      <label>Type<select value={poiForm.typeId} onChange={(event) => setPoiForm((current) => ({ ...current, typeId: event.target.value, customTypeName: event.target.value === '__custom__' ? current.customTypeName : '' }))} required><option value="" disabled>Select a type</option>{poiTypeOptions.map((type) => <option key={type.id} value={type.id}>{type.name}</option>)}</select></label>
                      <label>Name<input type="text" value={poiForm.name} onChange={(event) => setPoiForm((current) => ({ ...current, name: event.target.value }))} placeholder="Forest entrance" required /></label>
                      {poiForm.typeId === '__custom__' ? <label className="event-form-wide">New type name<input type="text" value={poiForm.customTypeName} onChange={(event) => setPoiForm((current) => ({ ...current, customTypeName: event.target.value }))} placeholder="Shuttle stop" required /></label> : null}
                      <label className="event-form-wide">Description<textarea value={poiForm.description} onChange={(event) => setPoiForm((current) => ({ ...current, description: event.target.value }))} placeholder="Optional short guidance for visitors." /></label>
                      <label>Latitude<input type="number" step="0.0001" value={poiForm.latitude} onChange={(event) => setPoiForm((current) => ({ ...current, latitude: event.target.value }))} required /></label>
                      <label>Longitude<input type="number" step="0.0001" value={poiForm.longitude} onChange={(event) => setPoiForm((current) => ({ ...current, longitude: event.target.value }))} required /></label>
                      <p className="muted poi-helper-text">Create and edit POIs here by entering coordinates manually.</p>
                      <div className="event-actions event-form-wide">{editingPoiId ? <button type="button" className="secondary-button" onClick={resetPoiForm}>Cancel edit</button> : null}<button type="submit" className={editingPoiId ? 'save-button' : 'create-button'}>{editingPoiId ? 'Save POI' : 'Create POI'}</button></div>
                    </form>
                  </article>
                </div>
                <div className="control-operations-grid">
                  <article className="panel stack control-operation-card control-list-card">
                    <div className="panel-heading"><div><p className="eyebrow">POI list</p><h2>Current reserve locations</h2><p className="muted">Review and edit the reserve’s saved points of interest.</p></div></div>
                    <div className="event-list compact-card-list">{selectedReservePois.length === 0 ? <p className="empty-state">No POIs have been added yet.</p> : selectedReservePois.map((poi) => <article key={poi.id} className="event-card compact-card"><div className="event-card-header"><div><strong>{poi.name}</strong><span>{poi.typeName}</span></div></div><p>{poi.description || 'No description provided.'}</p><div className="event-meta"><span>{Number(poi.latitude).toFixed(4)}, {Number(poi.longitude).toFixed(4)}</span></div><div className="event-actions"><button type="button" className="edit-button" onClick={() => startPoiEdit(poi)}>Edit</button><button type="button" className="danger-button" onClick={() => window.confirm(`Delete POI "${poi.name}"?`) && onDeleteReservePoi(selectedReserve.id, poi.id).then(() => resetPoiForm())}>Delete</button></div></article>)}</div>
                  </article>
                  <article className="panel stack control-operation-card control-list-card control-event-feed">
                    <div className="panel-heading"><div><p className="eyebrow">Event stream</p><h2>{filteredControlEvents.length} matching events</h2><p className="muted">Update status, priority, or traveler visibility from one focused place.</p></div></div>
                    <div className="event-list control-event-list">{filteredControlEvents.length === 0 ? <p className="empty-state">No events match the current filters.</p> : filteredControlEvents.map((event) => {
                      const imageAttachments = imageAttachmentsForEvent(event)

                      return (
                        <article key={event.id} className={controlEventCardClass(event)}>
                          <div className="event-card-header"><div><strong>{event.type}</strong><span>{formatRelativeTime(event.updatedAt || event.createdAt)}</span></div><div className="event-chip-group">{event.status === 'OPEN' ? <span className="event-attention-badge" title="This event still needs manager attention">!</span> : null}<span className={`priority-chip ${priorityStyle(event.priority).badge}`}>{event.priority}</span><span className={`status-chip status-${event.status.toLowerCase()}`}>{event.status}</span></div></div>
                          <p>{event.description || 'No description provided.'}</p>
                          <div className="event-meta"><span>Source: {event.origin}</span><span>Public: {event.publishedToTravelers ? 'Visible to travelers' : 'Manager only'}</span>{event.reporterName ? <span>Reporter: {event.reporterName}</span> : null}</div>
                          {imageAttachments.length > 0 ? <div className="event-media-list">{imageAttachments.map((mediaItem) => <a key={mediaItem.id || mediaItem.mediaUrl} href={resolveMediaUrl(mediaItem.mediaUrl)} target="_blank" rel="noreferrer" className="event-media-link"><img src={resolveMediaUrl(mediaItem.mediaUrl)} alt={mediaItem.originalFilename || `${event.type} attachment`} className="event-media-preview" loading="lazy" /></a>)}</div> : null}
                          <div className="event-actions"><select value={event.priority} onChange={(actionEvent) => onUpdateEventPriority(event.id, actionEvent.target.value)}><option value="LOW">Low priority</option><option value="MEDIUM">Medium priority</option><option value="HIGH">High priority</option></select><select value={event.status} onChange={(actionEvent) => onUpdateEventStatus(event.id, actionEvent.target.value)}><option value="OPEN">Open</option><option value="IN_PROGRESS">In progress</option><option value="CLOSED">Closed</option></select><button type="button" className={event.publishedToTravelers ? 'secondary-button' : 'publish-button'} onClick={() => onUpdateEventPublish(event.id, !event.publishedToTravelers)}>{event.publishedToTravelers ? 'Hide from travelers' : 'Publish to travelers'}</button></div>
                        </article>
                      )
                    })}</div>
                  </article>
                </div>
              </div>
            )}
          </> : <article className="panel"><p className="empty-state">Choose a reserve from the active reserves page to open its control center.</p></article>}
        </section>
      ) : null}

      {page === 'requests' ? (
        <section className="manager-page stack">
          <div className="content-grid content-grid-wide">
            <article className="panel stack">
              <div className="panel-heading"><div><p className="eyebrow">New request</p><h2>Ask the admin for reserve access</h2></div></div>
              <form className="event-form" onSubmit={submitReserveRequest}>
                <label className="event-form-wide">Requested reserve name<input type="text" value={requestForm.reserveName} onChange={(event) => setRequestForm((current) => ({ ...current, reserveName: event.target.value }))} placeholder="Ein Gedi Nature Reserve" required /></label>
                <label className="event-form-wide">Message to admin<textarea value={requestForm.message} onChange={(event) => setRequestForm((current) => ({ ...current, message: event.target.value }))} placeholder="Explain why this reserve should be assigned to you." required /></label>
                <button type="submit" className="create-button">Send reserve request</button>
              </form>
            </article>
            <article className="panel stack">
              <div className="panel-heading"><div><p className="eyebrow">Filters</p><h2>Request history</h2></div></div>
              <form className="event-form request-filter-form" onSubmit={(event) => event.preventDefault()}>
                <label className="event-form-wide">Search<input type="text" value={requestFilters.search} onChange={(event) => setRequestFilters((current) => ({ ...current, search: event.target.value }))} placeholder="Reserve name, message, or status" /></label>
                <label>Status<select value={requestFilters.status} onChange={(event) => setRequestFilters((current) => ({ ...current, status: event.target.value }))}><option value="ALL">All statuses</option><option value="OPEN">Open</option><option value="APPROVED">Approved</option><option value="REJECTED">Rejected</option></select></label>
              </form>
            </article>
          </div>
          <div className="content-grid content-grid-wide">
            <article className="panel stack"><div className="panel-heading"><div><p className="eyebrow">Open requests</p><h2>Awaiting admin action</h2></div></div><div className="event-list">{openRequests.length === 0 ? <p className="empty-state">No open requests match the current filters.</p> : openRequests.map((request) => <article key={request.id} className="event-card"><div className="event-card-header"><div><strong>{request.reserveName}</strong><span>{formatDate(request.createdAt)}</span></div><span className={`status-chip status-${request.status.toLowerCase()}`}>{request.status}</span></div><p>{request.message}</p></article>)}</div></article>
            <article className="panel stack"><div className="panel-heading"><div><p className="eyebrow">History</p><h2>Resolved requests</h2></div></div><div className="event-list">{historyRequests.length === 0 ? <p className="empty-state">No resolved requests match the current filters.</p> : historyRequests.map((request) => <article key={request.id} className="event-card"><div className="event-card-header"><div><strong>{request.reserveName}</strong><span>{formatDate(request.resolvedAt || request.createdAt)}</span></div><span className={`status-chip status-${request.status.toLowerCase()}`}>{request.status}</span></div><p>{request.message}</p></article>)}</div></article>
          </div>
        </section>
      ) : null}
    </main>
  )
}
