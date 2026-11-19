#!/usr/bin/env python3
"""Checks (and optionally upgrades) each sandbox's Temporal SDK version.

For every language folder, reads the SDK version the samples actually build
with (from the lockfile where one exists, since manifests often hold ranges
like ^1.25.0 or >=1.35) and compares it to the newest stable release of the
matching temporalio/sdk-<language> repo on GitHub.

Check mode (default): prints a Markdown report, appends it to
$GITHUB_STEP_SUMMARY when set, emits a ::warning annotation per outdated
group, and exits 1 if any sandbox is behind, 2 if a version can't be read or
fetched.

Upgrade mode (--upgrade): bumps every outdated sandbox to the latest release,
regenerating lockfiles with each language's own tooling. A language whose
upgrade fails is reverted and reported, and the others still go through.
Writes a pull request body to --pr-body and exits 0.

Writes these step outputs to $GITHUB_OUTPUT when set:
  check:   status=ok|outdated|error, languages=<space-separated slugs>
  upgrade: changed=true|false, failed=true|false, title=<PR title>

Usage: check-sdk-versions.py [--report report.md]
       check-sdk-versions.py --upgrade [--pr-body body.md] [--files files.txt]
Run from inside the repo to check; it reads the repo containing the current
directory. Upgrade mode only keeps changes to manifests and lockfiles that
hold SDK versions (see VERSION_FILES) and reverts anything else.
Set GITHUB_TOKEN (or GH_TOKEN) to avoid GitHub API rate limits.
Requires: Python 3.11+ (stdlib only). Upgrade mode also needs the toolchain
for each outdated language: go, npm, uv, composer, bundle, cargo.
"""

import argparse
import json
import os
import re
import subprocess
import sys
import tomllib
import urllib.request
import xml.etree.ElementTree as ET
from functools import cache
from pathlib import Path

# The repo containing the current directory, not this script: CI runs a copy
# of the script from a separate checkout against the default branch.
REPO_ROOT = Path(
    subprocess.check_output(["git", "rev-parse", "--show-toplevel"], text=True).strip()
)

# The only files an upgrade may change: the manifests and lockfiles that hold
# SDK versions. Anything else a tool touches is reverted and never committed.
VERSION_FILES = {
    "go.mod", "go.sum",  # Go
    "pom.xml",  # Java
    "uv.lock",  # Python
    "package.json", "package-lock.json",  # TypeScript
    "composer.json", "composer.lock",  # PHP
    "Gemfile", "Gemfile.lock",  # Ruby
    "Cargo.toml", "Cargo.lock",  # Rust
}  # plus *.csproj for .NET


def is_version_file(path):
    parts = Path(path).parts
    return (
        len(parts) > 1
        and parts[0].endswith("-sandbox")
        and (parts[-1] in VERSION_FILES or parts[-1].endswith(".csproj"))
    )

# Plain stable tags only: v1.2.3, 1.2.3, or v2.20. Skips contrib/... tags
# (sdk-go) and prerelease suffixes like -rc.1.
STABLE_TAG = re.compile(r"^v?(\d+(?:\.\d+){1,2})$")


class CheckError(Exception):
    pass


def tracked(pattern):
    """Git-tracked files matching a pathspec, so vendor/ and .venv/ are skipped."""
    out = subprocess.check_output(
        ["git", "ls-files", "--", pattern], cwd=REPO_ROOT, text=True
    )
    return [REPO_ROOT / line for line in out.splitlines() if line]


def parse_version(text):
    match = STABLE_TAG.match(text.strip())
    if not match:
        raise CheckError(f"Unrecognized version {text!r}")
    parts = [int(p) for p in match.group(1).split(".")]
    return tuple(parts + [0] * (3 - len(parts)))


def fmt(version):
    return ".".join(str(p) for p in version)


def rel(path):
    return str(Path(path).relative_to(REPO_ROOT))


