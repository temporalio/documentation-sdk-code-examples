//! The operations an `ActivityHandle` supports, shown together for reference.

use std::error::Error;

use temporalio_client::{ActivityHandle, Client};
use temporalio_common::ActivityDefinition;

/// Waits for, describes, cancels, and terminates the Activity behind `handle`.
#[rustfmt::skip] // keeps the trailing comments in the snippet aligned
pub async fn handle_operations<A: ActivityDefinition>(
    handle: &ActivityHandle<Client, A>,
) -> Result<(), Box<dyn Error>> {
    // @@@SNIPSTART rust-standalone-activities-handle-operations
    handle.result().await?;                            // block until the activity completes
    handle.describe(Default::default()).await?;        // status, timestamps, attempt, ...
    handle.cancel(Default::default()).await?;          // request cancellation
    handle.terminate(Default::default()).await?;       // force-close the activity
    // @@@SNIPEND

    Ok(())
}
