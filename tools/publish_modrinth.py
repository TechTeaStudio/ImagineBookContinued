"""Publish the built mod jar and its sources jar to Modrinth as a new version.

Run by .github/workflows/build.yml on a `v*` tag, after the build, the tests and the Mixin check
passed. Everything comes from the repository: VERSION is the version, its CHANGELOG.md section is the
changelog, build/libs holds the two jars. Standard library only, so CI needs no extra download.

Environment:
    MODRINTH_TOKEN    personal access token with the scopes Create versions, Read projects and
                      Read versions (a GitHub secret; it is sent only in the Authorization header
                      and never printed)
    MODRINTH_PROJECT  project slug or ID (default imaginebook-continued)

Usage (from the repository root):
    python tools/publish_modrinth.py --tag v1.3.0.1   # publish
    python tools/publish_modrinth.py --dry-run        # print the version data, send nothing
"""
import argparse
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
API = "https://api.modrinth.com/v2"
MC = "26.3"
ARTIFACT = "imaginebook-continued-fabric"
# Modrinth blocks generic clients; it asks for a User-Agent that names the project.
USER_AGENT = "TechTeaStudio/ImagineBookContinued (github.com/TechTeaStudio)"
# The version API takes project IDs, not slugs. Read from api.modrinth.com/v2/project/<slug> on
# 2026-11-17; Modrinth IDs never change.
DEPENDENCIES = [
    ("P7dR8mSH", "required"),  # fabric-api
    ("BWloQ9jK", "optional"),  # stendhal: compatible since 1.3.0.0, not needed
]


def changelog_section(text, version):
    """The body under `## <version>` up to the next `## ` heading."""
    lines = text.splitlines()
    try:
        start = lines.index(f"## {version}") + 1
    except ValueError:
        sys.exit(f"CHANGELOG.md has no '## {version}' heading")
    end = next((i for i in range(start, len(lines)) if lines[i].startswith("## ")), len(lines))
    body = "\n".join(lines[start:end]).strip()
    if not body:
        sys.exit(f"CHANGELOG.md section '## {version}' is empty")
    return body


def check_tag(tag, version):
    if tag != f"v{version}":
        sys.exit(f"tag '{tag}' does not match VERSION {version}; expected v{version}")


def version_data(version, project_id, changelog):
    return {
        "project_id": project_id,
        "name": f"{version} for Minecraft {MC}",
        "version_number": f"{version}+{MC}",
        "changelog": changelog,
        "dependencies": [{"project_id": pid, "dependency_type": kind} for pid, kind in DEPENDENCIES],
        "game_versions": [MC],
        "loaders": ["fabric"],
        "version_type": "release",
        "environment": "client_only",
        "featured": False,
        "status": "listed",
        "file_parts": ["primary", "sources"],
        "primary_file": "primary",
        # LGPL-3.0: the repository is private, so the sources travel with every version.
        "file_types": {"sources": "sources-jar"},
    }


def find_jars(libs, version):
    jar = libs / f"{ARTIFACT}-{version}+{MC}.jar"
    sources = libs / f"{ARTIFACT}-{version}+{MC}-sources.jar"
    for path in (jar, sources):
        if not path.is_file():
            sys.exit(f"{path} not found; build first")
    return jar, sources


def multipart(fields, files):
    boundary = uuid.uuid4().hex
    parts = []
    for name, value in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n'
                     f"Content-Type: application/json\r\n\r\n".encode() + value.encode() + b"\r\n")
    for name, path in files.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{path.name}"\r\n'
                     f"Content-Type: application/java-archive\r\n\r\n".encode() + path.read_bytes() + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode())
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def headers(token):
    return {"Authorization": token, "User-Agent": USER_AGENT}


def call(method, path, token, body=None, content_type=None):
    request = urllib.request.Request(API + path, data=body, method=method, headers=headers(token))
    if content_type:
        request.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            return json.loads(response.read() or b"null")
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", "replace")[:500]
        hint = " (check the token scopes and MODRINTH_PROJECT)" if error.code in (401, 403, 404) else ""
        sys.exit(f"Modrinth {method} {path} failed: HTTP {error.code}{hint}: {detail}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--tag", default="")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    version = (ROOT / "VERSION").read_text(encoding="utf-8").strip()
    changelog = changelog_section((ROOT / "CHANGELOG.md").read_text(encoding="utf-8"), version)
    jar, sources = find_jars(ROOT / "build" / "libs", version)
    project = os.environ.get("MODRINTH_PROJECT", "imaginebook-continued")
    if args.dry_run:
        print(json.dumps(version_data(version, f"<id of {project}>", changelog), indent=2))
        print("files:", jar.name, sources.name)
        return
    check_tag(args.tag, version)
    token = os.environ.get("MODRINTH_TOKEN", "")
    if not token:
        sys.exit("MODRINTH_TOKEN is not set; add it as a repository secret")
    project_id = call("GET", f"/project/{project}", token)["id"]
    data = version_data(version, project_id, changelog)
    # Re-running a release job must not fail on, or duplicate, a version that is already up.
    existing = call("GET", f"/project/{project_id}/version", token) or []
    if any(v.get("version_number") == data["version_number"] for v in existing):
        print(f"{data['version_number']} is already on Modrinth; nothing to do")
        return
    body, content_type = multipart({"data": json.dumps(data)}, {"primary": jar, "sources": sources})
    created = call("POST", "/version", token, body, content_type)
    print(f"published {created['version_number']} (version id {created['id']}) to Modrinth project {project}")


if __name__ == "__main__":
    main()
