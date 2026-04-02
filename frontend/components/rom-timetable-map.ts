import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

type RoutePoint = {
  name: string;
  uopid: string;
  country: string;
  latitude: number;
  longitude: number;
  role: 'ORIGIN' | 'VIA' | 'DESTINATION' | 'AUTO';
  distanceFromStartMeters: number;
  journeyLocationType: string;
};

type BackgroundOp = {
  uopid: string;
  name: string;
  latitude: number;
  longitude: number;
};

const DEFAULT_CENTER: [number, number] = [47.3769, 8.5417];
const DEFAULT_ZOOM = 7;
const BASEMAP_URL = 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png';
const ROUTE_COLOR = '#14b8a6';
const ROUTE_GLOW_WEIGHT = 10;
const ROUTE_LINE_WEIGHT = 3;
const FIT_PADDING = 40;

const ROLE_COLORS: Record<RoutePoint['role'], string> = {
  ORIGIN: '#0ea5e9',
  VIA: '#f59e0b',
  DESTINATION: '#ef4444',
  AUTO: '#64748b',
};

const ROLE_BADGE: Partial<Record<RoutePoint['role'], string>> = {
  ORIGIN: 'A',
  VIA: 'V',
  DESTINATION: 'Z',
};

let stylesInjected = false;

function injectMapStyles() {
  if (stylesInjected) return;
  stylesInjected = true;
  const style = document.createElement('style');
  style.textContent = `
    .rtm-marker {
      display: flex; align-items: center; gap: 5px; white-space: nowrap;
    }
    .rtm-marker__badge {
      width: 20px; height: 20px; border-radius: 50%;
      display: flex; align-items: center; justify-content: center;
      font-family: 'JetBrains Mono', monospace; font-size: 9px; font-weight: 700;
      color: #fff; border: 2px solid rgba(255,255,255,0.4); flex-shrink: 0;
    }
    .rtm-marker__label {
      display: flex; flex-direction: column;
      background: rgba(10,14,23,0.88); padding: 2px 6px; border-radius: 3px;
      border: 1px solid rgba(255,255,255,0.08);
    }
    .rtm-marker__name {
      font-family: 'Inter', sans-serif; font-size: 11px; font-weight: 600;
      color: #e2e8f0; line-height: 1.3;
    }
    .rtm-marker__km {
      font-family: 'JetBrains Mono', monospace; font-size: 9px;
      color: #94a3b8; line-height: 1.3;
    }
    .rtm-marker--auto .rtm-marker__badge {
      width: 7px; height: 7px; border-width: 1px;
      border-color: rgba(255,255,255,0.15);
    }
    .rtm-marker--auto .rtm-marker__label { display: none; }
    .rtm-marker--auto:hover .rtm-marker__label,
    .rtm-marker--auto:focus-within .rtm-marker__label { display: flex; }

    .rtm-bg-tooltip {
      background: rgba(10,14,23,0.88) !important; border: 1px solid rgba(255,255,255,0.08) !important;
      border-radius: 3px !important; color: #e2e8f0 !important;
      font-family: 'Inter', sans-serif !important; font-size: 11px !important;
      padding: 2px 6px !important; box-shadow: none !important;
    }
    .rtm-bg-tooltip::before { border-top-color: rgba(10,14,23,0.88) !important; }

    .rtm-overlay {
      background: rgba(10,14,23,0.92); border: 1px solid rgba(255,255,255,0.08);
      border-radius: 6px; padding: 8px 12px; color: #e2e8f0; pointer-events: none;
    }
    .rtm-overlay__header {
      font-family: 'Inter', sans-serif; font-size: 12px; font-weight: 600;
      display: flex; align-items: center; gap: 6px; margin-bottom: 3px;
    }
    .rtm-overlay__arrow { color: #14b8a6; }
    .rtm-overlay__stats {
      display: flex; gap: 12px;
      font-family: 'JetBrains Mono', monospace; font-size: 10px; color: #94a3b8;
    }
  `;
  document.head.appendChild(style);
}

function escapeHtml(text: string): string {
  const el = document.createElement('div');
  el.textContent = text;
  return el.innerHTML;
}

class RomTimetableMap extends HTMLElement {
  private map?: any;
  private container?: HTMLDivElement;
  private routeLayer?: any;
  private layers: any[] = [];
  private bgMarkers: any[] = [];
  private overlay?: any;
  private resizeObserver?: ResizeObserver;

  connectedCallback() {
    injectMapStyles();

    if (!this.container) {
      this.container = document.createElement('div');
      this.container.style.cssText = 'width:100%;height:100%;min-height:200px';
      this.appendChild(this.container);
    }

    if (!this.map) {
      this.map = L.map(this.container, {
        zoomControl: true,
        attributionControl: false,
      }).setView(DEFAULT_CENTER, DEFAULT_ZOOM);

      L.tileLayer(BASEMAP_URL, {
        maxZoom: 18,
      }).addTo(this.map);

      L.control
        .attribution({ prefix: false, position: 'bottomleft' })
        .addAttribution(
          '&copy; <a href="https://osm.org/copyright">OSM</a> &amp; <a href="https://carto.com/">CARTO</a>',
        )
        .addTo(this.map);
    }

    // Re-register observer on every attach (fixes re-attach after step switch)
    this.resizeObserver?.disconnect();
    this.resizeObserver = new ResizeObserver(() => this.map?.invalidateSize());
    this.resizeObserver.observe(this);
    requestAnimationFrame(() => this.map?.invalidateSize());
  }

  disconnectedCallback() {
    this.resizeObserver?.disconnect();
    this.resizeObserver = undefined;
  }

