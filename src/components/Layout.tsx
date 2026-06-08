import { useState } from "react";

type Tab = "home" | "stats";

interface HeaderProps {
  tab: Tab;
  onChange: (t: Tab) => void;
  demoMode?: boolean;
}

export function Header({ tab, onChange, demoMode }: HeaderProps) {
  return (
    <header className="w-full border-b" style={{ borderColor: "var(--border)" }}>
      <div className="mx-auto flex max-w-4xl items-center justify-between px-5 py-4">
        <button
          onClick={() => onChange("home")}
          className="flex items-center gap-2 text-left focus-ring rounded-md -ml-2 px-2 py-1"
          aria-label="Go to home"
        >
          <span className="grid h-8 w-8 place-items-center rounded-md"
                style={{ background: "var(--accent)", color: "var(--accent-fg)" }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"
                 strokeLinecap="round" strokeLinejoin="round">
              <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
              <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
            </svg>
          </span>
          <span className="font-semibold tracking-tight text-lg">Sniplink</span>
        </button>

        <nav className="flex items-center gap-1 rounded-full p-1"
             style={{ background: "var(--card)", border: "1px solid var(--border)" }}
             role="tablist">
          <TabButton active={tab === "home"} onClick={() => onChange("home")}>Shorten</TabButton>
          <TabButton active={tab === "stats"} onClick={() => onChange("stats")}>Stats</TabButton>
        </nav>
      </div>
      {demoMode && (
        <div className="mx-auto max-w-4xl px-5 pb-3">
          <div className="inline-flex items-center gap-2 rounded-md px-2.5 py-1 text-xs"
               style={{ background: "color-mix(in oklab, var(--warning) 15%, transparent)", color: "var(--warning)" }}>
            <span className="dot h-1.5 w-1.5 rounded-full bg-current" />
            Demo mode — backend not reachable. Data is stored locally in your browser.
          </div>
        </div>
      )}
    </header>
  );
}

function TabButton({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  const [hover, setHover] = useState(false);
  return (
    <button
      role="tab"
      aria-selected={active}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      onClick={onClick}
      className="rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors focus-ring"
      style={{
        background: active ? "var(--accent)" : hover ? "color-mix(in oklab, var(--fg) 8%, transparent)" : "transparent",
        color: active ? "var(--accent-fg)" : "var(--fg)",
      }}
    >
      {children}
    </button>
  );
}

export function Footer() {
  return (
    <footer className="mt-auto border-t" style={{ borderColor: "var(--border)" }}>
      <div className="mx-auto flex max-w-4xl flex-col items-center gap-2 px-5 py-6 text-center text-sm"
           style={{ color: "var(--muted)" }}>
        <p>
          Built with Spring Boot 3 · React 19 · local ONNX risk scoring.
        </p>
        <p className="text-xs">
          No external AI APIs · No Redis · Direct Postgres reads.
        </p>
      </div>
    </footer>
  );
}
