//! Runs the Workflow's paths against a local dev server, with the OpenAI call
//! mocked so the tests need no API key.
//!
//! Set TEMPORAL_CLI_PATH to use an installed Temporal CLI instead of downloading one.

use std::time::Duration;

use human_in_the_loop::{
    ApprovalDecision, ApprovalRequest, ProposedAction, WorkflowInput, activities::AgentActivities,
    workflows::HumanInTheLoopWorkflow,
};
use temporalio_client::{WorkflowGetResultOptions, WorkflowSignalOptions, WorkflowStartOptions};
use temporalio_sdk::{
    Runtime, Worker, WorkerOptions,
    testing::{EphemeralExe, LocalWorkflowEnvironmentOptions, WorkflowEnvironment},
};
use tokio::sync::{Mutex, mpsc};

fn action(action_type: &str, risky_action: bool) -> ProposedAction {
    ProposedAction {
        action_type: action_type.to_string(),
        description: "test action".to_string(),
        reasoning: "test reasoning".to_string(),
        risky_action,
    }
}

mod mock {
    // The Workflow calls these Activities by name, so the handles the macro
    // generates for them go unused.
    #![allow(dead_code)]

    use std::sync::Arc;

    use human_in_the_loop::{ApprovalRequest, OpenAIResponsesRequest, ProposedAction};
    use temporalio_macros::activities;
    use temporalio_sdk::activities::{ActivityContext, ActivityError};
    use tokio::sync::mpsc;

    /// Stand-ins for the real Activities, registered under the same names.
    pub struct MockActivities {
        pub action: ProposedAction,
        pub requests: mpsc::UnboundedSender<ApprovalRequest>,
    }

    #[activities]
    impl MockActivities {
        #[activity(name = "propose_action")]
        async fn propose_action(
            self: Arc<Self>,
            _ctx: ActivityContext,
            _request: OpenAIResponsesRequest,
        ) -> Result<ProposedAction, ActivityError> {
            Ok(self.action.clone())
        }

        #[activity(name = "notify_approval_needed")]
        async fn notify_approval_needed(
            self: Arc<Self>,
            _ctx: ActivityContext,
            request: ApprovalRequest,
        ) -> Result<(), ActivityError> {
            let _ = self.requests.send(request);
            Ok(())
        }

        #[activity(name = "execute_action")]
        async fn execute_action(
            _ctx: ActivityContext,
            action: ProposedAction,
        ) -> Result<String, ActivityError> {
            Ok(format!("Executed: {}", action.action_type))
        }
    }
}

use mock::MockActivities;

/// Runs the Workflow on a Worker with the given Activities. If `decide` is set,
/// sends it as the reviewer's `(approved, notes)` once approval is requested.
async fn run_workflow<A>(
    activities: A,
    requests: Option<mpsc::UnboundedReceiver<ApprovalRequest>>,
    timeout_seconds: u64,
    decide: Option<(bool, &str)>,
    with_request_id: bool,
) -> String
where
    A: temporalio_sdk::activities::ActivityImplementer + Send + Sync + 'static,
{
    let mut options = LocalWorkflowEnvironmentOptions::default();
    if let Ok(path) = std::env::var("TEMPORAL_CLI_PATH") {
        options.server_executable = EphemeralExe::ExistingPath(path);
    }
    let env = WorkflowEnvironment::start_local(options).await.unwrap();
    let client = env.client().clone();
    let task_queue = "human-in-the-loop-test";

    let runtime = Runtime::from_current_tokio(Default::default()).unwrap();
    let worker_options = WorkerOptions::new(task_queue)
        .register_activities(activities)
        .register_workflow::<HumanInTheLoopWorkflow>()
        .unwrap()
        .build();
    let mut worker = Worker::new(&runtime, client.clone(), worker_options).unwrap();
    let shutdown = worker.shutdown_handle();
    let requests = Mutex::new(requests);

    let (result, run) = tokio::join!(
        async {
            let handle = client
                .start_workflow(
                    HumanInTheLoopWorkflow::run,
                    WorkflowInput {
                        user_request: "test".to_string(),
                        approval_timeout_seconds: timeout_seconds,
                    },
                    WorkflowStartOptions::new(task_queue, "human-in-the-loop-test").build(),
                )
                .await
                .unwrap();

            if let Some((approved, notes)) = decide {
                let mut requests = requests.lock().await;
                let request = tokio::time::timeout(
                    Duration::from_secs(10),
                    requests.as_mut().unwrap().recv(),
                )
                .await
                .expect("approval was never requested")
                .unwrap();
                handle
                    .signal(
                        HumanInTheLoopWorkflow::approval_decision,
                        ApprovalDecision {
                            approved,
                            request_id: with_request_id.then_some(request.request_id),
                            reviewer_notes: Some(notes.to_string()),
                            decided_at: Some("2026-01-01T00:00:00Z".to_string()),
                        },
                        WorkflowSignalOptions::default(),
                    )
                    .await
                    .unwrap();
            }
            let result = handle
                .get_result(WorkflowGetResultOptions::default())
                .await
                .unwrap();
            shutdown();
            result
        },
        worker.run()
    );
    run.unwrap();
    env.shutdown().await.unwrap();
    result
}

/// Runs the Workflow with mocked Activities that propose `action`.
async fn run_mocked(
    action: ProposedAction,
    timeout_seconds: u64,
    decide: Option<(bool, &str)>,
    with_request_id: bool,
) -> String {
    let (sender, receiver) = mpsc::unbounded_channel();
    let mocks = MockActivities {
        action,
        requests: sender,
    };
    run_workflow(
        mocks,
        Some(receiver),
        timeout_seconds,
        decide,
        with_request_id,
    )
    .await
}

#[tokio::test]
async fn safe_action_is_auto_approved() {
    let result = run_mocked(action("list_files", false), 30, None, true).await;
    assert_eq!(
        result,
        "Action completed successfully (auto-approved): Executed: list_files"
    );
}

#[tokio::test]
async fn risky_action_runs_when_approved() {
    let result = run_mocked(
        action("delete_data", true),
        300,
        Some((true, "Looks good")),
        true,
    )
    .await;
    assert_eq!(
        result,
        "Action completed successfully: Executed: delete_data"
    );
}

#[tokio::test]
async fn decision_without_request_id_applies_to_pending_request() {
    let result = run_mocked(
        action("delete_data", true),
        300,
        Some((true, "Looks good")),
        false,
    )
    .await;
    assert_eq!(
        result,
        "Action completed successfully: Executed: delete_data"
    );
}

#[tokio::test]
async fn risky_action_is_rejected() {
    let result = run_mocked(
        action("delete_data", true),
        300,
        Some((false, "Too risky")),
        true,
    )
    .await;
    assert_eq!(result, "Action rejected. Reviewer notes: Too risky");
}

#[tokio::test]
async fn risky_action_times_out_without_a_decision() {
    let result = run_mocked(action("delete_data", true), 1, None, true).await;
    assert!(result.contains("timed out after 1 seconds"), "{result}");
}

/// Runs the real Activities with no API key: the mock proposed action is risky,
/// so the Workflow waits for approval and times out. Skipped when
/// OPENAI_API_KEY is set, so it never calls OpenAI.
#[tokio::test]
async fn propose_action_uses_risky_mock_without_api_key() {
    if std::env::var("OPENAI_API_KEY").is_ok_and(|key| !key.is_empty()) {
        eprintln!("skipped: OPENAI_API_KEY is set");
        return;
    }
    let result = run_workflow(AgentActivities, None, 1, None, true).await;
    assert!(result.contains("timed out after 1 seconds"), "{result}");
}
