// @@@SNIPSTART rust-nexus-call-operation
use std::time::Duration;

use temporalio_common::protos::coresdk::AsJsonPayloadExt;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{
    ApplicationFailure, NexusOperationOptions, WorkflowContext, WorkflowContextView, WorkflowResult,
};

#[workflow]
pub struct GreetingWorkflow {
    pub name: String,
}

#[workflow_methods]
impl GreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        let nexus_started = ctx
            .start_nexus_operation(
                NexusOperationOptions::builder()
                    .endpoint("my-endpoint")
                    .service("my-service")
                    .operation("my-operation")
                    .input(name.as_json_payload().map_err(ApplicationFailure::new)?)
                    .start_to_close_timeout(Duration::from_secs(10))
                    .build(),
            )
            .await;

        let nexus_started = match nexus_started {
            Ok(started) => started,
            Err(failure) => return Ok(format!("Nexus start failed: {failure:?}")),
        };
        let nexus_result = nexus_started.result().await;

        println!("Nexus result: {:?}", nexus_result);

        Ok(format!("nexus result: {:?}", nexus_result))
    }
}
// @@@SNIPEND
