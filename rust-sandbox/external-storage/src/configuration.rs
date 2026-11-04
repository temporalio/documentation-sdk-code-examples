use std::sync::Arc;
use temporalio_common::{
    data_converters::{
        DataConverter, ExternalStorage, ExternalStorageConfigError, StorageDriver,
        StorageDriverSelectContext, StorageDriverSelector,
    },
    protos::temporal::api::common::v1::Payload,
};

pub fn converter(
    driver: Arc<dyn StorageDriver>,
) -> Result<DataConverter, ExternalStorageConfigError> {
    // @@@SNIPSTART rust-external-storage-threshold
    let storage = ExternalStorage::new(driver)?.with_payload_size_threshold(256 * 1024);
    let data_converter = DataConverter::default().with_external_storage(storage);
    // @@@SNIPEND
    Ok(data_converter)
}

// @@@SNIPSTART rust-external-storage-selector
struct PreferDriver(Arc<dyn StorageDriver>);

impl StorageDriverSelector for PreferDriver {
    fn select_driver(
        &self,
        _context: &StorageDriverSelectContext,
        _payload: &Payload,
    ) -> Option<Arc<dyn StorageDriver>> {
        Some(self.0.clone())
    }
}

pub fn migration_storage(
    preferred: Arc<dyn StorageDriver>,
    legacy: Arc<dyn StorageDriver>,
) -> Result<ExternalStorage, ExternalStorageConfigError> {
    ExternalStorage::with_selector(
        vec![preferred.clone(), legacy],
        Arc::new(PreferDriver(preferred)),
    )
}
// @@@SNIPEND
