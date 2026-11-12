use temporalio_macros::activities;
use temporalio_sdk::activities::{ActivityContext, ActivityError};

use crate::SingleRecord;

/// The dataset is always 3 pages long.
pub const PAGE_COUNT: u32 = 3;

/// Generates records in memory so the sample needs no external storage. A real
/// application would page through a database table, a file, or an object store here.
pub struct RecordLoader;

#[activities]
impl RecordLoader {
    /// Returns the next page of records, or an empty list when there are none left.
    /// The input is `(page_size, offset)`.
    #[activity]
    pub async fn get_records(
        _ctx: ActivityContext,
        input: (u32, u32),
    ) -> Result<Vec<SingleRecord>, ActivityError> {
        let (page_size, offset) = input;
        if offset >= page_size * PAGE_COUNT {
            return Ok(Vec::new());
        }
        Ok((offset..offset + page_size)
            .map(|id| SingleRecord { id })
            .collect())
    }
}
