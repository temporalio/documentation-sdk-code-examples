package externalstorage

import (
	"testing"

	"github.com/stretchr/testify/require"
	commonpb "go.temporal.io/api/common/v1"
	"go.temporal.io/sdk/converter"
)

func TestLocalDiskStorageDriverRoundTrip(t *testing.T) {
	driver := NewLocalDiskStorageDriver(t.TempDir())
	payload, err := converter.GetDefaultDataConverter().ToPayload("large value")
	require.NoError(t, err)

	claims, err := driver.Store(converter.StorageDriverStoreContext{
		Target: converter.StorageDriverWorkflowInfo{Namespace: "default", WorkflowID: "wf-1"},
	}, []*commonpb.Payload{payload})
	require.NoError(t, err)
	require.Len(t, claims, 1)
	require.Contains(t, claims[0].ClaimData["path"], "wf-1")

	retrieved, err := driver.Retrieve(converter.StorageDriverRetrieveContext{}, claims)
	require.NoError(t, err)
	var value string
	require.NoError(t, converter.GetDefaultDataConverter().FromPayload(retrieved[0], &value))
	require.Equal(t, "large value", value)
}

func TestPreferredSelectorAlwaysPicksPreferred(t *testing.T) {
	preferred := NewLocalDiskStorageDriver(t.TempDir())
	legacy := &namedDriver{StorageDriver: NewLocalDiskStorageDriver(t.TempDir()), name: "legacy"}
	storage := MultipleDriversSetup(preferred, legacy)
	require.Len(t, storage.Drivers, 2)

	selected, err := storage.DriverSelector.SelectDriver(converter.StorageDriverSelectContext{}, &commonpb.Payload{})
	require.NoError(t, err)
	require.Same(t, preferred, selected)
}

// namedDriver gives a second local disk driver a distinct name.
type namedDriver struct {
	converter.StorageDriver
	name string
}

func (d *namedDriver) Name() string { return d.name }
