import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

type RoutePoint = {
  name: string;
  latitude: number;
  longitude: number;
  role: 'ORIGIN' | 'VIA' | 'DESTINATION' | 'AUTO';
};

class RomTimetableMap extends HTMLElement {
  private map?: any;
  private container?: HTMLDivElement;
  private routeLayer?: any;
  private markers: any[] = [];
  private resizeObserver?: ResizeObserver;

  connectedCallback() {
    if (!this.container) {
      this.container = document.createElement('div');
      this.container.style.width = '100%';
      this.container.style.height = '100%';
      this.container.style.minHeight = '420px';
      this.appendChild(this.container);
    }

    if (!this.map) {
      this.map = L.map(this.container, {
        zoomControl: true,
        attributionControl: true,
      }).setView([47.3769, 8.5417], 7);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors',
        maxZoom: 18,
      }).addTo(this.map);

      this.resizeObserver = new ResizeObserver(() => this.map?.invalidateSize());
      this.resizeObserver.observe(this);
      requestAnimationFrame(() => this.map?.invalidateSize());
    }
  }

  disconnectedCallback() {
    this.resizeObserver?.disconnect();
  }

  clearRoute() {
    this.routeLayer?.remove();
    this.routeLayer = undefined;
    this.markers.forEach((marker) => marker.remove());
    this.markers = [];
  }

  setRoute(points: RoutePoint[]) {
    if (!this.map) {
      return;
    }

    this.clearRoute();

    const latLngs = points
      .filter((point) => Number.isFinite(point.latitude) && Number.isFinite(point.longitude))
      .map((point) => [point.latitude, point.longitude]);

    if (latLngs.length === 0) {
      this.map.setView([47.3769, 8.5417], 7);
      return;
    }

    this.routeLayer = L.polyline(latLngs, {
      color: '#14b8a6',
      weight: 4,
      opacity: 0.85,
    }).addTo(this.map);

    points.forEach((point, index) => {
      if (!Number.isFinite(point.latitude) || !Number.isFinite(point.longitude)) {
        return;
      }
      const color =
        point.role === 'ORIGIN'
          ? '#0ea5e9'
          : point.role === 'DESTINATION'
            ? '#ef4444'
            : point.role === 'VIA'
              ? '#f59e0b'
              : '#94a3b8';

      const marker = L.circleMarker([point.latitude, point.longitude], {
        radius: point.role === 'AUTO' ? 4 : 6,
        color,
        weight: 2,
        fillColor: color,
        fillOpacity: point.role === 'AUTO' ? 0.45 : 0.9,
      }).addTo(this.map!);

      marker.bindTooltip(`${index + 1}. ${point.name}`, {
        direction: 'top',
        offset: [0, -4],
      });
      this.markers.push(marker);
    });

    if (latLngs.length === 1) {
      this.map.setView(latLngs[0], 11);
    } else {
      this.map.fitBounds(this.routeLayer.getBounds(), { padding: [24, 24] });
    }
  }
}

customElements.define('rom-timetable-map', RomTimetableMap);
