import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ArrowLeft, Check } from "lucide-react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { DEFAULT_EXERCISES } from "@/lib/gymExercises";
import { cn } from "@/lib/cn";

export function GymLog() {
  const nav = useNavigate();
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [saving, setSaving] = useState(false);

  function toggle(name: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(name)) next.delete(name);
      else next.add(name);
      return next;
    });
  }

  async function onSave() {
    if (saving || selected.size === 0) return;
    setSaving(true);
    try {
      await api.createGymSession(Array.from(selected));
      nav("/gym", { replace: true });
    } catch (e) {
      console.error(e);
      setSaving(false);
    }
  }

  return (
    <div className="flex-1 flex flex-col">
      <header className="flex items-center justify-between mb-5">
        <Link
          to="/gym"
          className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="h-4 w-4 mr-1" /> Cancel
        </Link>
        <h1 className="font-display text-lg">Log Workout</h1>
        <span className="w-16" />
      </header>

      <ul className="space-y-2 mb-6">
        {DEFAULT_EXERCISES.map((name) => {
          const active = selected.has(name);
          return (
            <li key={name}>
              <button
                onClick={() => toggle(name)}
                className={cn(
                  "flex w-full items-center justify-between rounded-xl border px-4 py-3 text-left transition-colors",
                  active
                    ? "border-brand bg-brand/10 text-foreground"
                    : "border-border bg-card text-foreground hover:border-muted",
                )}
              >
                <span className="font-medium">{name}</span>
                {active && <Check className="h-5 w-5 text-brand" />}
              </button>
            </li>
          );
        })}
      </ul>

      <div className="mt-auto">
        <Button
          onClick={onSave}
          disabled={saving || selected.size === 0}
          size="xl"
          className="w-full font-display text-xl"
        >
          Save {selected.size > 0 ? `(${selected.size})` : ""}
        </Button>
      </div>
    </div>
  );
}
