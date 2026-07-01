import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Trash2 } from "lucide-react";
import { api, type GymSessionRow } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { formatDateTime } from "@/lib/format";

export function GymSummary() {
  const { id } = useParams();
  const nav = useNavigate();
  const [session, setSession] = useState<GymSessionRow | null>(null);

  useEffect(() => {
    if (!id) return;
    api.getGymSession(Number(id)).then(setSession).catch(console.error);
  }, [id]);

  async function onDelete() {
    if (!session) return;
    if (!confirm("Delete this workout?")) return;
    await api.deleteGymSession(session.id);
    nav("/gym", { replace: true });
  }

  if (!session) {
    return <p className="text-muted-foreground">Loading…</p>;
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
        <button
          onClick={onDelete}
          className="text-muted-foreground hover:text-destructive mt-4"
          aria-label="Delete"
        >
          <Trash2 className="h-5 w-5" />
        </button>
      </header>

      <h1 className="font-display text-3xl tracking-tight">
        {session.exercises.length} exercise
        {session.exercises.length === 1 ? "" : "s"}
      </h1>
      <p className="text-sm text-muted-foreground mb-5">
        {formatDateTime(session.logged_at_ms)}
      </p>

      <Card>
        <ul className="divide-y divide-border">
          {session.exercises.map((name, i) => (
            <li key={i} className="px-5 py-3 font-medium">
              {name}
            </li>
          ))}
        </ul>
      </Card>

      <div className="mt-6">
        <Button onClick={() => nav("/gym")} className="w-full" variant="outline">
          Done
        </Button>
      </div>
    </>
  );
}
