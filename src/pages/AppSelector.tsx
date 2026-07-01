import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { APPS, type AppKind } from "@/lib/apps";
import { api } from "@/lib/api";
import { formatInstallDate } from "@/lib/format";
import { Button } from "@/components/ui/button";

export function AppSelector() {
  const nav = useNavigate();
  const [installationInstalledAtMs, setInstallationInstalledAtMs] = useState<
    number | null
  >(null);

  useEffect(() => {
    api
      .installationInstalledAtMs()
      .then(setInstallationInstalledAtMs)
      .catch(console.error);
  }, []);

  return (
    <>
      <header className="mb-10 mt-4">
        <h1 className="font-display text-4xl tracking-tight">didit</h1>
        {installationInstalledAtMs && (
          <p className="mt-1 text-sm text-muted-foreground">
            Installed {formatInstallDate(installationInstalledAtMs)}
          </p>
        )}
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
