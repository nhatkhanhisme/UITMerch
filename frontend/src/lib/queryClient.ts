import { cacheClear } from "./sessionCache";
import { QueryClient } from "@tanstack/react-query";
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 15000, retry: false, refetchOnWindowFocus: true },
    mutations: { retry: false },
  },
});
export function resetServerState() {
  void queryClient.cancelQueries();
  queryClient.clear();
}
export function invalidateFeatures(
  domains: string[] = ["orders", "catalog", "campaigns"],
) {
  const keys = new Set<string>();
  if (domains.includes("orders"))
    ["order-history", "campaign-order", "analytics"].forEach((k) =>
      keys.add(k),
    );
  if (domains.includes("campaigns"))
    [
      "campaigns",
      "campaign",
      "reservations",
      "campaign-order",
      "purchase-context",
      "campaign-merch",
      "campaign-owned",
    ].forEach((k) => keys.add(k));
  if (domains.includes("catalog")) cacheClear();
  if (domains.includes("catalog"))
    ["purchase-context", "campaign-merch", "restock", "campaign"].forEach((k) =>
      keys.add(k),
    );
  void queryClient.invalidateQueries({
    predicate: (query) => keys.has(String(query.queryKey[0])),
  });
  if (domains.includes("orders")) {
    window.dispatchEvent(new CustomEvent("order-status-changed"));
    window.dispatchEvent(
      new CustomEvent("org-order-event", { detail: { type: "UPDATED" } }),
    );
  }
}
