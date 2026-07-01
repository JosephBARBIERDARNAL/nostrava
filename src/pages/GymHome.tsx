import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ChevronRight, Dumbbell, Home } from "lucide-react";
import { api, type GymSessionRow } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { formatDate } from "@/lib/format";

export function GymHome() {
  const nav = useNavigate();
  const [recent, setRecent] = useState<GymSessionRow[]>([]);

  useEffect(() => {
    api.listRecentGym(5).then(setRecent).catch(console.error);
  }, []);

  return (
    <>
      <header className="mb-8 flex items-start justify-between">
        <h1 className="font-display text-4xl tracking-tight">Gym</h1>
        <Link
          to="/"
          className="p-2 -m-2 text-muted-foreground hover:text-foreground"
          aria-label="All apps"
        >
          <Home className="h-5 w-5" />
        </Link>
      </header>

      <Button
        onClick={() => nav("/gym/log")}
        size="xl"
        className="w-full mb-8 font-display text-xl"
      >
        <Dumbbell className="h-5 w-5" /> Log workout
      </Button>

      <div className="flex items-baseline justify-between mb-3">
        <h2 className="font-display text-lg">Recent</h2>
        <Link
          to="/gym/history"
          className="text-sm text-brand hover:underline inline-flex items-center gap-1"
        >
          History <ChevronRight className="h-4 w-4" />
        </Link>
      </div>

      {recent.length === 0 ? (
        <Card className="p-6 text-center text-sm text-muted-foreground">
          No workouts yet. Tap "Log workout" to record your first.
        </Card>
      ) : (
        <ul className="space-y-2">
          {recent.map((s) => (
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
                    {s.exercises.length} exercise
                    {s.exercises.length === 1 ? "" : "s"}
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
