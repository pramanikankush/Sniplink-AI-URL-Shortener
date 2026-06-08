import type { StatsResponse } from "../api/types";
import { formatDate, formatNumber } from "../utils/format";

export function SkeletonCard() {
  return (
    <div className="mt-6 rounded-xl p-5" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
      <div className="skeleton h-4 w-32 mb-4" />
      <div className="skeleton h-10 w-full mb-3" />
      <div className="flex gap-3">
        <div className="skeleton h-16 flex-1" />
        <div className="skeleton h-16 flex-1" />
        <div className="skeleton h-16 flex-1" />
      </div>
    </div>
  );
}

export function StatsCard({ stats }: { stats: StatsResponse }) {
  return (
    <div
      className="card-hover fade-up mt-6 rounded-xl p-5"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <p className="mb-3 text-xs uppercase tracking-wider" style={{ color: "var(--muted)" }}>
        Link details
      </p>

      <a
        href={stats.longUrl}
        target="_blank"
        rel="noreferrer"
        className="block truncate font-medium hover:underline"
        title={stats.longUrl}
      >
        {stats.longUrl}
      </a>

      <div className="mt-4 grid grid-cols-3 gap-3">
        <Stat label="Code" value={stats.code} mono />
        <Stat label="Clicks" value={formatNumber(stats.clicks)} />
        <Stat label="Created" value={formatDate(stats.createdAt)} />
      </div>
    </div>
  );
}

function Stat({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="rounded-lg p-3" style={{ background: "color-mix(in oklab, var(--fg) 4%, transparent)" }}>
      <p className="text-[10px] uppercase tracking-wider" style={{ color: "var(--muted)" }}>{label}</p>
      <p className={`mt-1 truncate text-sm font-semibold ${mono ? "font-mono" : ""}`} title={value}>{value}</p>
    </div>
  );
}
