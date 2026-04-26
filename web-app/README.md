# Web App

React and Vite dashboard for the reserve management system.

## Features

- Sign in and sign up flow for reserve managers
- Administrator console for reserve catalog review and manager assignment
- Manager workspace for reserve requests, event creation, event updates, and publishing traveler-facing alerts
- Map-based reserve views using Leaflet

## Related Docs

- [Web App Block Diagram](../docs/web-app-block-diagram.md)
- [Backend Block Diagram](../docs/backend-block-diagram.md)
- [System Architecture Planning Document](../docs/system-architecture-planning.md)

## Run

```bash
npm install
npm run dev
```

The app runs on `http://localhost:5173` by default.

Optional environment variable:

```bash
VITE_API_BASE_URL=http://localhost:8080
```
