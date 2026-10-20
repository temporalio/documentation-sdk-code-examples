package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/workers/sessions"
)

// @@@SNIPSTART go-session-worker
func main() {
	// The client and worker are heavyweight objects that should be created once per process.
	temporalClient, err := client.Dial(client.Options{
		HostPort: client.DefaultHostPort,
	})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	workerOptions := worker.Options{
		EnableSessionWorker:               true,
		MaxConcurrentSessionExecutionSize: 1000,
	}
	w := worker.New(temporalClient, "fileprocessing", workerOptions)
	w.RegisterWorkflow(sessions.SomeFileProcessingWorkflow)
	w.RegisterActivity(&sessions.FileActivities{})

	err = w.Run(worker.InterruptCh())
	if err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
// @@@SNIPEND
