# Agent instructions

See [CONTRIBUTING.md](CONTRIBUTING.md) for the full contribution guide. This file
summarizes what an agent needs to act correctly in this repo.

## What this repo is

Standalone SDK sample projects, one per language folder (`go-sandbox/`,
`java-sandbox/`, `python-sandbox/`, `ruby-sandbox/`, `rust-sandbox/`,
`typescript-sandbox/`, `dotnet-sandbox/`), each pulled into docs.temporal.io
pages via Snipsync snippet markers. Folders are independent — don't add
cross-language shared tooling.

**CI is compile-only.** `.github/workflows/ci-on-pr.yml` runs one job per
language on every PR (all jobs, regardless of which folder changed). It compiles
code and test files but never runs tests, and Python/Ruby only get a syntax
check. Passing CI is not validation — local commands are still required and must
be reported in the PR description.

## Rules

- Match the existing layout and style of the target `*-sandbox/` folder before
  inventing a new structure (e.g. new Java samples are their own Maven module,
  like `java-sandbox/external-storage`).
- Never invent a docs.temporal.io URL or slug. Only reference a guide URL the
  user has given you, or leave the link as a placeholder for the docs writer to
  fill in.
- When a code block is meant to be embedded in a doc page, wrap it in matching
  Snipsync start/end comments, as shown in
  [CONTRIBUTING.md](CONTRIBUTING.md#snipsync-markers). Use `#` instead of `//`
  for Python and Ruby. Names must be globally unique in the repo, using
  `<language>-<topic>-<detail>`
  (e.g. `java-s3-driver-create`). Don't reuse or guess a name that isn't
  explicitly given — check for collisions with `rg '@{3}SNIPSTART' .` first.
- Before opening a PR, run the target language's local commands and paste them
  into the PR description:

  | Language | Commands |
  |---|---|
  | TypeScript | `npm run lint`, `npm run format:check`, `npm run build`, `npm test` |
  | Go | `go test ./...` |
  | Java | `mvn verify` |
  | Python | `pytest` |
  | Ruby | `ruby -wc <file>.rb` for each changed file |
  | Rust | `cargo build`, `cargo test` |
  | .NET | `dotnet build` |

- Keep CI green: Java code must compile at release 21; commit `Cargo.lock` /
  `package-lock.json` with dependency changes (CI uses `--locked` / `npm ci`);
  if you add a sandbox folder or a Java module needing different settings,
  update `ci-on-pr.yml` in the same PR.
- Branch as `docs/<short-sample-name>`. If there's a companion PR in the
  `documentation` repo, mention it in this repo's PR description.
