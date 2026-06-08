export function isValidUrl(value: string): { ok: boolean; reason?: string } {
  const trimmed = value.trim();
  if (!trimmed) return { ok: false, reason: "Enter a URL to shorten" };
  if (!/^https?:\/\//i.test(trimmed)) {
    return { ok: false, reason: "URL must start with http:// or https://" };
  }
  try {
    const u = new URL(trimmed);
    if (!u.hostname.includes(".")) {
      return { ok: false, reason: "That doesn't look like a valid host" };
    }
    return { ok: true };
  } catch {
    return { ok: false, reason: "That doesn't look like a valid URL" };
  }
}

export function formatDate(iso: string): string {
  try {
    const d = new Date(iso);
    return d.toLocaleString(undefined, {
      year: "numeric", month: "short", day: "2-digit",
      hour: "2-digit", minute: "2-digit",
    });
  } catch { return iso; }
}

export function formatNumber(n: number): string {
  return new Intl.NumberFormat().format(n);
}

export function riskLabel(score: number): { label: string; tone: "safe" | "caution" | "danger" } {
  if (score < 0.25) return { label: "Safe",        tone: "safe" };
  if (score < 0.6)  return { label: "Caution",     tone: "caution" };
  return              { label: "Risky",        tone: "danger" };
}
