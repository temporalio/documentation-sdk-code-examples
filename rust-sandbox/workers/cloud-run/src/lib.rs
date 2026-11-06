//! Code samples for the Rust SDK "Serverless Workers on GCP Cloud Run" page.
//!
//! `src/main.rs` is the Worker. It needs a Temporal Cloud Namespace: set `TEMPORAL_ADDRESS`,
//! `TEMPORAL_NAMESPACE`, `TEMPORAL_API_KEY`, and `TEMPORAL_TASK_QUEUE`, then `cargo run`.
//! The `Dockerfile` packages it into an image.

pub mod activities;
pub mod workflows;
