# Contributing

This repo holds standalone SDK sample projects that get pulled into pages on
[docs.temporal.io](https://docs.temporal.io) via a snippet-extraction tool
("Snipsync"). Each language lives in its own top-level `*-sandbox/` folder and is
otherwise independent — there's no shared build tooling, and no CI in this repo.
Validation is manual, per-project, and reported in the PR description.

## Repo layout

| Path | What it is |
|---|---|
| `go-sandbox/` | Go SDK sample, flat files + `_test.go` siblings |
| `java-sandbox/` | Independent Maven projects, one per sample (e.g. `temporal-hello-world/`, `external-storage/`) |
| `python-sandbox/` | Flat Python sample files |
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
   there is no CI to catch issues — and note the exact commands run in the PR
   description.
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

Use `#` instead of `//` for Python. Naming convention: `<language>-<topic>-<detail>`,
e.g. `java-s3-driver-create` (see
[java-sandbox/external-storage/src/main/java/io/temporal/docs/externalstorage/S3StorageDriverExamples.java](java-sandbox/external-storage/src/main/java/io/temporal/docs/externalstorage/S3StorageDriverExamples.java)).

## Testing/validation per language

There's no CI — run these locally before opening a PR, and list the commands you
ran in the PR description:

| Language | Commands |
|---|---|
| TypeScript | `npm run lint`, `npm run format:check`, `npm run build`, `npm test` |
| Go | `go test ./...` |
| Java | `mvn verify` (add JUnit 5 + Mockito tests under `src/test/java/` where practical) |
| Python | `pytest` (use `WorkflowEnvironment`/`ActivityEnvironment` for workflow/activity tests) |
| Rust | `cargo build`, `cargo test` |
| .NET | `dotnet build` |

## Pull requests

- Branch name: `docs/<short-sample-name>`.
- Describe what the sample demonstrates and which local commands you ran to
  validate it.
- If a companion docs.temporal.io PR exists, link it and note that this PR
  supplies the Snipsync source for it (draft this PR if the doc page isn't ready
  yet).
