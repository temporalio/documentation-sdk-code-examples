// @@@SNIPSTART go-nexus-quickstart-worker
package main

import (
	"log"

	"my-org/quickstart"

	"github.com/nexus-rpc/sdk-go/nexus"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/worker"
)

func main() {
	// Empty Options defaults to the "default" Namespace.
	c, err := client.Dial(client.Options{})
	if err != nil {
		log.Fatalln("Unable to create client", err)
	}
	defer c.Close()

	w := worker.New(c, "my-task-queue", worker.Options{})
	w.RegisterWorkflow(quickstart.SayHelloWorkflow)
	w.RegisterWorkflow(quickstart.HelloNexusWorkflow)
	w.RegisterActivity(quickstart.Greet)

	service := nexus.NewService(quickstart.HelloServiceName)
	err = service.Register(quickstart.HelloOperation)
	if err != nil {
		log.Fatalln("Unable to register operations", err)
	}
	w.RegisterNexusService(service)

	err = w.Run(worker.InterruptCh())
	if err != nil {
		log.Fatalln("Unable to start worker", err)
	}
}
// @@@SNIPEND
