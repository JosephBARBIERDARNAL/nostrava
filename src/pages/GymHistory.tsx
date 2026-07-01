import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, ChevronLeft, ChevronRight } from "lucide-react";
import { api, type GymHistoryBucket, type Range } from "@/lib/api";
import { Card } from "@/components/ui/card";
import { Tabs, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { formatDate } from "@/lib/format";

export function GymHistory() {
  const [range, setRange] = useState<Range>("week");
  const [anchorMs, setAnchorMs] = useState<number>(Date.now());
  const [bucket, setBucket] = useState<GymHistoryBucket | null>(null);

  useEffect(() => {
    api.listGymRange(range, anchorMs).then(setBucket).catch(console.error);
  }, [range, anchorMs]);

  function shift(forward: boolean) {
    const d = new Date(anchorMs);
    switch (range) {
      case "week":
        d.setDate(d.getDate() + (forward ? 7 : -7));
        break;
      case "month":
        d.setMonth(d.getMonth() + (forward ? 1 : -1));
        break;
      case "year":
        d.setFullYear(d.getFullYear() + (forward ? 1 : -1));
        break;
    }
    setAnchorMs(d.getTime());
  }

  return (
    <>
      <header className="flex items-center justify-between mb-5">
        <Link
          to="/gym"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4 mr-1" /> Home
        </Link>
        <h1 className="font-display text-lg">History</h1>
        <span className="w-12" />
      </header>

      <Tabs
        value={range}
        onValueChange={(v) => setRange(v as Range)}
        className="mb-5"
      >
        <TabsList className="w-full grid grid-cols-3">
          <TabsTrigger value="week">Week</TabsTrigger>
          <TabsTrigger value="month">Month</TabsTrigger>
          <TabsTrigger value="year">Year</TabsTrigger>
        </TabsList>
      </Tabs>

      <div className="flex items-center justify-between mb-4">
        <button
          onClick={() => shift(false)}
          className="p-2 text-muted-foreground hover:text-foreground"
          aria-label="Previous"
        >
          <ChevronLeft className="h-5 w-5" />
        </button>
        <p className="font-display text-base">{bucket?.label ?? "—"}</p>
        <button
          onClick={() => shift(true)}
          className="p-2 text-muted-foreground hover:text-foreground"
          aria-label="Next"
        >
          <ChevronRight className="h-5 w-5" />
        </button>
      </div>

      {bucket && (
        <div className="mb-5">
          <Stat label="Workouts" value={bucket.session_count.toString()} />
        </div>
      )}

      {bucket && bucket.sessions.length === 0 ? (
        <Card className="p-6 text-center text-sm text-muted-foreground">
          No workouts in this {range}.
        </Card>
      ) : (
        <ul className="space-y-2">
          {bucket?.sessions.map((s) => (
            <li key={s.id}>
              <Link
                to={`/gym/summary/${s.id}`}
                className="block rounded-xl border border-border bg-card px-4 py-3 hover:border-muted transition-colors"
              >
                <div className="flex items-baseline justify-between">
                  <span className="font-medium">
                    {formatDate(s.logged_at_ms)}
                  </span>
                  <span className="text-sm text-muted-foreground">
                    {s.exercises.length} exercise{s.exercises.length === 1 ? "" : "s"}
                  </span>
                </div>
                <p className="text-sm text-muted-foreground truncate">
                  {s.exercises.join(", ")}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-3 text-center">
      <div className="text-[10px] uppercase tracking-widest text-muted-foreground">
        {label}
      </div>
      <div className="mt-1 font-display text-base tabular-nums">{value}</div>
    </div>
  );
}
