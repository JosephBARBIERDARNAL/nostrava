import { useNavigate } from "react-router-dom";
import { APPS, type AppKind } from "@/lib/apps";
import { Button } from "@/components/ui/button";

export function AppSelector() {
  const nav = useNavigate();

  return (
    <>
      <header className="mb-10">
        <h1 className="font-display text-4xl tracking-tight">didit</h1>
      </header>

      <div className="flex-1 flex flex-col justify-center gap-4">
        {(Object.keys(APPS) as AppKind[]).map((key) => {
          const app = APPS[key];
          const Icon = app.icon;
          return (
            <Button
              key={key}
              onClick={() => nav(app.path)}
              size="xl"
              variant={
                key === "running"
                  ? "primary"
                  : key === "biking"
                    ? "secondary"
                    : "outline"
              }
              className="w-full font-display text-xl"
            >
              <Icon className="h-6 w-6" /> {app.label}
            </Button>
          );
        })}
      </div>
    </>
  );
}