@cache
def latest_release(repo):
    token = os.environ.get("GITHUB_TOKEN") or os.environ.get("GH_TOKEN")
    headers = {"Accept": "application/vnd.github+json", "User-Agent": "sdk-version-check"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(
        f"https://api.github.com/repos/{repo}/releases?per_page=100", headers=headers
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        releases = json.load(response)
    versions = [
        parse_version(r["tag_name"])
        for r in releases
        if not r["draft"] and not r["prerelease"] and STABLE_TAG.match(r["tag_name"])
    ]
    if not versions:
        raise CheckError(f"No stable release found for {repo}")
    return max(versions)


# Readers return a list of (path, version string as written in the file).


def java_poms():
    # Skips dependency-reduced-pom.xml from the shade plugin.
    return [p for p in tracked("java-sandbox/*pom.xml") if p.name == "pom.xml"]


def read_go():
    path = REPO_ROOT / "go-sandbox/go.mod"
    match = re.search(r"^\s*(?:require\s+)?go\.temporal\.io/sdk\s+(\S+)", path.read_text(), re.M)
    return [(path, match.group(1))] if match else []


def read_java():
    found = []
    for path in java_poms():
        root = ET.parse(path).getroot()
        ns = {"m": root.tag[1:].split("}")[0]} if root.tag.startswith("{") else {}
        p = "m:" if ns else ""
        props = {
            el.tag.split("}")[-1]: (el.text or "").strip()
            for el in root.findall(f"{p}properties/*", ns)
        }
        for dep in root.iter(f"{{{ns['m']}}}dependency" if ns else "dependency"):
            group = dep.findtext(f"{p}groupId", namespaces=ns)
            artifact = dep.findtext(f"{p}artifactId", namespaces=ns)
            version = dep.findtext(f"{p}version", namespaces=ns)
            if group == "io.temporal" and artifact == "temporal-sdk" and version:
                version = re.sub(r"\$\{([^}]+)\}", lambda m: props.get(m.group(1), m.group(0)), version)
                found.append((path, version.strip()))
    return found


def read_python():
    found = []
    for path in tracked("python-sandbox/*uv.lock"):
        lock = tomllib.loads(path.read_text())
        versions = [p["version"] for p in lock.get("package", []) if p["name"] == "temporalio"]
        # uv can fork a resolution by Python version (e.g. an older temporalio
        # for python < 3.11 that newer releases no longer support). Only the
        # newest entry is something we can upgrade, so check that one.
        if versions:
            found.append((path, max(versions, key=parse_version)))
    return found


def read_typescript():
    path = REPO_ROOT / "typescript-sandbox/package-lock.json"
    packages = json.loads(path.read_text())["packages"]
    core = ["activity", "client", "common", "testing", "worker", "workflow"]
    return [
        (path, packages[f"node_modules/@temporalio/{name}"]["version"])
        for name in core
        if f"node_modules/@temporalio/{name}" in packages
    ]


def read_dotnet():
    found = []
    for path in tracked("dotnet-sandbox/*.csproj"):
        for ref in ET.parse(path).getroot().iter("PackageReference"):
            if ref.get("Include") == "Temporalio" and ref.get("Version"):
                found.append((path, ref.get("Version")))
    return found


def read_php():
    path = REPO_ROOT / "php-sandbox/composer.lock"
    lock = json.loads(path.read_text())
    return [
        (path, package["version"])
        for package in lock.get("packages", []) + lock.get("packages-dev", [])
        if package["name"] == "temporal/sdk"
    ]


def read_ruby():
    path = REPO_ROOT / "ruby-sandbox/Gemfile.lock"
    # Matches "    temporalio (1.11.0)" but not platform gems like "(1.11.0-arm64-darwin)".
    versions = set(re.findall(r"^    temporalio \((\d+(?:\.\d+)*)\)$", path.read_text(), re.M))
    return [(path, v) for v in sorted(versions)]


def read_rust():
    found = []
    for path in tracked("rust-sandbox/*Cargo.lock"):
        lock = tomllib.loads(path.read_text())
        for package in lock.get("package", []):
            if package["name"] == "temporalio-sdk":
                found.append((path, package["version"]))
    return found


# Upgraders take behind ({old version as written: [lockfile/manifest paths]})
# and the latest version tuple. Each Temporal-owned dependency pinned at an
# old core version is bumped with it, since every artifact in a language's SDK
# repo is released at the same version.


def run(cmd, cwd):
    print(f"::group::{cwd}$ {' '.join(cmd)}", file=sys.stderr)
    result = subprocess.run(cmd, cwd=REPO_ROOT / cwd, capture_output=True, text=True)
    print(result.stdout[-5000:] + result.stderr[-5000:], file=sys.stderr)
    print("::endgroup::", file=sys.stderr)
    if result.returncode != 0:
        tail = "\n".join((result.stderr or result.stdout).strip().splitlines()[-15:])
        raise CheckError(f"`{' '.join(cmd)}` failed in {cwd}:\n{tail}")


def edit_files(paths, edit):
    """Applies edit(text) -> text to each file; returns the files that changed."""
    changed = []
    for path in paths:
        text = path.read_text()
        new_text = edit(text)
        if new_text != text:
            path.write_text(new_text)
            changed.append(path)
    return changed


def replace_versions(pattern, old_versions, new):
    """Edit function replacing OLD in pattern (with an {old} placeholder and
    groups 1 and 2 around the version) for each old version."""

    def edit(text):
        for old in old_versions:
            # str.replace, not str.format: patterns contain literal regex braces.
            text = re.sub(pattern.replace("{old}", re.escape(old)), rf"\g<1>{new}\g<2>", text, flags=re.M)
        return text

    return edit


def upgrade_go(behind, latest):
    run(["go", "get", f"go.temporal.io/sdk@v{fmt(latest)}"], "go-sandbox")
    run(["go", "mod", "tidy"], "go-sandbox")


def upgrade_java(behind, latest):
    new = fmt(latest)

    def edit(text):
        for old in behind:
            text = text.replace(f"<temporal.version>{old}</temporal.version>", f"<temporal.version>{new}</temporal.version>")
            text = re.sub(
                r"<dependency>.*?</dependency>",
                lambda m: m.group(0).replace(f"<version>{old}</version>", f"<version>{new}</version>")
                if "<groupId>io.temporal</groupId>" in m.group(0)
                else m.group(0),
                text,
                flags=re.S,
            )
        return text

    if not edit_files(java_poms(), edit):
        raise CheckError("No pom.xml versions matched")


def upgrade_python(behind, latest):
    for files in behind.values():
        for lockfile in files:
            run(["uv", "lock", "--upgrade-package", "temporalio"], rel(Path(lockfile).parent))


def upgrade_typescript(behind, latest):
    edit = replace_versions(r'("@temporalio/[^"]+":\s*"[\^~]?){old}(")', behind, fmt(latest))
    if not edit_files([REPO_ROOT / "typescript-sandbox/package.json"], edit):
        raise CheckError("No @temporalio versions in package.json matched")
    # @temporalio packages pin each other exactly as peers, so npm can't move
    # them in place (ERESOLVE). Drop them from the lockfile and let npm
    # re-resolve just those, keeping every other locked version.
    lock_path = REPO_ROOT / "typescript-sandbox/package-lock.json"
    lock = json.loads(lock_path.read_text())
    lock["packages"] = {k: v for k, v in lock["packages"].items() if "node_modules/@temporalio/" not in k}
    lock_path.write_text(json.dumps(lock, indent=2) + "\n")
    run(["npm", "install", "--package-lock-only", "--ignore-scripts", "--no-audit", "--no-fund"], "typescript-sandbox")


def upgrade_dotnet(behind, latest):
    edit = replace_versions(
        r'(<PackageReference\s+Include="Temporalio(?:\.[^"]*)?"\s+Version="){old}(")', behind, fmt(latest)
    )
    if not edit_files(tracked("dotnet-sandbox/*.csproj"), edit):
        raise CheckError("No Temporalio PackageReference versions matched")


def upgrade_php(behind, latest):
    constraint = f"^{latest[0]}.{latest[1]}"
    edit_files(
        [REPO_ROOT / "php-sandbox/composer.json"],
        lambda text: re.sub(r'("temporal/sdk":\s*")[^"]*(")', rf"\g<1>{constraint}\g<2>", text),
    )
    run(
        ["composer", "update", "temporal/sdk", "--with-all-dependencies", "--no-install", "--no-interaction", "--no-progress"],
        "php-sandbox",
    )


def upgrade_ruby(behind, latest):
    constraint = f"{latest[0]}.{latest[1]}"
    edit_files(
        [REPO_ROOT / "ruby-sandbox/Gemfile"],
        lambda text: re.sub(r"""(gem\s+["']temporalio["']\s*,\s*["']~>\s*)[\d.]+(["'])""", rf"\g<1>{constraint}\g<2>", text),
    )
    run(["bundle", "lock", "--update", "temporalio"], "ruby-sandbox")


def upgrade_rust(behind, latest):
    # Matches both temporalio-sdk = "1.2.0" and temporalio-sdk = { version = "1.2.0", ... }.
    edit = replace_versions(
        r'^(temporalio-[a-z-]+\s*=\s*(?:\{[^}\n]*?version\s*=\s*)?"){old}(")', behind, fmt(latest)
    )
    changed = edit_files(tracked("rust-sandbox/*Cargo.toml"), edit)
    if not changed:
        raise CheckError("No temporalio-* versions in Cargo.toml matched")
    # Each sample is its own crate with its own Cargo.lock. Resolving metadata
    # rewrites the lockfile to satisfy the new requirements, changing as little
    # else as possible.
    for manifest in changed:
        run(["cargo", "metadata", "--format-version", "1", "--manifest-path", manifest.name], rel(manifest.parent))


SDKS = [
    # slug (folder is <slug>-sandbox), display name, SDK repo, reader, upgrader, local commands from AGENTS.md
    ("dotnet", ".NET", "temporalio/sdk-dotnet", read_dotnet, upgrade_dotnet, ["dotnet build"]),
    ("go", "Go", "temporalio/sdk-go", read_go, upgrade_go, ["go test ./..."]),
    ("java", "Java", "temporalio/sdk-java", read_java, upgrade_java, ["mvn verify"]),
    ("php", "PHP", "temporalio/sdk-php", read_php, upgrade_php, ["php -l <file>.php"]),
    ("python", "Python", "temporalio/sdk-python", read_python, upgrade_python, ["pytest"]),
    ("ruby", "Ruby", "temporalio/sdk-ruby", read_ruby, upgrade_ruby, ["ruby -wc <file>.rb"]),
    ("rust", "Rust", "temporalio/sdk-rust", read_rust, upgrade_rust, ["cargo build", "cargo test"]),
    (
        "typescript",
        "TypeScript",
        "temporalio/sdk-typescript",
        read_typescript,
        upgrade_typescript,
        ["npm run lint", "npm run format:check", "npm run build", "npm test"],
    ),
]


def check_all():
    results = []
    for slug, language, repo, reader, upgrader, commands in SDKS:
        result = {"slug": slug, "language": language, "repo": repo, "upgrader": upgrader, "commands": commands}
        try:
            result["latest"] = latest = latest_release(repo)
            uses = reader()
            if not uses:
                raise CheckError("No Temporal SDK dependency found")
            # {old version as written: [files]}
            behind = {}
            for path, version in uses:
                if parse_version(version) < latest:
                    files = behind.setdefault(version, [])
                    if path not in files:
                        files.append(path)
            result["behind"] = behind
            result["used"] = sorted({fmt(parse_version(v)) for _, v in uses}, key=parse_version)
        except Exception as error:  # report every language, not just the first failure
            result["error"] = str(error)
        results.append(result)
    return results


def render(results, heading="## Temporal SDK versions", annotate=False):
    rows, outdated = [], []
    for r in results:
        language, repo = r["language"], r["repo"]
        if "error" in r:
            rows.append(f"| {language} | ? | ? | ⚠️ check failed: {r['error']} |")
            if annotate:
                print(f"::error::{language} SDK version check failed: {r['error']}", file=sys.stderr)
            continue
        latest, behind = fmt(r["latest"]), r["behind"]
        file_count = sum(len(files) for files in behind.values())
        status = f"❌ {file_count} file(s) behind" if behind else "✅ up to date"
        rows.append(f"| {language} | {', '.join(r['used'])} | [{latest}](https://github.com/{repo}/releases) | {status} |")
        for version, files in sorted(behind.items(), key=lambda item: parse_version(item[0])):
            names = sorted(rel(f) for f in files)
            listing = "\n".join(f"  - `{f}`" for f in names)
            outdated.append(
                f"- **{language}** {fmt(parse_version(version))} → {latest} ({len(files)} file(s))\n"
                f"  <details><summary>Files</summary>\n\n{listing}\n\n  </details>"
            )
            if annotate:
                # One annotation per group; GitHub shows at most 10 warnings per step.
                print(
                    f"::warning file={names[0]}::{language} SDK {version} is behind the "
                    f"latest release {latest} in {len(files)} file(s)",
                    file=sys.stderr,
                )
    return "\n".join(
        [heading, "", "| Language | In use | Latest release | Status |", "|---|---|---|---|"]
        + rows
        + (["", "### Outdated files", ""] + outdated if outdated else [])
    ) + "\n"


def set_outputs(**outputs):
    if path := os.environ.get("GITHUB_OUTPUT"):
        with open(path, "a") as f:
            for key, value in outputs.items():
                f.write(f"{key}={value}\n")


def write_summary(text):
    if summary := os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(summary, "a") as f:
            f.write(text)


def check(args):
    results = check_all()
    report = render(results, annotate=True)
    print(report)
    if args.report:
        args.report.write_text(report)
    write_summary(report)

    outdated = [r["slug"] for r in results if r.get("behind")]
    errored = any("error" in r for r in results)
    set_outputs(status="error" if errored else "outdated" if outdated else "ok", languages=" ".join(outdated))
    if errored:
        sys.exit(2)
    sys.exit(1 if outdated else 0)


def upgrade(args):
    before = check_all()
    upgraded, failed = [], []
    for r in before:
        if not r.get("behind"):
            continue
        old = ", ".join(sorted({fmt(parse_version(v)) for v in r["behind"]}, key=parse_version))
        try:
            r["upgrader"](r["behind"], r["latest"])
            upgraded.append((r, f"{r['language']} {old} → {fmt(r['latest'])}"))
        except Exception as error:
            # Leave this language untouched so the rest can still ship.
            subprocess.run(["git", "checkout", "--", f"{r['slug']}-sandbox"], cwd=REPO_ROOT, check=True)
            failed.append(f"- **{r['language']}** {old} → {fmt(r['latest'])}:\n\n  ```\n  {str(error).replace(chr(10), chr(10) + '  ')}\n  ```")
            print(f"::error::{r['language']} SDK upgrade failed: {error}", file=sys.stderr)

    # Keep only changes to version files; revert anything else a tool touched.
    # Only sandbox folders: upgrades run there, and only listed files are ever
    # committed, so other local edits (e.g. to .github/) are left alone.
    modified = subprocess.check_output(
        ["git", "diff", "--name-only", "--", "*-sandbox/*"], cwd=REPO_ROOT, text=True
    ).splitlines()
    files = [f for f in modified if is_version_file(f)]
    if stray := [f for f in modified if not is_version_file(f)]:
        subprocess.run(["git", "checkout", "--", *stray], cwd=REPO_ROOT, check=True)
        for f in stray:
            print(f"::warning file={f}::Reverted: the upgrade changed a file that doesn't hold SDK versions", file=sys.stderr)
    if args.files:
        args.files.write_text("".join(f"{f}\n" for f in files))
    changed = bool(files)

    after = render(check_all(), heading="## SDK versions after this PR")

    commands = "\n".join(
        f"- [ ] {r['language']} (`{r['slug']}-sandbox/`): " + ", ".join(f"`{c}`" for c in r["commands"])
        for r, _ in upgraded
    )
    source = "the Check SDK versions workflow"
    if os.environ.get("GITHUB_RUN_ID"):
        run_url = f"{os.environ['GITHUB_SERVER_URL']}/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}"
        source = f"the [Check SDK versions]({run_url}) workflow"
    body = "\n".join(
        [
            f"Upgrades the sandboxes to the latest Temporal SDK releases. Opened by {source}.",
            "",
            "### Upgraded",
            "",
            *[f"- {line}" for _, line in upgraded],
            *(["", "### Could not upgrade automatically", "", *failed] if failed else []),
            *(
                ["", "### Reverted", "", "The upgrade tools also changed these files, which don't hold SDK versions, so they were left out:", "", *[f"- `{f}`" for f in stray]]
                if stray
                else []
            ),
            "",
            after,
            "### Before merging",
            "",
            "CI only compiles the samples, and the upgrade doesn't touch sample code, so "
            "check release notes for breaking changes and run each upgraded language's "
            "local commands (see AGENTS.md):",
            "",
            commands,
        ]
    ) + "\n"
    print(body)
    if args.pr_body:
        args.pr_body.write_text(body)
    write_summary(body)

    title = "chore(deps): upgrade Temporal SDKs (" + ", ".join(
        f"{r['language']} {fmt(r['latest'])}" for r, _ in upgraded
    ) + ")"
    set_outputs(changed=str(changed).lower(), failed=str(bool(failed)).lower(), title=title)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--report", type=Path, help="Check mode: also write the Markdown report here")
    parser.add_argument("--upgrade", action="store_true", help="Upgrade outdated sandboxes instead of failing")
    parser.add_argument("--pr-body", type=Path, help="Upgrade mode: write the pull request body here")
    parser.add_argument("--files", type=Path, help="Upgrade mode: write the changed version files to commit here, one per line")
    args = parser.parse_args()
    upgrade(args) if args.upgrade else check(args)


if __name__ == "__main__":
    main()
