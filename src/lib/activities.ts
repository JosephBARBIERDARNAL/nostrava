export type ActivityKind = "running" | "biking";

export interface ActivityConfig {
  title: string;
  verb: string;
  noun: string;
  nounPlural: string;
  trackingLabel: string;
  metric: "pace" | "speed";
}

export const ACTIVITY_CONFIG: Record<ActivityKind, ActivityConfig> = {
  running: {
    title: "Running",
    verb: "Start a run",
    noun: "run",
    nounPlural: "Runs",
    trackingLabel: "Running",
    metric: "pace",
  },
  biking: {
    title: "Biking",
    verb: "Start a ride",
    noun: "ride",
    nounPlural: "Rides",
    trackingLabel: "Riding",
    metric: "speed",
  },
};
