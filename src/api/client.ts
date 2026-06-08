import type { ApiError, ShortenResponse, StatsResponse } from "./types";

/**
 * Typed HTTP client for the shortener backend.
 *
 * If `VITE_API_BASE` is empty or the backend is unreachable, calls fall back
 * to an in-memory demo store so the UI is fully usable without Docker.
 */

const RAW_API_BASE = (import.meta.env.VITE_API_BASE as string | undefined) ?? "";
const API_BASE = RAW_API_BASE.replace(/\/$/, "") || "http://localhost:8080";

export class ShortenerError extends Error {
  constructor(message: string, public status: number) {
    super(message);
    this.name = "ShortenerError";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(API_BASE + path, {
    headers: { "Content-Type": "application/json" },
    ...init,
  });
  if (!res.ok) {
    let err: ApiError = { error: "Request failed", status: res.status };
    try { err = (await res.json()) as ApiError; } catch { /* ignore */ }
    const fallback = err.error || res.statusText || `HTTP ${res.status}`;
    throw new ShortenerError(fallback, err.status || res.status);
  }
  return (await res.json()) as T;
}

/* ---------- Demo fallback ---------- */

const DEMO_KEY = "sniplink:demo:urls";
type DemoEntry = { code: string; longUrl: string; clicks: number; createdAt: string; riskScore: number };

function readDemo(): Record<string, DemoEntry> {
  try { return JSON.parse(localStorage.getItem(DEMO_KEY) || "{}"); } catch { return {}; }
}
function writeDemo(m: Record<string, DemoEntry>) {
  localStorage.setItem(DEMO_KEY, JSON.stringify(m));
}
function makeCode(n = 7) {
  const a = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
  const buf = new Uint8Array(n);
  if (typeof crypto !== "undefined" && crypto.getRandomValues) {
    crypto.getRandomValues(buf);
  } else {
    for (let i = 0; i < n; i++) buf[i] = Math.floor(Math.random() * 256);
  }
  let s = "";
  for (let i = 0; i < n; i++) s += a[buf[i] % a.length];
  return s;
}
function demoRisk(url: string): number {
  let s = 0;
  if (/^https?:\/\/\d{1,3}(\.\d{1,3}){3}/.test(url)) s += 0.25;
  if (/\.(xyz|top|buzz|click|gq|ml|tk)\b/i.test(url)) s += 0.25;
  if (/(paypa1|g00gle|arnazon|micros0ft)/i.test(url)) s += 0.35;
  if (url.length > 200) s += 0.1;
  return Math.min(1, Math.round(s * 100) / 100);
}

/* ---------- Probe ---------- */

let backendReachable: boolean | null = null;

export async function isBackendUp(): Promise<boolean> {
  if (backendReachable !== null) return backendReachable;
  try {
    const ctrl = new AbortController();
    const t = setTimeout(() => ctrl.abort(), 1500);
    const r = await fetch(API_BASE + "/actuator/health", { signal: ctrl.signal });
    clearTimeout(t);
    backendReachable = r.ok;
  } catch {
    backendReachable = false;
  }
  return backendReachable;
}

export function isUsingDemoMode(): boolean {
  return backendReachable === false;
}

export function resetBackendProbe() {
  backendReachable = null;
}

/* ---------- Demo URL helpers ---------- */

/**
 * In demo mode there is no backend to redirect the short link, so the link
 * points to a {@code ?code=…} query the user can paste into the Stats tab.
 */
function demoShortUrl(code: string): string {
  return `${window.location.origin}/?code=${encodeURIComponent(code)}`;
}

/* ---------- Public API ---------- */

export async function shorten(url: string): Promise<ShortenResponse> {
  if (!(await isBackendUp())) {
    const store = readDemo();
    const existing = Object.values(store).find(e => e.longUrl === url);
    if (existing) {
      return {
        code: existing.code,
        shortUrl: demoShortUrl(existing.code),
        riskScore: existing.riskScore,
      };
    }
    const code = makeCode();
    const riskScore = demoRisk(url);
    store[code] = { code, longUrl: url, clicks: 0, createdAt: new Date().toISOString(), riskScore };
    writeDemo(store);
    return { code, shortUrl: demoShortUrl(code), riskScore };
  }
  return request<ShortenResponse>("/api/v1/shorten", {
    method: "POST",
    body: JSON.stringify({ url }),
  });
}

export async function getStats(code: string): Promise<StatsResponse> {
  if (!(await isBackendUp())) {
    const store = readDemo();
    const e = store[code];
    if (!e) throw new ShortenerError("Short link not found", 404);
    return { code: e.code, longUrl: e.longUrl, clicks: e.clicks, createdAt: e.createdAt };
  }
  return request<StatsResponse>(`/api/v1/stats/${encodeURIComponent(code)}`);
}

/** Simulate a click — only meaningful against a real backend. */
export async function recordClick(code: string): Promise<void> {
  if (!(await isBackendUp())) {
    const store = readDemo();
    if (store[code]) {
      store[code].clicks++;
      writeDemo(store);
    }
    return;
  }
  await fetch(`${API_BASE}/${encodeURIComponent(code)}`, { method: "GET", redirect: "manual" }).catch(() => {});
}
