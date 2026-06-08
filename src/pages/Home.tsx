import { useState } from "react";
import { shorten, ShortenerError } from "../api/client";
import { ResultCard } from "../components/ResultCard";
import { isValidUrl } from "../utils/format";
import type { ShortenResponse } from "../api/types";

export function Home() {
  const [url, setUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ShortenResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [touched, setTouched] = useState(false);

  const validation = isValidUrl(url);
  const showError = touched && !validation.ok;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setTouched(true);
    setError(null);
    if (!validation.ok) return;

    setLoading(true);
    try {
      const r = await shorten(url.trim());
      setResult(r);
    } catch (err) {
      if (err instanceof ShortenerError) {
        if (err.status === 422) setError("This URL was flagged as unsafe by the AI risk model.");
        else if (err.status === 429) setError("Slow down — you've hit the rate limit. Try again in a minute.");
        else if (err.status >= 500) setError("The server hit an unexpected error. Please try again.");
        else setError(err.message || "Something went wrong.");
      } else {
        setError("Network error. Check your connection and try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="fade-up mx-auto w-full max-w-2xl px-5 pt-14 pb-10">
      <div className="text-center">
        <span
          className="inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium"
          style={{ background: "color-mix(in oklab, var(--fg) 6%, transparent)", color: "var(--fg)" }}
        >
          <span className="h-1.5 w-1.5 rounded-full" style={{ background: "var(--success)" }} />
          Local AI · Zero paid APIs
        </span>
        <h1 className="mt-5 text-4xl font-semibold tracking-tight sm:text-5xl">
          Shorten links.<br />Know the risk.
        </h1>
        <p className="mx-auto mt-4 max-w-lg text-base" style={{ color: "var(--muted)" }}>
          Paste a URL, get a short link, and see an on-device AI risk score before you share it.
        </p>
      </div>

      <form onSubmit={submit} className="mt-10">
        <label
          className="block rounded-xl p-1.5 transition-colors"
          style={{
            background: "var(--card)",
            border: `1px solid ${showError ? "var(--danger)" : "var(--border)"}`,
            boxShadow: showError ? "0 0 0 3px color-mix(in oklab, var(--danger) 18%, transparent)" : "none",
          }}
        >
          <div className="flex items-stretch gap-2">
            <span className="grid shrink-0 place-items-center pl-3" style={{ color: "var(--muted)" }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                   strokeLinecap="round" strokeLinejoin="round">
                <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
              </svg>
            </span>
            <input
              type="text"
              value={url}
              placeholder="https://example.com/your-long-url"
              onChange={(e) => { setUrl(e.target.value); setError(null); }}
              onBlur={() => setTouched(true)}
              className="min-w-0 flex-1 bg-transparent py-3 text-base outline-none"
              style={{ color: "var(--fg)" }}
              autoComplete="off"
              spellCheck={false}
              inputMode="url"
            />
            <button
              type="submit"
              disabled={loading}
              className="focus-ring shrink-0 rounded-lg px-4 py-2.5 text-sm font-semibold transition-opacity disabled:opacity-60"
              style={{ background: "var(--accent)", color: "var(--accent-fg)" }}
            >
              {loading ? (
                <span className="inline-flex items-center gap-2">
                  <span className="dot h-1.5 w-1.5 rounded-full bg-current" style={{ animationDelay: "0s" }} />
                  <span className="dot h-1.5 w-1.5 rounded-full bg-current" style={{ animationDelay: "0.15s" }} />
                  <span className="dot h-1.5 w-1.5 rounded-full bg-current" style={{ animationDelay: "0.3s" }} />
                </span>
              ) : (
                "Shorten URL"
              )}
            </button>
          </div>
        </label>

        {showError && (
          <p className="mt-2 pl-2 text-sm" style={{ color: "var(--danger)" }}>
            {validation.reason}
          </p>
        )}
        {error && (
          <p className="mt-2 pl-2 text-sm" style={{ color: "var(--danger)" }}>
            {error}
          </p>
        )}
      </form>

      {result && <ResultCard result={result} />}

      {/* Helper row */}
      <div className="mt-10 grid grid-cols-1 gap-3 sm:grid-cols-3">
        <Feature title="Base62 codes" desc="7-char, collision-resistant, URL-safe." />
        <Feature title="Per-IP rate limit" desc="Token-bucket (Bucket4j) at the gateway." />
        <Feature title="ONNX risk model" desc="Fully local inference — no OpenAI/Gemini." />
      </div>
    </section>
  );
}

function Feature({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="rounded-xl p-4" style={{ background: "var(--card)", border: "1px solid var(--border)" }}>
      <p className="text-sm font-semibold">{title}</p>
      <p className="mt-1 text-xs leading-relaxed" style={{ color: "var(--muted)" }}>{desc}</p>
    </div>
  );
}
