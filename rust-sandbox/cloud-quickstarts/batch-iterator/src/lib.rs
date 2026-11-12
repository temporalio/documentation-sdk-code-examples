//! A minimal sample of the iterator batch-processing pattern.
//!
//! The parent Workflow processes one page of records with child Workflows, then
//! continues as new with the next offset.

pub mod activities;
pub mod workflows;

use serde::{Deserialize, Serialize};

/// Records loaded and processed per Workflow run (one page).
pub const PAGE_SIZE: u32 = 5;

/// The Task Queue the Worker polls and the client targets. Keep the Worker and
/// client pointed at the same value.
pub fn task_queue() -> String {
    std::env::var("TEMPORAL_TASK_QUEUE")
        .ok()
        .filter(|tq| !tq.is_empty())
        .unwrap_or_else(|| "quickstart-batch-iterator".to_string())
}

/// One record in the dataset being processed.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct SingleRecord {
    pub id: u32,
}
