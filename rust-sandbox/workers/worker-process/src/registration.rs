use std::error::Error;

use temporalio_sdk::WorkerOptions;

use crate::{
    activities::GreetingActivities,
    workflows::{GreetingWorkflow, OrderWorkflow},
};

/// Worker options that register two Workflows and the Greeting Activities.
pub fn worker_options() -> Result<WorkerOptions, Box<dyn Error>> {
    // @@@SNIPSTART rust-worker-process-register
    let worker_options = WorkerOptions::new("my-task-queue")
        .register_workflow::<GreetingWorkflow>()?
        .register_workflow::<OrderWorkflow>()?
        .register_activities(GreetingActivities)
        .build();
    // @@@SNIPEND

    Ok(worker_options)
}
