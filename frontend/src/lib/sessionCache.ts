/**
 * sessionCache — lightweight sessionStorage cache with TTL.
 *
 * Data is stored per browser tab/session.
 * It is automatically cleared when the user:
 *   - refreshes the page (F5 / Ctrl+R)
 *   - closes the tab or browser
 *
 * Within the same session, navigating between pages reuses cached data instantly.
 * Sorting or filtering naturally produces a different cache key, so the backend
 * is called with the new params and the result is stored under the new key.
 */

const PREFIX = "uitmerch:";
const DEFAULT_TTL_MS = 5 * 60 * 1000; // 5 minutes

interface CacheEntry<T> {
  data: T;
  expiresAt: number;
}

function buildKey(key: string): string {
  return PREFIX + key;
}

/** Read a value from cache. Returns null if missing or expired. */
export function cacheGet<T>(key: string): T | null {
  try {
    const raw = sessionStorage.getItem(buildKey(key));
    if (!raw) return null;
    const entry: CacheEntry<T> = JSON.parse(raw);
    if (Date.now() > entry.expiresAt) {
      sessionStorage.removeItem(buildKey(key));
      return null;
    }
    return entry.data;
  } catch {
    return null;
  }
}

/** Write a value to cache with optional TTL (default 5 min). */
export function cacheSet<T>(key: string, data: T, ttlMs = DEFAULT_TTL_MS): void {
  try {
    const entry: CacheEntry<T> = { data, expiresAt: Date.now() + ttlMs };
    sessionStorage.setItem(buildKey(key), JSON.stringify(entry));
  } catch {
    // sessionStorage may be full or disabled — fail silently
  }
}

/** Remove one specific cache key. */
export function cacheDelete(key: string): void {
  try {
    sessionStorage.removeItem(buildKey(key));
  } catch {
    // ignore
  }
}

/** Remove all uitmerch cache entries (full reset within current session). */
export function cacheClear(): void {
  try {
    const toDelete: string[] = [];
    for (let i = 0; i < sessionStorage.length; i++) {
      const k = sessionStorage.key(i);
      if (k?.startsWith(PREFIX)) toDelete.push(k);
    }
    toDelete.forEach((k) => sessionStorage.removeItem(k));
  } catch {
    // ignore
  }
}

/**
 * Build a stable cache key from an object of params.
 * Undefined / null values are omitted so equivalent states share one key.
 *
 * Example:
 *   cacheKey("merch", { page: 1, sort: "name,asc", category: undefined })
 *   → "merch:page=1:sort=name,asc"
 */
export function cacheKey(
  prefix: string,
  params: Record<string, string | number | null | undefined>,
): string {
  const parts = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== "")
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([k, v]) => `${k}=${v}`);
  return parts.length ? `${prefix}:${parts.join(":")}` : prefix;
}
