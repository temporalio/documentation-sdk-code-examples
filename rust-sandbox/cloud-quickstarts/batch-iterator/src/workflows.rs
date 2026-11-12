use std::time::Duration;

use futures::future::join_all;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{
    ActivityOptions, ChildWorkflowOptions, ContinueAsNewOptions, WorkflowContext, WorkflowResult,
};

use crate::{SingleRecord, activities::RecordLoader};

/// Implements the iterator pattern.
///
/// A single Workflow run processes one page of records in parallel, starting one
/// [`RecordProcessorWorkflow`] child per record. After every child completes, the
/// Workflow continues as new with the next offset, so each run's Event History
/// stays small no matter how large the dataset is.
#[workflow]
#[derive(Default)]
pub struct IteratorBatchWorkflow;

#[workflow_methods]
impl IteratorBatchWorkflow {
    /// The input is `(page_size, offset)`: the number of records to process in a
    /// single Workflow run, and the offset of the first record (0 starts the batch).
    /// Returns the total number of processed records.
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>, input: (u32, u32)) -> WorkflowResult<u32> {
        let (page_size, offset) = input;
        let records = ctx
            .execute_activity(
                RecordLoader::get_records,
                (page_size, offset),
                ActivityOptions::start_to_close_timeout(Duration::from_secs(5)),
            )
            .await?;

        // No more records in the dataset: the batch is done.
        if records.is_empty() {
            return Ok(offset);
        }

        // Start one child per record with a human-friendly ID, e.g.
        // "batch-iterator-123/7".
        let workflow_id = ctx.workflow_id().to_string();
        let started = join_all(records.iter().map(|record| {
            ctx.start_child_workflow(
                RecordProcessorWorkflow::run,
                record.clone(),
                ChildWorkflowOptions::workflow_id(format!("{workflow_id}/{}", record.id)),
            )
        }))
        .await;
        let mut children = Vec::with_capacity(started.len());
        for child in started {
            children.push(child?);
        }
        // Wait for every child in this page. Error handling is kept minimal for brevity.
        for result in join_all(children.into_iter().map(|child| child.result())).await {
            result?;
        }

        let next = (page_size, offset + records.len() as u32);
        // continue_as_new() always returns an error that ends this run.
        match ctx.continue_as_new(next, ContinueAsNewOptions::default())? {}
    }
}

/// Child Workflow that processes a single record.
#[workflow]
#[derive(Default)]
pub struct RecordProcessorWorkflow;

#[workflow_methods]
impl RecordProcessorWorkflow {
    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>, record: SingleRecord) -> WorkflowResult<()> {
        // Simulate 1-3 seconds of work. Kept short so the whole batch finishes in
        // seconds. ctx.random() is deterministic on replay, unlike a system RNG.
        let seconds = 1 + ctx.random::<u64>() % 3;
        ctx.timer(Duration::from_secs(seconds)).await;
        // Skip the log line when replaying so each record is logged once.
        if !ctx.is_replaying() {
            println!("Processed {record:?}");
        }
        Ok(())
    }
}
