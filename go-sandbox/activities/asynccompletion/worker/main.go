package main

import (
	"context"
	"encoding/base64"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart/activities/asynccompletion"
)

// logExternalSystem prints the Task Token so you can pass it to the completer program.
type logExternalSystem struct{}

func (logExternalSystem) Submit(ctx context.Context, taskToken []byte, request asynccompletion.ApprovalRequest) error {
	log.Printf("Approval requested for order %s. Task Token: %s", request.OrderID, base64.StdEncoding.EncodeToString(taskToken))
	return nil
}

func main() {
	temporalClient, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer temporalClient.Close()

	w := worker.New(temporalClient, asynccompletion.TaskQueue, worker.Options{})
	w.RegisterWorkflow(asynccompletion.ApprovalWorkflow)
	w.RegisterActivity(&asynccompletion.Activities{External: logExternalSystem{}})

	if err := w.Run(worker.InterruptCh()); err != nil {
		log.Fatalln("Unable to start Worker", err)
	}
}
