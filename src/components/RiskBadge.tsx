import { riskLabel } from "../utils/format";

export function RiskBadge({ score }: { score: number }) {
  const { label, tone } = riskLabel(score);
  const palette = {
    safe:    { bg: "color-mix(in oklab, var(--success) 14%, transparent)", fg: "var(--success)" },
    caution: { bg: "color-mix(in oklab, var(--warning) 14%, transparent)", fg: "var(--warning)" },
    danger:  { bg: "color-mix(in oklab, var(--danger) 14%, transparent)",  fg: "var(--danger)" },
  }[tone];
  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium"
      style={{ background: palette.bg, color: palette.fg }}
      title={`AI risk score: ${score.toFixed(2)} / 1.00`}
    >
      <span className="h-1.5 w-1.5 rounded-full" style={{ background: palette.fg }} />
      {label} · {score.toFixed(2)}
    </span>
  );
}
