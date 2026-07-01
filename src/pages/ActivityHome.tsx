import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ChevronRight, Home, Play } from "lucide-react";
import { api, type ActivityKind, type SessionRow } from "@/lib/api";
import { ACTIVITY_CONFIG } from "@/lib/activities";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  formatInstallDate,
  formatDate,
  formatDistance,
  formatDuration,
  formatPace,
  formatSpeed,
} from "@/lib/format";

export function ActivityHome() {
  const { activity } = useParams<{ activity: string }>();
  const kind = activity as ActivityKind;
  const config = ACTIVITY_CONFIG[kind];
  const nav = useNavigate();
  const [recent, setRecent] = useState<SessionRow[]>([]);
  const [installationUpdatedAtMs, setInstallationUpdatedAtMs] = useState<
    number | null
  >(null);
  const [starting, setStarting] = useState(false);

  useEffect(() => {
    api
      .installationUpdatedAtMs()
      .then(setInstallationUpdatedAtMs)
      .catch(console.error);
    api.listRecent(5, kind).then(setRecent).catch(console.error);
    // Resume into active tracking if this activity has a session already running.
    api.activeActivity().then((a) => {
      if (a === kind) nav(`/${kind}/track`, { replace: true });
    });
  }, [nav, kind]);

  async function onStart() {
    if (starting) return;
    setStarting(true);
    try {
      await api.startSession(kind);
      nav(`/${kind}/track`);
    } catch (e) {
      console.error(e);
      setStarting(false);
    }
  }

  const formatMetric = config.metric === "pace" ? formatPace : formatSpeed;

  return (
    <>
      <header className="mb-8 flex items-start justify-between mt-4">
        <div>
          <h1 className="font-display text-4xl tracking-tight">
            {config.title}
          </h1>
          {installationUpdatedAtMs && (
            <p className="mt-1 text-sm text-muted-foreground">
              Version {formatInstallDate(installationUpdatedAtMs)}
            </p>
          )}
        </div>
        <Link
          to="/"
          className="p-2 -m-2 text-muted-foreground hover:text-foreground"
          aria-label="All apps"
        >
          <Home className="h-5 w-5" />
        </Link>
      </header>

      <Button
        onClick={onStart}
        disabled={starting}
        size="xl"
        className="w-full mb-8 font-display text-xl"
      >
        <Play className="h-5 w-5" /> {config.verb}
      </Button>

      <div className="flex items-baseline justify-between mb-3">
        <h2 className="font-display text-lg">Recent</h2>
        <Link
          to={`/${kind}/history`}
          className="text-sm text-brand hover:underline inline-flex items-center gap-1"
        >
          History <ChevronRight className="h-4 w-4" />
        </Link>
      </div>

      {recent.length === 0 ? (
        <Card className="p-6 text-center text-sm text-muted-foreground">
          No {config.nounPlural.toLowerCase()} yet. Tap "{config.verb}" to
          record your first.
        </Card>
      ) : (
        <ul className="space-y-2">
          {recent.map((s) => (
            <li key={s.id}>
              <Link
                to={`/${kind}/summary/${s.id}`}
                className="block rounded-xl border border-border bg-card px-4 py-3 hover:border-muted transition-colors"
              >
                <div className="flex items-baseline justify-between">
                  <span className="font-medium">
                    {formatDate(s.started_at_ms)}
                  </span>
                  <span className="font-display text-lg tabular-nums">
                    {formatDistance(s.total_distance_m)}
                  </span>
                </div>
                <div className="flex justify-between text-sm text-muted-foreground tabular-nums">
                  <span>{formatDuration(s.moving_duration_ms ?? 0)}</span>
                  <span>{formatMetric(s.avg_pace_s_per_km)}</span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
