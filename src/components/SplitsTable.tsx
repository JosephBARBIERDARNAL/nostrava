import type { Split } from "@/lib/api";
import { formatDuration } from "@/lib/format";

interface Props {
  splits: Split[];
}

export function SplitsTable({ splits }: Props) {
  if (splits.length === 0) {
    return (
      <p className="text-sm text-muted-foreground">No full kilometres in this run.</p>
    );
  }

  const fastest = Math.min(...splits.map((s) => s.duration_ms));

  return (
    <div className="overflow-hidden rounded-xl border border-border">
      <table className="w-full text-sm">
        <thead className="bg-muted/40 text-xs uppercase tracking-wide text-muted-foreground">
          <tr>
            <th className="px-4 py-2 text-left">Km</th>
            <th className="px-4 py-2 text-right">Time</th>
            <th className="px-4 py-2 text-right">↑ m</th>
          </tr>
        </thead>
        <tbody>
          {splits.map((s) => {
            const isFastest = s.duration_ms === fastest;
            return (
              <tr key={s.km_index} className="border-t border-border">
                <td className="px-4 py-2 font-medium">{s.km_index}</td>
                <td className={`px-4 py-2 text-right tabular-nums ${isFastest ? "text-brand font-semibold" : ""}`}>
                  {formatDuration(s.duration_ms)}
                </td>
                <td className="px-4 py-2 text-right text-muted-foreground">
                  {s.elevation_gain_m ? s.elevation_gain_m.toFixed(0) : "—"}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
