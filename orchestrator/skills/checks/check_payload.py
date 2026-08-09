#!/usr/bin/env python3
"""skills:payload-schemas — every AI-dispatched task queue operation has a
payload schema, and the schema's constraints reject malicious input (path
traversal in stem, bad format, oversize). Deterministic backlog creation has its
own server/helper input contract and is intentionally absent here. Dependency-
free minimal validator against the schema's stem pattern / enums / maxLengths.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SCHEMAS = os.path.normpath(os.path.join(HERE, "..", "_index", "payload-schemas"))
OPS = ["prep", "answers", "run", "drop"]


def validate(payload, schema):
    props = schema.get("properties", {})
    for req in schema.get("required", []):
        if req not in payload:
            return False, f"missing {req}"
    if schema.get("additionalProperties") is False:
        for k in payload:
            if k not in props:
                return False, f"additional prop {k}"
    for k, v in payload.items():
        spec = props.get(k, {})
        if "const" in spec and v != spec["const"]:
            return False, f"{k} const"
        if "enum" in spec and v not in spec["enum"]:
            return False, f"{k} enum"
        if spec.get("type") == "string":
            if not isinstance(v, str):
                return False, f"{k} type"
            if "maxLength" in spec and len(v) > spec["maxLength"]:
                return False, f"{k} maxLength"
            if "pattern" in spec and not re.match(spec["pattern"], v):
                return False, f"{k} pattern"
        if spec.get("type") == "array":
            if not isinstance(v, list):
                return False, f"{k} type"
            if "maxItems" in spec and len(v) > spec["maxItems"]:
                return False, f"{k} maxItems"
            item_spec = spec.get("items", {})
            for idx, item in enumerate(v):
                if item_spec.get("type") == "string":
                    if not isinstance(item, str):
                        return False, f"{k}[{idx}] type"
                    if "maxLength" in item_spec and len(item) > item_spec["maxLength"]:
                        return False, f"{k}[{idx}] maxLength"
                    if "pattern" in item_spec and not re.match(item_spec["pattern"], item):
                        return False, f"{k}[{idx}] pattern"
    return True, "ok"


def main():
    fail = 0
    for op in OPS:
        path = os.path.join(SCHEMAS, f"task.{op}.schema.json")
        if not os.path.isfile(path):
            print(f"    FAIL: missing schema task.{op}", file=sys.stderr); fail = 1; continue
        schema = json.load(open(path))
        good = {"operation": f"task.{op}", "mode": "skills"}
        good["stem"] = "TASK_7_profile_note_archive"
        ok, why = validate(good, schema)
        if not ok:
            print(f"    FAIL: task.{op} rejected a valid payload ({why})", file=sys.stderr); fail = 1
        # malicious: path traversal in stem
        for mal, label in [
            ({"operation": f"task.{op}", "stem": "../../etc/passwd"}, "path-traversal stem"),
            ({"operation": f"task.{op}", "stem": "TASK_7", "extra": "x"}, "additional prop"),
            ({"operation": f"task.{op}", "stem": "TASK_7_x", "mode": "evil"}, "bad mode enum"),
            ({"operation": f"task.{op}", "stem": "TASK_7_x", "answers": []}, "bad string type"),
            ({"operation": f"task.{op}", "stem": "TASK_7_x", "selectedIds": ["x"] * 201}, "selectedIds maxItems"),
            ({"operation": f"task.{op}", "stem": "TASK_7_x", "selectedIds": ["x" * 201]}, "selectedIds item maxLength"),
        ]:
            ok, _ = validate(mal, schema)
            if ok:
                print(f"    FAIL: task.{op} ACCEPTED malicious payload ({label})", file=sys.stderr); fail = 1
    if not fail:
        print(f"    ok: {len(OPS)} task payload schemas accept valid + reject malicious (traversal/extra/enum)")
    return fail


if __name__ == "__main__":
    sys.exit(main())
