# Contributing

This repo holds standalone SDK sample projects that get pulled into pages on
[docs.temporal.io](https://docs.temporal.io) via a snippet-extraction tool
("Snipsync"). Each language lives in its own top-level `*-sandbox/` folder and is
otherwise independent — there's no shared build tooling. A compile-only CI check
runs on every PR (see [CI checks](#ci-checks)), but it doesn't run tests, so
local validation is still required and must be reported in the PR description.

## Repo layout

| Path | What it is |
|---|---|
| `go-sandbox/` | Go SDK sample, flat files + `_test.go` siblings |
| `java-sandbox/` | Independent Maven projects, one per sample (e.g. `temporal-hello-world/`, `external-storage/`) |
| `python-sandbox/` | Flat Python sample files |
| `ruby-sandbox/` | Flat Ruby sample files with a `Gemfile` |
| `rust-sandbox/` | One Cargo crate, topic-per-file |
| `typescript-sandbox/` | TS sample (npm project) |
| `dotnet-sandbox/` | `.sln` with one `.csproj` per project (Client/Worker/Workflow) |

## Adding a new sample

1. Pick (or confirm with the docs writer) the exact guide/page on docs.temporal.io
   this sample supports, and agree on snippet names up front if the doc page's
   placeholders already exist.
2. Create a branch named `docs/<short-sample-name>`.
3. Add the sample under the matching `*-sandbox/` language folder, following that
   language's existing project layout (e.g. a new Maven module next to
   `java-sandbox/external-storage`, or flat files like `python-sandbox`).
4. Wrap each code block the doc page will embed in Snipsync markers (see below),
   using descriptive, globally-unique names.
5. Run the language's normal build/test/lint commands locally (see table below) —
   CI only checks that the code compiles — and note the exact commands run in the
   PR description.
6. Optionally add a short README in the sample linking the docs.temporal.io guide
   by URL, e.g. "This sample supports the Temporal guide [Guide title](https://docs.temporal.io/...)."
7. Open the PR against `main` here, and coordinate a companion PR in the
   `documentation` repo whose `.mdx` page has matching `SNIPSTART`/`SNIPEND`
   placeholders for the same snippet names. Reference the companion PR in this
   repo's PR description.

## Snipsync markers

Wrap the exact code a doc page will embed in matching start/end comments. The
snippet **name is the join key** — the docs repo has a `<!--SNIPSTART name-->` /
`<!--SNIPEND-->` placeholder in the target `.mdx` page, and Snipsync (run on the
docs-repo side) copies the code across by matching names.

```java
// @@@SNIPSTART java-s3-driver-create
S3AsyncClient s3Client = S3AsyncClient.builder().region(Region.US_EAST_2).build();
...
// @@@SNIPEND
```

Use `#` instead of `//` for Python and Ruby. Naming convention: `<language>-<topic>-<detail>`,
e.g. `java-s3-driver-create` (see
[java-sandbox/external-storage/src/main/java/io/temporal/docs/externalstorage/S3StorageDriverExamples.java](java-sandbox/external-storage/src/main/java/io/temporal/docs/externalstorage/S3StorageDriverExamples.java)).

## Testing/validation per language

CI doesn't run tests — run these locally before opening a PR, and list the
commands you ran in the PR description:

| Language | Commands |
|---|---|
| TypeScript | `npm run lint`, `npm run format:check`, `npm run build`, `npm test` |
| Go | `go test ./...` |
| Java | `mvn verify` (add JUnit 5 + Mockito tests under `src/test/java/` where practical) |
| Python | `pytest` (use `WorkflowEnvironment`/`ActivityEnvironment` for workflow/activity tests) |
| Ruby | `ruby -wc <file>.rb` for each file you changed |
| Rust | `cargo build`, `cargo test` |
| .NET | `dotnet build` |

## CI checks

The **Compile check** workflow ([.github/workflows/ci-on-pr.yml](.github/workflows/ci-on-pr.yml))
runs on every PR and every push to `main`. It has one job per language, and every
job runs regardless of which folder changed, so a break in any sandbox fails the
check.

| Job | What it runs |
|---|---|
| .NET | `dotnet build temporaliodotnethelloworld.sln --configuration Release` (.NET 9) |
| Go | `go build ./...`, `go vet ./...` (version from `go.mod`) |
| Java | `mvn -B -f <module>/pom.xml test-compile -Dmaven.compiler.release=21` for each `*/pom.xml` (JDK 21) |
| Python | `python -m compileall -q -x '/\.venv/' .` (Python 3.13) |
| Ruby | `ruby -wc` on every `.rb` file (Ruby 3.3) |
| Rust | `cargo check --all-targets --locked` (stable, with `protoc`) |
| TypeScript | `npm ci`, `npx tsc --noEmit` (version from `.nvmrc`) |

CI compiles test files but never runs tests, and Python/Ruby only get a syntax
check. Keep Java code at Java 21 or lower, and commit updated lockfiles
(`Cargo.lock`, `package-lock.json`) with any dependency change, since CI uses
`--locked` and `npm ci`. If you add a new sandbox folder or a Java module that
needs different settings, update the workflow in the same PR.

## Pull requests

- Branch name: `docs/<short-sample-name>`.
- Describe what the sample demonstrates and which local commands you ran to
  validate it.
- If a companion docs.temporal.io PR exists, link it and note that this PR
  supplies the Snipsync source for it (draft this PR if the doc page isn't ready
  yet).
