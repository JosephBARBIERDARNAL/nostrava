import { Bike, Dumbbell, Footprints } from "lucide-react";

export type AppKind = "running" | "biking" | "gym";

export const APPS: Record<AppKind, { label: string; path: string; icon: typeof Bike }> = {
  running: { label: "Running", path: "/running", icon: Footprints },
  biking: { label: "Biking", path: "/biking", icon: Bike },
  gym: { label: "Gym", path: "/gym", icon: Dumbbell },
};
