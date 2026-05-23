import type { TrackPoint } from "@/lib/api";

interface Props {
  points: TrackPoint[];
  width?: number;
  height?: number;
  className?: string;
}

/**
 * Simple SVG polyline preview of a GPS track. No map tiles — just the shape.
 * Normalises coordinates to fit the viewBox while preserving aspect ratio.
 */
export function PolylinePreview({ points, width = 360, height = 200, className }: Props) {
  const active = points.filter((p) => !p.paused);
  if (active.length < 2) {
    return (
      <div
        className={className}
        style={{
          width,
          height,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "hsl(var(--muted-foreground))",
          fontSize: 12,
        }}
      >
        No track recorded
      </div>
    );
  }

  const lats = active.map((p) => p.lat);
  const lngs = active.map((p) => p.lng);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);

  // Cosine correction so distance feels right on screen.
  const meanLat = (minLat + maxLat) / 2;
  const xScaleRaw = (maxLng - minLng) * Math.cos((meanLat * Math.PI) / 180);
  const yScaleRaw = maxLat - minLat;

  const pad = 12;
  const w = width - pad * 2;
  const h = height - pad * 2;
  const scale = Math.min(w / Math.max(xScaleRaw, 1e-9), h / Math.max(yScaleRaw, 1e-9));
  const projW = xScaleRaw * scale;
  const projH = yScaleRaw * scale;
  const offX = pad + (w - projW) / 2;
  const offY = pad + (h - projH) / 2;

  const project = (lat: number, lng: number) => {
    const x = offX + (lng - minLng) * Math.cos((meanLat * Math.PI) / 180) * scale;
    // SVG y axis is flipped (north should be up).
    const y = offY + (maxLat - lat) * scale;
    return [x, y] as const;
  };

  const d = active
    .map((p, i) => {
      const [x, y] = project(p.lat, p.lng);
      return `${i === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`;
    })
    .join(" ");

  const [sx, sy] = project(active[0].lat, active[0].lng);
  const [ex, ey] = project(active[active.length - 1].lat, active[active.length - 1].lng);

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      width="100%"
      height={height}
      className={className}
      preserveAspectRatio="xMidYMid meet"
    >
      <rect
        x={0.5}
        y={0.5}
        width={width - 1}
        height={height - 1}
        rx={12}
        fill="hsl(var(--card))"
        stroke="hsl(var(--border))"
      />
      <path d={d} stroke="hsl(var(--brand))" strokeWidth={3} fill="none" strokeLinecap="round" strokeLinejoin="round" />
      <circle cx={sx} cy={sy} r={5} fill="hsl(var(--brand))" />
      <circle cx={ex} cy={ey} r={5} fill="hsl(var(--accent))" stroke="black" strokeWidth={1} />
    </svg>
  );
}
