import { useState } from "react";
import { getStats, isUsingDemoMode, recordClick, ShortenerError } from "../api/client";
import { SkeletonCard, StatsCard } from "../components/StatsCard";
import type { StatsResponse } from "../api/types";

const CODE_PATTERN = /^[0-9A-Za-z]{1,12}$/;

export function Stats() {
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const lookup = async (e: React.FormEvent) => {
    e.preventDefault();
    const c = code.trim();
    if (!c) { setError("Enter a short code"); return; }
    if (!CODE_PATTERN.test(c)) {
      setError("Codes are 1-12 letters or digits.");
      return;
    }
    setError(null);
    setStats(null);
    setLoading(true);
    try {
      const s = await getStats(c);
      setStats(s);
    } catch (err) {
      if (err instanceof ShortenerError && err.status === 404) {
        setError(`No link found for "${c}".`);
      } else if (err instanceof ShortenerError) {
        setError(err.message);
      } else {
        setError("Couldn't reach the server.");
      }
    } finally {
      setLoading(false);
    }
  };

  const simulateClick = async () => {
    if (!stats) return;
    await recordClick(stats.code);
    // Re-fetch so the counter updates
    try { setStats(await getStats(stats.code)); } catch { /* ignore */ }
  };

  return (
    <section className="fade-up mx-auto w-full max-w-2xl px-5 pt-14 pb-10">
      <div className="text-center">
        <h2 className="text-3xl font-semibold tracking-tight sm:text-4xl">Link stats</h2>
        <p className="mx-auto mt-3 max-w-md text-base" style={{ color: "var(--muted)" }}>
          Paste a short code to see click counts and creation details.
        </p>
      </div>

      <form onSubmit={lookup} className="mt-10">
        <label
          className="block rounded-xl p-1.5"
          style={{ background: "var(--card)", border: "1px solid var(--border)" }}
        >
          <div className="flex items-stretch gap-2">
            <span className="grid shrink-0 place-items-center pl-3 font-mono text-sm" style={{ color: "var(--muted)" }}>
              /
            </span>
            <input
              type="text"
              value={code}
              placeholder="abc1234"
              onChange={(e) => setCode(e.target.value)}
              className="min-w-0 flex-1 bg-transparent py-3 font-mono text-base outline-none"
              style={{ color: "var(--fg)" }}
              autoComplete="off"
              spellCheck={false}
            />
            <button
              type="submit"
              disabled={loading}
              className="focus-ring shrink-0 rounded-lg px-4 py-2.5 text-sm font-semibold transition-opacity disabled:opacity-60"
              style={{ background: "var(--accent)", color: "var(--accent-fg)" }}
            >
              {loading ? "Looking up…" : "Lookup"}
            </button>
          </div>
        </label>

        {error && (
          <p className="mt-2 pl-2 text-sm" style={{ color: "var(--danger)" }}>{error}</p>
        )}
      </form>

      {loading && <SkeletonCard />}
      {stats && !loading && (
        <>
          <StatsCard stats={stats} />
          <div className="fade-up mt-4 flex justify-end gap-2">
            <button
              onClick={simulateClick}
              className="focus-ring rounded-lg px-3 py-1.5 text-xs font-medium"
              style={{ background: "color-mix(in oklab, var(--fg) 6%, transparent)", color: "var(--fg)" }}
            >
              Simulate click
            </button>
            <a
              href={isUsingDemoMode() ? `/?code=${encodeURIComponent(stats.code)}` : `/${encodeURIComponent(stats.code)}`}
              target="_blank"
              rel="noreferrer"
              className="focus-ring inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-medium"
              style={{ background: "var(--accent)", color: "var(--accent-fg)" }}
            >
              Visit link
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                   strokeLinecap="round" strokeLinejoin="round">
                <path d="M7 17L17 7M7 7h10v10" />
              </svg>
            </a>
          </div>
        </>
      )}

      {!loading && !stats && !error && (
        <EmptyState />
      )}
    </section>
  );
}

function EmptyState() {
  return (
    <div
      className="mt-10 rounded-xl p-8 text-center"
      style={{ background: "var(--card)", border: "1px dashed var(--border)" }}
    >
      <div
        className="mx-auto grid h-12 w-12 place-items-center rounded-full"
        style={{ background: "color-mix(in oklab, var(--fg) 6%, transparent)" }}
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
             strokeLinecap="round" strokeLinejoin="round" style={{ color: "var(--muted)" }}>
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.3-4.3" />
        </svg>
      </div>
      <p className="mt-4 text-sm font-medium">No link selected yet</p>
      <p className="mt-1 text-xs" style={{ color: "var(--muted)" }}>
        Shorten a URL on the previous tab, then come back here to see its stats.
      </p>
    </div>
  );
}
