# Temporal SDK sandboxes

Standalone Temporal SDK sample projects that supply the code snippets on
[docs.temporal.io](https://docs.temporal.io). Each language lives in its own
top-level `*-sandbox/` folder. The folders don't share any build tooling.
[Snipsync](#snipsync-markers) copies marked code blocks from this repo into
the matching doc pages.

| Folder | What it is |
|---|---|
| [`dotnet-sandbox/`](dotnet-sandbox/) | `.sln` with one `.csproj` per project (Client/Worker/Workflow) |
| [`go-sandbox/`](go-sandbox/) | Go module, with `_test.go` files next to the code they test |
| [`java-sandbox/`](java-sandbox/) | Independent Maven projects, one per sample (e.g. `temporal-hello-world/`, `external-storage/`) |
| [`python-sandbox/`](python-sandbox/) | Flat Python sample files |
| [`ruby-sandbox/`](ruby-sandbox/) | Flat Ruby sample files with a `Gemfile` |
| [`rust-sandbox/`](rust-sandbox/) | One Cargo crate, one topic per file |
| [`typescript-sandbox/`](typescript-sandbox/) | npm project |

## Contributing a sample

Every sample in this repo has to build and run, because readers copy it
straight off the docs page. To add one:

1. **Agree on the target page.** Confirm with the docs writer which
   docs.temporal.io page the sample supports. If the page already has
   placeholders, agree on the snippet names too.
2. **Branch.** Name it `docs/<short-sample-name>`.
3. **Follow the folder's existing layout.** Put the sample in the matching
   `*-sandbox/` folder and copy the structure that's already there. For
   example, a new Java sample is its own Maven module next to
   `java-sandbox/external-storage/`, and a new Python sample is flat files
   in `python-sandbox/`.
4. **Add Snipsync markers.** Wrap each block the doc page will embed (see
   [below](#snipsync-markers)).
5. **Add tests where practical.** Use the SDK's test environments where they
   exist, such as `WorkflowEnvironment`/`ActivityEnvironment` in Python, the Go
   `testsuite` package, or JUnit 5 + Mockito in Java.
6. **Validate locally.** Run the [local commands](#local-validation) for your
   language. CI only checks that the code compiles, so you have to run the
   tests yourself.
7. **Open a PR against `main`.** In the description, say what the sample
   demonstrates and paste the local commands you ran. If there's a companion PR
   in the `documentation` repo with the matching `SNIPSTART`/`SNIPEND`
   placeholders, link it. Open this PR as a draft if the doc page isn't ready
   yet.

## CI checks

Every pull request, and every push to `main`, runs the **Compile check**
workflow ([`.github/workflows/ci-on-pr.yml`](.github/workflows/ci-on-pr.yml)).
It has one job per language, and **every job runs on every PR**, whichever
folder you changed. A break in any sandbox fails the check.

| Job | Folder | What it runs |
|---|---|---|
| .NET | `dotnet-sandbox/` | `dotnet build temporaliodotnethelloworld.sln --configuration Release` (.NET 9) |
| Go | `go-sandbox/` | `go build ./...` and `go vet ./...` (Go version from `go.mod`) |
| Java | `java-sandbox/` | `mvn -B -f <module>/pom.xml test-compile -Dmaven.compiler.release=21` for **each** `*/pom.xml` (JDK 21) |
| Python | `python-sandbox/` | `python -m compileall -q -x '/\.venv/' .` (Python 3.13) |
| Ruby | `ruby-sandbox/` | `ruby -wc` on every `.rb` file (Ruby 3.3) |
| Rust | `rust-sandbox/` | `cargo check --all-targets --locked` (stable toolchain, with `protoc` installed) |
| TypeScript | `typescript-sandbox/` | `npm ci` and `npx tsc --noEmit` (Node version from `.nvmrc`) |

Things that commonly trip CI:

- **The checks compile code but don't run it.** Test files get compiled
  (Go `vet`, Java `test-compile`, Rust `--all-targets`), but no tests run.
  Python and Ruby only get a syntax check, so a bad import or a typo in a
  method name will still pass. Run the tests locally.
- **Java modules compile at release 21.** Each `java-sandbox/*/` folder is
  compiled on its own with no parent POM, and the build forces
  `maven.compiler.release=21`. Don't use language features newer than Java 21.
- **Rust needs `--locked`.** Commit your updated `Cargo.lock` along with any
  `Cargo.toml` change.
- **TypeScript needs `npm ci`.** Commit your updated `package-lock.json` along
  with any `package.json` change.
- **Ruby runs with warnings on (`-w`).** Fix any warnings `ruby -wc` reports
  locally before you push.

Pushing a new commit to a PR cancels the CI run that's still going for the
previous commit.

## Local validation

Run these in the language's folder before you open a PR, then paste them into
the PR description:

| Language | Commands |
|---|---|
| TypeScript | `npm run lint`, `npm run format:check`, `npm run build`, `npm test` |
| Go | `go test ./...` |
| Java | `mvn verify` (in each module you changed) |
| Python | `pytest` |
| Ruby | `ruby -wc <file>.rb` for each file you changed |
| Rust | `cargo build`, `cargo test` |
| .NET | `dotnet build` |

## Runnable design patterns

Design-pattern samples live in a `design-patterns/<pattern>/` folder inside
the language's sandbox, for example
[`typescript-sandbox/src/design-patterns/saga/`](typescript-sandbox/src/design-patterns/saga/).
Each one has a `runner.json` manifest that says how to run it end to end and
what result to expect. The "Run this pattern live" feature on
docs.temporal.io runs samples from these manifests, so every pattern listed
there must pass here first.

| Field | What it holds |
|---|---|
| `pattern`, `language` | Identifiers the docs site uses to find the sample |
| `workdir` | Folder the commands run in, relative to the repo root |
| `files` | Files shown to the reader, relative to `workdir` |
| `setup` | Installs dependencies |
| `worker` | Starts the Worker. It keeps running while the starter runs |
| `starter` | Starts the Workflow and exits when it finishes |
| `expect.status` | Final Workflow status: `COMPLETED`, `FAILED`, and so on |
| `expect.output` | Lines that must appear, in order, in the Worker and starter output |

The **Run design patterns** workflow
([`.github/workflows/run-design-patterns.yml`](.github/workflows/run-design-patterns.yml))
runs every manifest against a fresh Temporal dev server on pull requests that
touch a pattern, on pushes to `main`, and nightly. A nightly failure opens or
updates a tracking issue. To run one manifest locally, install the
[Temporal CLI](https://github.com/temporalio/cli/#installation) and `jq`, then
run this from the repo root:

```bash
.github/scripts/run-design-pattern.sh typescript-sandbox/src/design-patterns/saga/runner.json
```

Set `SKIP_SETUP=1` to skip the `setup` command when dependencies are already
installed.

## Snipsync markers

Snipsync uses the snippet name to match code here with a placeholder in the
docs. The `.mdx` page in the `documentation` repo has
`<!--SNIPSTART name-->` / `<!--SNIPEND-->`, and Snipsync copies the code with the
same name into it.

```java
// @@@SNIPSTART java-s3-driver-create
S3AsyncClient s3Client = S3AsyncClient.builder().region(Region.US_EAST_2).build();
// @@@SNIPEND
```

- Use `#` instead of `//` for Python and Ruby.
- Name snippets `<language>-<topic>-<detail>`, e.g. `java-s3-driver-create`.
- Names must be unique across the whole repo. Before you add one, check for
  collisions:

  ```sh
  rg '@{3}SNIPSTART' .
  ```

See [CONTRIBUTING.md](CONTRIBUTING.md) for more details.
