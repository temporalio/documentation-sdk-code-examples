use std::{collections::HashMap, time::Duration};

use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{ActivityOptions, SignalWorkflowOptions, SyncWorkflowContext, WorkflowContext, WorkflowContextView, WorkflowResult};
use tokio::time::sleep;

use crate::{activities::{ActivityLanguages, MyActivities}};

#[derive(serde::Serialize, serde::Deserialize, Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Language {
    Chinese,
    English,
    French,
}


#[derive(serde::Serialize, serde::Deserialize)]
pub struct ApproveInput {
    pub name: String,
}
#[derive(serde::Serialize, serde::Deserialize)]
pub struct SetLanguageInput {
    pub language: Language,
}
#[derive(serde::Serialize, serde::Deserialize)]
pub struct GetLanguagesInput {
    pub include_unsupported: bool,
}

#[workflow(name = "greetings-workflow-10")]
pub struct GreetingsWorkflow {
    pub greetings: HashMap<Language, String>,
    language: Language,
    approved_for_release: bool,
    approver_name: Option<String>,
}


#[workflow_methods]
impl GreetingsWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView) -> Self {
        let mut greetings = HashMap::new();
        greetings.insert(Language::Chinese, "你好，世界".to_string());
        greetings.insert(Language::English, "Hello, world".to_string());
        
        Self {greetings, language: Language::English, approved_for_release: false, approver_name: None }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        let name = ctx.state(|s| s.greetings.clone());

        ctx.wait_condition(|s| !s.approved_for_release).await;

        // This is an example of sending a signal from within a workflow. You can trigger this signal using the Temporal CLI or another client, and it will be received and processed by the workflow while it's running.
        let signal_res = ctx.signal_workflow(SignalWorkflowOptions::new(
            "greetings-workflow-10",
            "019dbc0c-1bff-745d-8af8-eda45a598533",
            "approve",
            serde_json::to_vec(&ApproveInput {
                name: "Ziggy".to_string(),
            }),
        )).await;
        sleep(Duration::from_millis(60 * 1000)).await;

        Ok(format!("Hola: {:?}", name))
    }

    #[query]
    pub fn get_languages(&self, _ctx: &WorkflowContextView, input: GetLanguagesInput) -> Vec<Language> {
        if input.include_unsupported {
            vec![Language::Chinese, Language::English, Language::French]
        } else {
            self.greetings.keys().copied().collect()
        }
    }
    
    #[signal]
    pub fn approve(&mut self, _ctx: &mut SyncWorkflowContext<Self>, input: ApproveInput) {
        self.approved_for_release = true;
        self.approver_name = Some(input.name);
    }

    #[update]
    pub fn set_language(
        &mut self,
        _ctx: &mut SyncWorkflowContext<Self>,
        input: SetLanguageInput,
    ) -> Language {
        let previous_language = self.language;
        self.language = input.language;
        
        previous_language
    }

    // This is an example of an update method that executes an activity. When this update is triggered, it will execute the activity and wait for the result before returning a response to the caller.
    #[update]
    async fn set_language_activity(
        ctx: &mut WorkflowContext<Self>,
        language: Language,
    ) -> Language {
        let needs_greeting = ctx.state(|s| !s.greetings.contains_key(&language));

        if needs_greeting {
            // Serialize concurrent executions so updates are processed in order.
            ctx.wait_condition(|s| !s.approved_for_release).await;
            let needs_approval = ctx.state(|s| s.approved_for_release);
            while !needs_approval {
                sleep(Duration::from_secs(100));
            }
            ctx.state_mut(|s| {
                s.approved_for_release = true;
            });

            let result = async {
                let greeting = ctx.start_activity(
                    MyActivities::call_greeting_service, 
                    ActivityLanguages::French,
                    ActivityOptions::default()
                ).await;

                ctx.state_mut(|s| {
                    s.greetings.insert(language, greeting.unwrap());
                });
            }
            .await;

            ctx.state_mut(|s| {
                s.approved_for_release = false;
            });

            result;
        }

        let previous_language = ctx.state(|s| s.language);

        ctx.state_mut(|s| {
            s.language = language;
        });

        previous_language
    }

    #[update_validator(set_language)]
    fn validate_set_language(
        &self,
        _ctx: &WorkflowContextView,
        input: &SetLanguageInput,
    ) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        if !self.greetings.contains_key(&input.language) {
            Err("Not a valid language".into())
        } else {
            Ok(())
        }
    }
}

// Trigger the approve signal using the Temporal CLI:
// temporal workflow signal \
//     --workflow-id greetings-workflow-10 \
//     --name approve \
//     --input '{"name": "milk"}'

// Trigger the set_language update using the Temporal CLI:
// temporal workflow update execute \
//     --workflow-id greetings-workflow-10 \
//     --name set_language \
//     --input '{"language": "French"}'
