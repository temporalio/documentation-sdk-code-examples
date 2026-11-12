package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"
	"go.temporal.io/sdk/worker"

	batchiterator "my-org/quickstart/cloud-quickstarts/batch-iterator"
)

func main() {
	// LoadDefaultClientOptions reads the TEMPORAL_PROFILE env var (default
	// "default"), loads that profile from the TOML config file (default OS config
	// dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_* overrides. No
	// connection details are hardcoded, so the same Worker runs against a local
	// dev server or Temporal Cloud.
	opts, err := envconfig.LoadDefaultClientOptions()
	if err != nil {
		log.Fatalln("Unable to load client options", err)
	}
	// With no config file and no env overrides the loaded options are empty, so
	// fall back to the local dev server address.
	if opts.HostPort == "" {
		opts.HostPort = client.DefaultHostPort
	}
	if opts.Namespace == "" {
		opts.Namespace = client.DefaultNamespace
	}

	c, err := client.Dial(opts)
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, batchiterator.TaskQueue, worker.Options{})
	w.RegisterWorkflow(batchiterator.IteratorBatchWorkflow)
	w.RegisterWorkflow(batchiterator.RecordProcessorWorkflow)
	w.RegisterActivity(batchiterator.GetRecords)

	log.Printf("Worker connected to %s (namespace: %s), polling task queue %q",
		opts.HostPort, opts.Namespace, batchiterator.TaskQueue)

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
