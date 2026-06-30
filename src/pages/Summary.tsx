import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Trash2 } from "lucide-react";
import { api, type SessionDetail } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { ElevationChart } from "@/components/ElevationChart";
import { RouteMap } from "@/components/RouteMap";
import { SplitsTable } from "@/components/SplitsTable";
import {
  formatDateTime, formatDistance, formatDuration, formatPace,
} from "@/lib/format";

export function Summary() {
  const { id } = useParams();
  const nav = useNavigate();
  const [detail, setDetail] = useState<SessionDetail | null>(null);

  useEffect(() => {
    if (!id) return;
    api.getSession(Number(id)).then(setDetail).catch(console.error);
  }, [id]);

  async function onDelete() {
    if (!detail) return;
    if (!confirm("Delete this run?")) return;
    await api.deleteSession(detail.session.id);
    nav("/", { replace: true });
  }

  if (!detail) {
    return <p className="text-muted-foreground">Loading…</p>;
  }

  const s = detail.session;

  return (
    <>
      <header className="flex items-center justify-between mb-5">
        <Link to="/" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="h-4 w-4 mr-1" /> Home
        </Link>
        <button
          onClick={onDelete}
          className="text-muted-foreground hover:text-destructive"
          aria-label="Delete run"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </header>

      <h1 className="font-display text-3xl tracking-tight">
        {formatDistance(s.total_distance_m)} · {formatDuration(s.moving_duration_ms ?? 0)}
      </h1>
      <p className="text-sm text-muted-foreground mb-5">{formatDateTime(s.started_at_ms)}</p>

      <Card className="mb-5">
        <CardContent className="p-2">
          <RouteMap points={detail.points} height={220} />
        </CardContent>
      </Card>

      <div className="grid grid-cols-2 gap-3 mb-5">
        <Stat label="Avg pace" value={formatPace(s.avg_pace_s_per_km)} />
        <Stat label="Moving time" value={formatDuration(s.moving_duration_ms ?? 0)} />
        <Stat label="Total time" value={formatDuration(s.total_duration_ms ?? 0)} />
        <Stat
          label="Elevation"
          value={`↑${(s.elevation_gain_m ?? 0).toFixed(0)} ↓${(s.elevation_loss_m ?? 0).toFixed(0)} m`}
        />
      </div>

      <h2 className="font-display text-lg mb-2">Splits</h2>
      <div className="mb-6">
        <SplitsTable splits={detail.splits} />
      </div>

      <h2 className="font-display text-lg mb-2">Elevation</h2>
      <Card>
        <CardContent>
          <ElevationChart points={detail.points} />
        </CardContent>
      </Card>

      <div className="mt-6">
        <Button onClick={() => nav("/")} className="w-full" variant="outline">
          Done
        </Button>
      </div>
    </>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <div className="text-xs uppercase tracking-widest text-muted-foreground">{label}</div>
      <div className="mt-1 font-display text-xl tabular-nums">{value}</div>
    </div>
  );
}
