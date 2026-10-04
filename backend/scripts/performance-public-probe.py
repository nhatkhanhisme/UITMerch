#!/usr/bin/env python3
"""Low-volume, read-only probe of public API routes on a running backend."""
import argparse
import http.client
import json
import math
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlsplit


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--samples", type=int, default=20)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    if not 1 <= args.samples <= 100:
        parser.error("Use 1–100 samples for this low-volume public probe.")
    base = urlsplit(args.base_url)
    cls = http.client.HTTPSConnection if base.scheme == "https" else http.client.HTTPConnection
    connection = cls(base.hostname, base.port, timeout=30)

    def get(path):
        start = time.perf_counter_ns()
        connection.request("GET", base.path.rstrip("/") + path, headers={"Accept": "application/json"})
        response = connection.getresponse()
        payload = response.read()
        return (time.perf_counter_ns() - start) / 1e6, response.status, payload

    paths = ["/", "/api/v1/categories", "/api/v1/public/merch?size=20",
             "/api/v1/public/merch?keyword=UIT&size=20", "/api/v1/public/merch/popular",
             "/api/v1/public/organizations?size=20", "/api/v1/public/events?size=20",
             "/api/v1/public/campaigns?size=20"]
    # Discover real IDs only from public list responses; perform no writes.
    for list_path, detail_prefix in [("/api/v1/public/merch?size=1", "/api/v1/public/merch/"),
                                     ("/api/v1/public/organizations?size=1", "/api/v1/public/organizations/"),
                                     ("/api/v1/public/events?size=1", "/api/v1/public/events/")]:
        _, status, payload = get(list_path)
        if status == 200:
            rows = json.loads(payload).get("data", [])
            if isinstance(rows, list) and rows:
                paths.append(detail_prefix + rows[0]["id"])
    result = {"base_url": args.base_url, "measured_at": datetime.now(timezone.utc).isoformat(),
              "warmups": 3, "concurrency": 1, "samples_per_route": args.samples,
              "http": "HTTP/1.1 keep-alive; no Authorization header", "routes": []}
    try:
        for path in paths:
            samples = []
            statuses = {}
            sizes = []
            for i in range(args.samples + 3):
                ms, status, payload = get(path)
                if i >= 3:
                    samples.append(ms)
                    sizes.append(len(payload))
                    statuses[str(status)] = statuses.get(str(status), 0) + 1
            sorted_samples = sorted(samples)
            entry = {"method": "GET", "path": path, "samples": len(samples), "statuses": statuses,
                     "p50_ms": sorted_samples[math.ceil(len(samples) * .5) - 1],
                     "p95_ms": sorted_samples[math.ceil(len(samples) * .95) - 1],
                     "p99_ms": sorted_samples[math.ceil(len(samples) * .99) - 1],
                     "latencies_ms": samples, "bytes_mean": sum(sizes) / len(sizes)}
            result["routes"].append(entry)
            print(f'{path}: p50={entry["p50_ms"]:.1f}ms p95={entry["p95_ms"]:.1f}ms {statuses}', flush=True)
    finally:
        connection.close()
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, indent=2) + "\n")


if __name__ == "__main__":
    main()
