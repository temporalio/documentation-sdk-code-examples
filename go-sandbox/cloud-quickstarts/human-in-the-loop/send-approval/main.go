package main

import (
	"context"
	"log"
	"os"
	"strings"
	"time"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/contrib/envconfig"

	humanintheloop "my-org/quickstart/cloud-quickstarts/human-in-the-loop"
)

const usage = `Usage: go run ./send-approval <approve|reject> [notes]`

func main() {
	if len(os.Args) < 2 {
		log.Fatalln(usage)
	}
	decision := strings.ToLower(os.Args[1])
	if decision != "approve" && decision != "reject" {
		log.Fatalln(usage)
	}
	var notes string
	if len(os.Args) > 2 {
		notes = os.Args[2]
	}

	// Same environment-configuration lookup the Worker and client use.
	opts, err := envconfig.LoadDefaultClientOptions()
	if err != nil {
		log.Fatalln("Unable to load client options", err)
	}
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

	// Deliver the reviewer's decision to the waiting Workflow as a Signal. With
	// no RequestID, it applies to whichever request is pending.
	err = c.SignalWorkflow(context.Background(), humanintheloop.WorkflowID, "", humanintheloop.ApprovalSignal,
		humanintheloop.ApprovalDecision{
			Approved:      decision == "approve",
			ReviewerNotes: notes,
			DecidedAt:     time.Now().UTC().Format(time.RFC3339),
		})
	if err != nil {
		log.Fatalln("Unable to send Signal", err)
	}
	log.Printf("Sent %s decision to Workflow %s", decision, humanintheloop.WorkflowID)
}
