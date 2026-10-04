#!/usr/bin/env python3
"""Build CSV and exportable charts from the opt-in benchmark JSON (no dependencies for CSV)."""
import argparse
import csv
import json
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("results", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    data = json.loads(args.results.read_text())
    args.output.mkdir(parents=True, exist_ok=True)
    fields = ["method", "path", "samples", "p50_ms", "p95_ms", "p99_ms", "mean_ms",
              "max_ms", "hibernate_statements_mean", "bytes_mean", "statuses", "mode"]
    with (args.output / "api-latency.csv").open("w", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(data["baseline"])
    with (args.output / "api-load.csv").open("w", newline="") as file:
        fields += ["concurrency", "rps", "pool_pending_max", "pool_active_max"]
        writer = csv.DictWriter(file, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(data["load"])
    with (args.output / "api-page-sizes.csv").open("w", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=fields + ["page_size"], extrasaction="ignore")
        writer.writeheader()
        writer.writerows(data["page_sizes"])
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
    except ImportError:
        print("CSV generated. Install matplotlib to generate charts.")
        return
    plt.rcParams.update({"font.size": 10, "axes.spines.top": False, "axes.spines.right": False})
    slowest = sorted(data["baseline"], key=lambda row: row["p95_ms"], reverse=True)[:15][::-1]
    fig, ax = plt.subplots(figsize=(13, 8))
    labels = [row["method"] + " " + row["path"].removeprefix("/api/v1/")
              + (" (AI stub)" if row["path"].endswith("visual-search") else "") for row in slowest]
    ax.barh(labels, [row["p95_ms"] for row in slowest], color="#2962a6")
    ax.set_xlabel("p95 HTTP latency (ms), local PostgreSQL, 15 or 30 measured requests / route")
    ax.set_title("UITMerch: 15 slowest routes in the local baseline")
    for i, row in enumerate(slowest):
        ax.text(row["p95_ms"] + 1, i, f'{row["p95_ms"]:.1f}', va="center")
    fig.tight_layout()
    fig.savefig(args.output / "slowest-apis.png", dpi=180)
    fig.savefig(args.output / "slowest-apis.svg")
    plt.close(fig)
    fig, axes = plt.subplots(1, 2, figsize=(14, 6))
    for path in dict.fromkeys(row["path"] for row in data["load"]):
        rows = [row for row in data["load"] if row["path"] == path]
        label = path.removeprefix("/api/v1/")
        axes[0].plot([row["concurrency"] for row in rows], [row["p95_ms"] for row in rows], marker="o", label=label)
        axes[1].plot([row["concurrency"] for row in rows], [row["rps"] for row in rows], marker="o", label=label)
    axes[0].set_ylabel("p95 HTTP latency (ms)")
    axes[1].set_ylabel("Completed requests / second")
    for ax in axes:
        ax.set_xlabel("Concurrent clients (closed loop, 200 requests / case)")
        ax.set_xticks([1, 5, 10, 20])
        ax.grid(alpha=.2)
    axes[0].legend(fontsize=8)
    fig.suptitle("UITMerch local load sweep: Hikari pool = 5")
    fig.tight_layout()
    fig.savefig(args.output / "load-sweep.png", dpi=180)
    fig.savefig(args.output / "load-sweep.svg")
    plt.close(fig)


if __name__ == "__main__":
    main()
