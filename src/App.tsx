import { Component, useEffect, useState, type ReactNode } from "react";
import { Footer, Header } from "./components/Layout";
import { Home } from "./pages/Home";
import { Stats } from "./pages/Stats";
import { isBackendUp, isUsingDemoMode, resetBackendProbe } from "./api/client";

type Tab = "home" | "stats";

/** Hash-based routing — works when served as a single HTML file. */
function useHashRoute(): [Tab, (t: Tab) => void] {
  const read = (): Tab => {
    const h = window.location.hash.replace(/^#\/?/, "").toLowerCase();
    return h === "stats" ? "stats" : "home";
  };
  const [tab, setTab] = useState<Tab>(read);
  useEffect(() => {
    const onHash = () => setTab(read());
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);
  const set = (t: Tab) => {
    window.location.hash = t === "home" ? "/" : "/stats";
    setTab(t);
  };
  return [tab, set];
}

class ErrorBoundary extends Component<{ children: ReactNode }, { error: Error | null }> {
  state = { error: null as Error | null };
  static getDerivedStateFromError(error: Error) { return { error }; }
  componentDidCatch(error: Error, info: { componentStack?: string }) {
    // eslint-disable-next-line no-console
    console.error("UI error boundary caught:", error, info);
  }
  render() {
    if (this.state.error) {
      return (
        <div className="mx-auto max-w-xl px-5 py-16 text-center">
          <h2 className="text-2xl font-semibold">Something went wrong.</h2>
          <p className="mt-3 text-sm" style={{ color: "var(--muted)" }}>
            {this.state.error.message || "An unexpected error occurred."}
          </p>
          <button
            onClick={() => { this.setState({ error: null }); window.location.reload(); }}
            className="focus-ring mt-6 rounded-md px-4 py-2 text-sm font-medium"
            style={{ background: "var(--accent)", color: "var(--accent-fg)" }}
          >
            Reload
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

export default function App() {
  const [tab, setTab] = useHashRoute();
  const [demo, setDemo] = useState<boolean>(false);

  useEffect(() => {
    let cancelled = false;
    resetBackendProbe();
    isBackendUp().then(() => {
      if (!cancelled) setDemo(isUsingDemoMode());
    });
    return () => { cancelled = true; };
  }, [tab]);

  return (
    <ErrorBoundary>
      <div className="flex min-h-full flex-col">
        <Header tab={tab} onChange={setTab} demoMode={demo} />
        <main className="flex-1">
          {tab === "home" ? <Home /> : <Stats />}
        </main>
        <Footer />
      </div>
    </ErrorBoundary>
  );
}
