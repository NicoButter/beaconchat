#!/usr/bin/env python3
"""Summarize local BeaconMesh metadata; no device access or payload output."""
import collections
import csv
import re
import statistics
import sys


def summarize(path):
    timings = collections.defaultdict(list)
    outcomes = collections.Counter()
    with open(path, encoding="utf-8") as source:
        for line in source:
            if "MeshDiagnostic(event=" not in line:
                continue
            fields = dict(re.findall(r"(?:\(|, )(\w+)=([^,)]*)", line))
            event = fields.get("event")
            if event == "gatt_operation" and fields.get("status") == "SUCCESS":
                duration = fields.get("durationMillis", "")
                if duration.isdecimal():
                    timings[fields.get("phase", "UNKNOWN")].append(int(duration))
            elif event == "gatt_result":
                outcomes[fields.get("status", "UNKNOWN")] += 1
                if fields.get("status") == "LINK_WRITTEN":
                    for field in ("durationMillis", "elapsedMillis"):
                        value = fields.get(field, "")
                        if value.isdecimal():
                            timings[f"TRANSFER_{field}"].append(int(value))
    writer = csv.writer(sys.stdout)
    writer.writerow(["phase", "count", "min_ms", "median_ms", "p95_ms", "max_ms"])
    for phase, values in sorted(timings.items()):
        ordered = sorted(values)
        # Nearest-rank percentile, including small samples.
        p95 = ordered[(95 * len(ordered) + 99) // 100 - 1]
        writer.writerow([phase, len(ordered), ordered[0], statistics.median(ordered), p95, ordered[-1]])
    for outcome, count in sorted(outcomes.items()):
        print(f"outcome,{outcome},{count}")
    if not timings and not outcomes:
        print("No BeaconMesh measurements found; no BLE result can be inferred.", file=sys.stderr)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: python3 scripts/mesh-ble-summary.py INPUT.log")
    summarize(sys.argv[1])
