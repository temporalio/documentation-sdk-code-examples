package externalstorage

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/worker"
)

// RunWorker connects a Client that offloads large Payloads with driver, and runs a Worker created from it.
func RunWorker(driver converter.StorageDriver) {
	// @@@SNIPSTART go-external-storage-setup
	c, err := client.Dial(client.Options{
		HostPort: "localhost:7233",
		ExternalStorage: converter.ExternalStorage{
			Drivers: []converter.StorageDriver{driver},
		},
	})
	if err != nil {
		log.Fatalf("connect to Temporal: %v", err)
	}
	defer c.Close()

	w := worker.New(c, "my-task-queue", worker.Options{})
	// @@@SNIPEND
	w.RegisterWorkflow(LargePayloadWorkflow)
	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalf("run Worker: %v", err)
	}
}

// DialWithLocalDiskDriver connects a Client that offloads large Payloads to local disk.
func DialWithLocalDiskDriver() (client.Client, error) {
	// @@@SNIPSTART go-external-storage-custom-driver-client
	c, err := client.Dial(client.Options{
		ExternalStorage: converter.ExternalStorage{
			Drivers: []converter.StorageDriver{NewLocalDiskStorageDriver("/tmp/temporal-payload-store")},
		},
	})
	// @@@SNIPEND
	return c, err
}

// DialWithThreshold connects a Client that offloads every Payload, whatever its size.
func DialWithThreshold(driver converter.StorageDriver) (client.Client, error) {
	// @@@SNIPSTART go-external-storage-size-threshold
	c, err := client.Dial(client.Options{
		ExternalStorage: converter.ExternalStorage{
			Drivers:              []converter.StorageDriver{driver},
			PayloadSizeThreshold: 1,
		},
	})
	// @@@SNIPEND
	return c, err
}
