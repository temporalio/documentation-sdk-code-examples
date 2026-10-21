package externalstorage

import (
	commonpb "go.temporal.io/api/common/v1"
	"go.temporal.io/sdk/converter"
)

// @@@SNIPSTART go-external-storage-multi-driver-selector
type PreferredSelector struct {
	preferred converter.StorageDriver
}

func (s *PreferredSelector) SelectDriver(
	ctx converter.StorageDriverSelectContext,
	payload *commonpb.Payload,
) (converter.StorageDriver, error) {
	return s.preferred, nil
}

func MultipleDriversSetup(preferredDriver, legacyDriver converter.StorageDriver) converter.ExternalStorage {
	return converter.ExternalStorage{
		Drivers:        []converter.StorageDriver{preferredDriver, legacyDriver},
		DriverSelector: &PreferredSelector{preferred: preferredDriver},
	}
}
// @@@SNIPEND
