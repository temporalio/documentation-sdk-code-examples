package main

import (
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
	"go.temporal.io/sdk/workflow"
)

func HelloWorkflow(ctx workflow.Context, name string) (string, error) {
	return "Hello " + name, nil
}

func main() {
	c, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	// @@@SNIPSTART go-worker-versioned
	w := worker.New(c, "my-task-queue", worker.Options{
		DeploymentOptions: worker.DeploymentOptions{
			UseVersioning: true,
			Version: worker.WorkerDeploymentVersion{
				DeploymentName: "my-app",
				BuildID:        "1.0",
			},
		},
	})

	w.RegisterWorkflowWithOptions(HelloWorkflow, workflow.RegisterOptions{
		VersioningBehavior: workflow.VersioningBehaviorPinned,
	})
	// @@@SNIPEND

	err = w.Run(worker.InterruptCh())
	if err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
