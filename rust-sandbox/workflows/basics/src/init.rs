use serde::{Deserialize, Serialize};
use std::time::SystemTime;
use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{WorkflowContext, WorkflowContextView, WorkflowResult};

#[derive(Serialize, Deserialize)]
pub struct PersonInput {
    pub name: String,
    pub age: u32,
}

#[workflow]
pub struct PersonWorkflow {
    name: String,
    age: u32,
    started_at: Option<SystemTime>,
}

#[workflow_methods]
impl PersonWorkflow {
    // @@@SNIPSTART rust-workflow-basics-init
    #[init]
    fn new(ctx: &WorkflowContextView, input: PersonInput) -> Self {
        Self {
            name: input.name,
            age: input.age,
            started_at: ctx.start_time(),
        }
    }
    // @@@SNIPEND

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let (name, age, has_start_time) =
            ctx.state(|s| (s.name.clone(), s.age, s.started_at.is_some()));
        Ok(format!("{name} is {age}; start time recorded: {has_start_time}"))
    }
}
