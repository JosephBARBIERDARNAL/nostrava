import type { TrackPoint } from "@/lib/api";

interface Props {
  points: TrackPoint[];
  height?: number;
}

export function ElevationChart({ points, height = 80 }: Props) {
  const alts = points
    .filter((p) => !p.paused && p.altitude_m != null)
    .map((p) => p.altitude_m as number);

  if (alts.length < 3) {
    return (
      <div className="text-xs text-muted-foreground py-3 text-center">
        No elevation data
      </div>
    );
  }

  const w = 360;
  const h = height;
  const min = Math.min(...alts);
  const max = Math.max(...alts);
  const range = Math.max(max - min, 1);

  const pts = alts
    .map((a, i) => {
      const x = (i / (alts.length - 1)) * w;
      const y = h - ((a - min) / range) * h;
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");

  return (
    <svg viewBox={`0 0 ${w} ${h}`} width="100%" height={h} preserveAspectRatio="none">
      <polyline points={pts} fill="none" stroke="hsl(var(--brand))" strokeWidth={2} />
    </svg>
  );
}
