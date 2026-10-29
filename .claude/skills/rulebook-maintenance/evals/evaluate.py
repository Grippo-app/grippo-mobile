#!/usr/bin/env python3
"""Grade captured read-only routing decisions, not metadata or application code."""
import argparse
import json
import pathlib

RESPONSE_FIELDS = ["id", "skills", "references", "gates", "roles", "actions", "loaded_files"]


def nonempty_text(value):
    return isinstance(value, str) and bool(value.strip()) and not any(0xD800 <= ord(char) <= 0xDFFF for char in value)


def validate_cases(cases):
    if not isinstance(cases, list) or not cases:
        return ["cases must be a non-empty array"]
    problems, identities = [], set()
    for number, case in enumerate(cases, 1):
        if not isinstance(case, dict):
            problems.append(f"case {number}: must be an object")
            continue
        for field in ("id", "prompt"):
            if not nonempty_text(case.get(field)):
                problems.append(f"case {number}: {field} must be a non-empty Unicode string")
        identity = case.get("id")
        if nonempty_text(identity):
            if identity in identities:
                problems.append(f"duplicate case: {identity}")
            identities.add(identity)
        for field in ("required_skills", "required_gates", "forbidden_actions"):
            values = case.get(field)
            if not isinstance(values, list) or any(not nonempty_text(x) for x in values):
                problems.append(f"case {number}: {field} must be a string list")
    return problems


def grade(cases, responses):
    problems = validate_cases(cases)
    if problems:
        return problems
    if not isinstance(responses, list):
        return ["responses must be an array"]
    by_id = {}
    for response in responses:
        if not isinstance(response, dict) or not nonempty_text(response.get("id")):
            problems.append("response must be an object with a non-empty Unicode string id")
            continue
        identity = response.get("id")
        if identity in by_id:
            problems.append(f"duplicate response: {identity}")
        by_id[identity] = response
    known = {case["id"] for case in cases}
    for identity in by_id.keys() - known:
        problems.append(f"unknown response: {identity}")
    for case in cases:
        identity = case["id"]
        response = by_id.get(identity)
        if response is None:
            problems.append(f"{identity}: missing response")
            continue
        valid = set()
        for field in ("skills", "references", "gates", "roles", "actions", "loaded_files"):
            if not isinstance(response.get(field), list) or any(not nonempty_text(x) for x in response[field]):
                problems.append(f"{identity}: {field} must be a string list")
            else:
                valid.add(field)
        for expected, actual in (("required_skills", "skills"), ("required_gates", "gates")):
            if actual in valid:
                missing = set(case[expected]) - set(response[actual])
                if missing:
                    problems.append(f"{identity}: missing {actual}: {sorted(missing)}")
        if "actions" in valid:
            forbidden = set(case["forbidden_actions"]) & set(response["actions"])
            if forbidden:
                problems.append(f"{identity}: forbidden actions: {sorted(forbidden)}")
    return problems


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("responses", type=pathlib.Path)
    args = parser.parse_args()
    try:
        catalog = json.loads(pathlib.Path(__file__).with_name("cases.json").read_text(encoding="utf-8"))
        if not isinstance(catalog, dict) or type(catalog.get("schema")) is not int or catalog["schema"] != 1 or catalog.get("response_fields") != RESPONSE_FIELDS:
            raise ValueError("unsupported case catalog schema or response_fields")
        cases = catalog.get("cases")
        responses = json.loads(args.responses.read_text(encoding="utf-8"))
    except (OSError, ValueError) as error:
        parser.error(str(error))
    problems = grade(cases, responses)
    for problem in problems:
        print("FAIL", problem)
    print(f"{len(cases) if isinstance(cases, list) else 0} cases; {len(problems)} problem(s)")
    raise SystemExit(bool(problems))


if __name__ == "__main__":
    main()
