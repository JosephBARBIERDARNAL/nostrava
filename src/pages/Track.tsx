import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Pause, Play, Square } from "lucide-react";
import { api, onMetrics, type LiveMetrics, type TrackPoint } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { PolylinePreview } from "@/components/PolylinePreview";
import { formatDistance, formatDuration, formatPace } from "@/lib/format";

export function Track() {
  const nav = useNavigate();
  const [metrics, setMetrics] = useState<LiveMetrics | null>(null);
  const [points, setPoints] = useState<TrackPoint[]>([]);
  const [busy, setBusy] = useState(false);
  const unlistenRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    let alive = true;
    api.liveMetrics().then((m) => alive && setMetrics(m));
    api.livePoints().then((pts) => alive && setPoints(pts ?? []));
    onMetrics((m) => {
      if (!alive) return;
      setMetrics(m);
      api.livePoints().then((pts) => alive && setPoints(pts ?? []));
    }).then((un) => {
      unlistenRef.current = un;
    });
    return () => {
      alive = false;
      unlistenRef.current?.();
    };
  }, []);

  // If somehow we land here without an active session, send the user home.
  useEffect(() => {
    api.currentState().then((s) => {
      if (s === "idle") nav("/", { replace: true });
    });
  }, [nav]);

  async function onPause() {
    if (busy) return;
    setBusy(true);
    try {
      await api.pauseSession();
      const m = await api.liveMetrics();
      setMetrics(m);
      setPoints((await api.livePoints()) ?? []);
    } finally {
      setBusy(false);
    }
  }

  async function onResume() {
    if (busy) return;
    setBusy(true);
    try {
      await api.resumeSession();
      const m = await api.liveMetrics();
      setMetrics(m);
      setPoints((await api.livePoints()) ?? []);
    } finally {
      setBusy(false);
    }
  }

  async function onStop() {
    if (busy) return;
    setBusy(true);
    try {
      const detail = await api.stopSession();
      nav(`/summary/${detail.session.id}`, { replace: true });
    } catch (e) {
      console.error(e);
      setBusy(false);
    }
  }

  const isPaused = metrics?.state === "paused";

  return (
    <div className="flex-1 flex flex-col">
      <header className="text-center mb-10">
        <p className="text-xs uppercase tracking-widest text-muted-foreground">
          {isPaused ? "Paused" : "Running"}
        </p>
      </header>

      <div className="flex-1 flex flex-col items-center justify-center gap-8">
        <div
          className={`relative w-64 h-64 rounded-full grid place-items-center transition-colors ${
            isPaused ? "bg-muted/30" : "bg-accent/15"
          }`}
        >
          <div
            className={`absolute inset-3 rounded-full border-[3px] ${
              isPaused ? "border-muted" : "border-brand"
            }`}
          />
          <span className="font-display text-5xl tabular-nums">
            {formatDuration(metrics?.moving_duration_ms ?? 0)}
          </span>
        </div>

        <div className="grid grid-cols-2 gap-4 w-full">
          <Stat label="Distance" value={formatDistance(metrics?.total_distance_m)} />
          <Stat label="Pace" value={formatPace(metrics?.avg_pace_s_per_km)} />
        </div>

        <div className="w-full rounded-xl border border-border bg-card p-2">
          <PolylinePreview points={points} height={180} />
        </div>
      </div>

      <div className="mt-10 grid grid-cols-2 gap-3">
        {isPaused ? (
          <Button onClick={onResume} disabled={busy} size="lg" variant="primary">
            <Play className="h-5 w-5" /> Resume
          </Button>
        ) : (
          <Button onClick={onPause} disabled={busy} size="lg" variant="primary">
            <Pause className="h-5 w-5" /> Pause
          </Button>
        )}
        <Button onClick={onStop} disabled={busy} size="lg" variant="outline">
          <Square className="h-5 w-5" /> Stop
        </Button>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4 text-center">
      <div className="text-xs uppercase tracking-widest text-muted-foreground">{label}</div>
      <div className="mt-1 font-display text-2xl tabular-nums">{value}</div>
    </div>
  );
}
