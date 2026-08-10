use temporalio_macros::{activities};
use temporalio_sdk::activities::{ActivityContext, ActivityError};
use std::sync::{Arc, atomic::{AtomicUsize, Ordering}};

struct TestGreetActivities {
    counter: AtomicUsize,
}

#[activities]
impl TestGreetActivities {
    #[activity]
    pub async fn greet(ctx: ActivityContext, name: String) -> Result<String, ActivityError> {
        ctx.record_heartbeat(vec!["greet activity started".into()]);

        if name == "ziggy" {
            return Err(ActivityError::Retryable {
                source: "Ziggy is not a valid name".into(),
                // next retry will be after 5 seconds
                explicit_delay: Some(std::time::Duration::from_secs(5)),
            });
        }
        
        Ok(format!("Hello, {}!", name))
    }

    // Activities can also use shared state via Arc<Self>
    #[activity]
    pub async fn increment(self: Arc<Self>, _ctx: ActivityContext) -> Result<u32, ActivityError> {
        Ok(self.counter.fetch_add(1, Ordering::Relaxed) as u32)
    }
}