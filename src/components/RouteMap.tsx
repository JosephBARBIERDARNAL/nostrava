import { useEffect, useMemo, useRef } from "react";
import * as L from "leaflet";
import type {
  CircleMarker,
  LatLngExpression,
  Map as LeafletMap,
  Polyline,
  TileLayer,
} from "leaflet";
import type { TrackPoint } from "@/lib/api";

interface Props {
  points: TrackPoint[];
  height?: number;
  live?: boolean;
  className?: string;
  emptyLabel?: string;
}

const TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
const TILE_ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>';

export function RouteMap({
  points,
  height = 220,
  live = false,
  className,
  emptyLabel = "No track recorded",
}: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<LeafletMap | null>(null);
  const tileLayerRef = useRef<TileLayer | null>(null);
  const routeLayerRef = useRef<Polyline | null>(null);
  const markerLayerRefs = useRef<CircleMarker[]>([]);

  const active = useMemo(() => points.filter((p) => !p.paused), [points]);
  const positions = useMemo<LatLngExpression[]>(
    () => active.map((p) => [p.lat, p.lng]),
    [active],
  );

  useEffect(() => {
    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
      tileLayerRef.current = null;
      routeLayerRef.current = null;
      markerLayerRefs.current = [];
    };
  }, []);

  useEffect(() => {
    if (!containerRef.current || positions.length < 2) return;

    if (!mapRef.current) {
      mapRef.current = L.map(containerRef.current, {
        attributionControl: true,
        zoomControl: false,
        scrollWheelZoom: false,
      });
      tileLayerRef.current = L.tileLayer(TILE_URL, {
        maxZoom: 19,
        attribution: TILE_ATTRIBUTION,
      }).addTo(mapRef.current);
    }

    const map = mapRef.current;
    if (routeLayerRef.current) routeLayerRef.current.remove();
    markerLayerRefs.current.forEach((marker) => marker.remove());

    routeLayerRef.current = L.polyline(positions, {
      color: "hsl(273 78% 35%)",
      weight: 4,
      opacity: 0.9,
      lineCap: "round",
      lineJoin: "round",
    }).addTo(map);

    const start = positions[0];
    const end = positions[positions.length - 1];
    markerLayerRefs.current = [
      L.circleMarker(start, {
        radius: 5,
        color: "hsl(273 78% 35%)",
        fillColor: "white",
        fillOpacity: 1,
        weight: 3,
      }).addTo(map),
      L.circleMarker(end, {
        radius: live ? 8 : 6,
        color: "black",
        fillColor: "hsl(45 100% 50%)",
        fillOpacity: 1,
        weight: 1.5,
      }).addTo(map),
    ];

    map.fitBounds(routeLayerRef.current.getBounds(), {
      padding: [26, 26],
      maxZoom: live ? 17 : 16,
    });
    map.invalidateSize();
  }, [positions, live]);

  if (active.length < 2) {
    return (
      <div
        data-testid="route-map-empty"
        className={`grid place-items-center rounded-lg border border-border bg-muted/20 text-xs text-muted-foreground ${className ?? ""}`}
        style={{ height }}
      >
        {emptyLabel}
      </div>
    );
  }

  return (
    <div
      ref={containerRef}
      role="img"
      aria-label={live ? "Live route map" : "Route map"}
      data-testid="route-map"
      className={`overflow-hidden rounded-lg border border-border bg-muted/20 ${className ?? ""}`}
      style={{ height }}
    />
  );
}
