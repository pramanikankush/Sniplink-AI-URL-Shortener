import { useState } from "react";
import { RiskBadge } from "./RiskBadge";
import type { ShortenResponse } from "../api/types";

export function ResultCard({ result }: { result: ShortenResponse }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(result.shortUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 1600);
    } catch {
      // Fallback: select the input content
      const input = document.getElementById("short-url-display") as HTMLInputElement | null;
      input?.select();
    }
  };

  return (
    <div
      className="card-hover fade-up mt-6 overflow-hidden rounded-xl p-5"
      style={{ background: "var(--card)", border: "1px solid var(--border)" }}
    >
      <div className="mb-3 flex items-center justify-between gap-3">
        <p className="text-xs uppercase tracking-wider" style={{ color: "var(--muted)" }}>
          Your short link
        </p>
        <RiskBadge score={result.riskScore} />
      </div>

      <div
        className="flex items-stretch gap-2 rounded-lg p-1.5"
        style={{ background: "color-mix(in oklab, var(--fg) 4%, transparent)" }}
      >
        <input
          id="short-url-display"
          readOnly
          value={result.shortUrl}
          onClick={(e) => (e.target as HTMLInputElement).select()}
          className="min-w-0 flex-1 bg-transparent px-2 py-1.5 font-mono text-sm focus-ring rounded"
          style={{ color: "var(--fg)" }}
        />
        <button
          onClick={copy}
          className="focus-ring shrink-0 rounded-md px-3 py-1.5 text-sm font-medium transition-colors"
          style={{ background: "var(--accent)", color: "var(--accent-fg)" }}
        >
          {copied ? "Copied!" : "Copy"}
        </button>
      </div>

      <div className="mt-3 flex items-center justify-between text-xs" style={{ color: "var(--muted)" }}>
        <span className="truncate pr-4">
          Code: <span className="font-mono" style={{ color: "var(--fg)" }}>{result.code}</span>
        </span>
        <a
          href={result.shortUrl}
          target="_blank"
          rel="noreferrer"
          className="focus-ring inline-flex items-center gap-1 rounded-md px-2 py-1 hover:underline"
        >
          Open link
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
               strokeLinecap="round" strokeLinejoin="round">
            <path d="M7 17L17 7M7 7h10v10" />
          </svg>
        </a>
      </div>
    </div>
  );
}
