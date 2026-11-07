// @@@SNIPSTART rust-message-passing-workflow
use std::{collections::HashMap, time::Duration};

use temporalio_macros::{workflow, workflow_methods};
use temporalio_sdk::{
    ActivityOptions, SyncWorkflowContext, WorkflowContext, WorkflowContextView, WorkflowResult,
};

use crate::activities::GreetingActivities;

#[derive(serde::Serialize, serde::Deserialize, Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum Language {
    Arabic,
    Chinese,
    English,
    French,
    Hindi,
    Spanish,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub struct GetLanguagesInput {
    pub include_unsupported: bool,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub struct ApproveInput {
    pub name: String,
}

#[derive(serde::Serialize, serde::Deserialize)]
pub struct SetLanguageInput {
    pub language: Language,
}

#[workflow]
pub struct GreetingsWorkflow {
    greetings: HashMap<Language, String>,
    language: Language,
    approved_for_release: bool,
    approver_name: Option<String>,
    greeting_service_busy: bool,
}

#[workflow_methods]
impl GreetingsWorkflow {
    #[init]
    fn new(_ctx: &WorkflowContextView) -> Self {
        let greetings = HashMap::from([
            (Language::Chinese, "你好，世界".to_string()),
            (Language::English, "Hello, world".to_string()),
        ]);

        Self {
            greetings,
            language: Language::English,
            approved_for_release: false,
            approver_name: None,
            greeting_service_busy: false,
        }
    }

    #[run]
    pub async fn run(ctx: &mut WorkflowContext<Self>) -> WorkflowResult<String> {
        // Wait for the approve Signal.
        ctx.wait_condition(|s| s.approved_for_release).await?;

        // Wait for running Signal and Update handlers to finish.
        ctx.wait_condition(|_| ctx.all_handlers_finished()).await?;

        Ok(ctx.state(|s| s.greetings[&s.language].clone()))
    }

    #[query]
    pub fn get_languages(
        &self,
        _ctx: &WorkflowContextView,
        input: GetLanguagesInput,
    ) -> Vec<Language> {
        if input.include_unsupported {
            vec![
                Language::Arabic,
                Language::Chinese,
                Language::English,
                Language::French,
                Language::Hindi,
                Language::Spanish,
            ]
        } else {
            let mut languages: Vec<Language> = self.greetings.keys().copied().collect();
            languages.sort_by_key(|language| format!("{language:?}"));
            languages
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

    #[update]
    pub async fn set_language_activity(
        ctx: &mut WorkflowContext<Self>,
        input: SetLanguageInput,
    ) -> Result<Language, Box<dyn std::error::Error + Send + Sync>> {
        let language = input.language;
        let needs_greeting = ctx.state(|s| !s.greetings.contains_key(&language));

        if needs_greeting {
            // Let one handler call the greeting service at a time.
            ctx.wait_condition(|s| !s.greeting_service_busy).await?;
            ctx.state_mut(|s| s.greeting_service_busy = true);

            let greeting = ctx
                .execute_activity(
                    GreetingActivities::call_greeting_service,
                    language,
                    ActivityOptions::start_to_close_timeout(Duration::from_secs(10)),
                )
                .await;

            ctx.state_mut(|s| s.greeting_service_busy = false);

            let Some(greeting) = greeting? else {
                return Err("The greeting service doesn't support this language".into());
            };
            ctx.state_mut(|s| {
                s.greetings.insert(language, greeting);
            });
        }

        let previous_language = ctx.state(|s| s.language);
        ctx.state_mut(|s| s.language = language);

        Ok(previous_language)
    }
}
// @@@SNIPEND
