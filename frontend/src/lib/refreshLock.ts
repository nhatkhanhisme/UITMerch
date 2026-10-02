/** Serialize refresh rotation across tabs, including browsers without Web Locks. */
export async function withRefreshLock<T>(work: () => Promise<T>): Promise<T> {
  if (typeof navigator !== "undefined" && navigator.locks) {
    return navigator.locks.request("uitmerch-refresh", work);
  }
  if (typeof localStorage === "undefined") return work();
  const key = "uitmerch-refresh-lease";
  const owner = crypto.randomUUID();
  const started = Date.now();
  while (Date.now() - started < 30000) {
    let acquired = false;
    try {
      const held = JSON.parse(localStorage.getItem(key) ?? "null") as {
        owner: string;
        expires: number;
      } | null;
      if (!held || held.expires < Date.now()) {
        localStorage.setItem(
          key,
          JSON.stringify({ owner, expires: Date.now() + 20000 }),
        );
        await new Promise((resolve) => setTimeout(resolve, 30));
        acquired =
          JSON.parse(localStorage.getItem(key) ?? "null")?.owner === owner;
      }
    } catch {
      return work();
    }
    if (acquired) {
      try {
        return await work();
      } finally {
        try {
          if (JSON.parse(localStorage.getItem(key) ?? "null")?.owner === owner)
            localStorage.removeItem(key);
        } catch {
          /* Storage can become unavailable while refreshing. */
        }
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error("Đang đồng bộ phiên đăng nhập. Vui lòng thử lại.");
}
