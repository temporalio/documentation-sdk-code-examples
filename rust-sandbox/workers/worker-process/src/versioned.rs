use std::error::Error;

use temporalio_sdk::WorkerOptions;

use crate::{activities::GreetingActivities, workflows::GreetingWorkflow};

// @@@SNIPSTART rust-worker-process-versioned
use temporalio_common::worker::{
    VersioningBehavior, WorkerDeploymentOptions, WorkerDeploymentVersion,
};

/// Worker options for a versioned Worker that pins its Workflows to Build ID 1.0.
pub fn versioned_worker_options() -> Result<WorkerOptions, Box<dyn Error>> {
    let worker_options = WorkerOptions::new("my-task-queue")
        .deployment_options(
            WorkerDeploymentOptions::new(
                WorkerDeploymentVersion::builder()
                    .deployment_name("my-app")
                    .build_id("1.0")
                    .build(),
            )
            .use_worker_versioning(true)
            .default_versioning_behavior(VersioningBehavior::Pinned)
            .build(),
        )
        .register_workflow::<GreetingWorkflow>()?
        .register_activities(GreetingActivities)
        .build();

    Ok(worker_options)
}
// @@@SNIPEND
