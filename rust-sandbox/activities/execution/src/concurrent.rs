// @@@SNIPSTART rust-activity-execution-concurrent
use std::time::Duration;

use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::workflows::join;
use temporalio_sdk::{ActivityOptions, WorkflowContext, WorkflowContextView, WorkflowResult};

use crate::activities::{ActivityLanguages, MyActivities};

#[workflow]
pub struct ConcurrentGreetingWorkflow {
    name: String,
}

#[workflow_methods]
impl ConcurrentGreetingWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView, name: String) -> Self {
        Self { name }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.name.clone());
        // Start both Activities without awaiting them yet
        let greeting = ctx.execute_activity(
            MyActivities::greet,
            name,
            ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
        );

        let language = ctx.execute_activity(
            MyActivities::call_greeting_service,
            ActivityLanguages::English,
            ActivityOptions::start_to_close_timeout(Duration::from_secs(30)),
        );

        // Run in parallel
        let (greeting_res, language_res) = join!(greeting, language);

        Ok(format!("{} ({})", greeting_res?, language_res?))
    }
}
// @@@SNIPEND
