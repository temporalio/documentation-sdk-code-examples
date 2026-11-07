# Set up

- Run `cargo update`
- Open a terminal and run: `temporal server start-dev`
- Open a different terminal and run: `cargo run`

## Quickstart

`quickstart/` is a standalone crate with the code from the [Rust quickstart](https://docs.temporal.io/develop/rust/quickstart).

- Open a terminal and run: `temporal server start-dev`
- Open a different terminal, `cd quickstart`, and run: `cargo run`
- Start the Workflow: `temporal workflow start --type GreetingWorkflow --task-queue my-task-queue --input '"Ziggy"'`
