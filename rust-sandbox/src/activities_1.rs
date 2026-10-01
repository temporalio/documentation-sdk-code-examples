use temporalio_macros::{activities};
use temporalio_sdk::{ApplicationFailure, activities::{ActivityContext, ActivityError}};
use std::{sync::{Arc, atomic::{AtomicUsize, Ordering}}};

pub struct TestGreetActivities {
    counter: AtomicUsize,
}

#[activities]
impl TestGreetActivities {
    #[activity]
    pub async fn greet(ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        ctx.record_heartbeat("greet activity started".to_string())
            .await?;

        if name == "ziggy" {
            return Err(ApplicationFailure::new("Ziggy is not a valid name").into());
        }

        Ok(format!("Hello, {}!", name))
    }

    // Activities can also use shared state via Arc<Self>
    #[activity]
    pub async fn increment(self: Arc<Self>, _ctx: ActivityContext) -> Result<u32, ActivityError> {
        Ok(self.counter.fetch_add(1, Ordering::Relaxed) as u32)
    }
}