  /** Permanently destroy the map and release resources (e.g., when navigating away). */
  destroy() {
    this.clearRoute();
    this.bgMarkers.forEach((m) => m.remove());
    this.bgMarkers = [];
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
  }

  clearRoute() {
    this.routeLayer?.remove();
    this.routeLayer = undefined;
    this.layers.forEach((layer) => layer.remove());
    this.layers = [];
    if (this.overlay) {
      this.overlay.remove();
      this.overlay = undefined;
    }
  }

  setRoute(points: RoutePoint[]) {
    if (!this.map) return;
    this.clearRoute();

    const valid = points.filter(
      (p) => Number.isFinite(p.latitude) && Number.isFinite(p.longitude),
    );
    if (valid.length === 0) {
      this.map.setView(DEFAULT_CENTER, DEFAULT_ZOOM);
      return;
    }

    const latLngs = valid.map((p) => L.latLng(p.latitude, p.longitude));

    // Route glow (wide, semi-transparent) for depth
    this.layers.push(
      L.polyline(latLngs, {
        color: ROUTE_COLOR,
        weight: ROUTE_GLOW_WEIGHT,
        opacity: 0.12,
        lineCap: 'round',
        lineJoin: 'round',
      }).addTo(this.map),
    );

    // Main route line
    this.routeLayer = L.polyline(latLngs, {
      color: ROUTE_COLOR,
      weight: ROUTE_LINE_WEIGHT,
      opacity: 0.9,
      lineCap: 'round',
      lineJoin: 'round',
    }).addTo(this.map);

    // Station markers
    valid.forEach((point) => {
      const isKey = point.role !== 'AUTO';
      const color = ROLE_COLORS[point.role] ?? ROLE_COLORS.AUTO;
      const km = (point.distanceFromStartMeters / 1000).toFixed(1);
      const badge = ROLE_BADGE[point.role] ?? '';
      const cls = isKey ? 'rtm-marker' : 'rtm-marker rtm-marker--auto';

      const title = `${point.name} (${km} km)`;
      const html =
        `<div class="${cls}" title="${escapeHtml(title)}">` +
        `<div class="rtm-marker__badge" style="background:${color};box-shadow:0 0 6px ${color}88">${badge}</div>` +
        `<div class="rtm-marker__label">` +
        `<span class="rtm-marker__name">${escapeHtml(point.name)}</span>` +
        `<span class="rtm-marker__km">${km} km${point.country ? ' \u00b7 ' + escapeHtml(point.country) : ''}</span>` +
        `</div></div>`;

      const icon = L.divIcon({
        html,
        className: '',
        iconSize: [0, 0],
        iconAnchor: isKey ? [-12, 10] : [-5, 4],
      });

      const marker = L.marker([point.latitude, point.longitude], {
        icon,
        alt: title,
      });
      this.layers.push(marker.addTo(this.map!));
    });

    this.addRouteOverlay(points);

    if (latLngs.length === 1) {
      this.map.setView(latLngs[0], 11);
    } else {
      this.map.fitBounds(this.routeLayer.getBounds(), { padding: [FIT_PADDING, FIT_PADDING] });
    }
  }

  setOperationalPoints(ops: BackgroundOp[]) {
    if (!this.map) return;

    // Remove existing background markers
    this.bgMarkers.forEach((m) => m.remove());
    this.bgMarkers = [];

    const valid = ops.filter(
      (op) => Number.isFinite(op.latitude) && Number.isFinite(op.longitude),
    );

    for (const op of valid) {
      const marker = L.circleMarker([op.latitude, op.longitude], {
        radius: 3,
        color: '#94a3b8',
        fillColor: '#94a3b8',
        fillOpacity: 0.3,
        opacity: 0.3,
        weight: 1,
      });

      marker.bindTooltip(escapeHtml(op.name), {
        direction: 'top',
        offset: [0, -5],
        className: 'rtm-bg-tooltip',
      });

      marker.on('click', () => {
        this.dispatchEvent(
          new CustomEvent('op-selected', {
            detail: { uopid: op.uopid, name: op.name },
            bubbles: true,
            composed: true,
          }),
        );
      });

      marker.addTo(this.map);
      this.bgMarkers.push(marker);
    }
  }

  private addRouteOverlay(points: RoutePoint[]) {
    const origin = points.find((p) => p.role === 'ORIGIN');
    const dest = points.find((p) => p.role === 'DESTINATION');
    const totalKm = dest
      ? (dest.distanceFromStartMeters / 1000).toFixed(1)
      : '0';
    const viaCount = points.filter((p) => p.role === 'VIA').length;
    const stopCount = points.length;

    const OverlayControl = L.Control.extend({
      options: { position: 'topright' },
      onAdd() {
        const el = L.DomUtil.create('div', 'rtm-overlay');
        el.innerHTML =
          `<div class="rtm-overlay__header">` +
          `<span>${escapeHtml(origin?.name ?? '?')}</span>` +
          `<span class="rtm-overlay__arrow">\u2192</span>` +
          `<span>${escapeHtml(dest?.name ?? '?')}</span></div>` +
          `<div class="rtm-overlay__stats">` +
          `<span>${totalKm} km</span>` +
          `<span>${stopCount} OP</span>` +
          `${viaCount > 0 ? `<span>${viaCount} Via</span>` : ''}` +
          `</div>`;
        return el;
      },
    });

    this.overlay = new OverlayControl();
    this.overlay.addTo(this.map!);
  }
}

customElements.define('rom-timetable-map', RomTimetableMap);
