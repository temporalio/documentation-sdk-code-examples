// Package service is the Nexus Service contract shared by the handler and the caller.
package service

import (
	"fmt"
	"strings"
)

const HelloServiceName = "my-hello-service"

// Echo operation
const EchoOperationName = "echo"

type EchoInput struct {
	Message string
}

type EchoOutput EchoInput

// Hello operation
const HelloOperationName = "say-hello"

// HelloWithArgsOperationName starts a Workflow that takes the name and language as separate arguments.
const HelloWithArgsOperationName = "say-hello-with-args"

type Language string

const (
	EN Language = "en"
	FR Language = "fr"
	DE Language = "de"
	ES Language = "es"
	TR Language = "tr"
)

type HelloInput struct {
	Name     string
	Language Language
}

type HelloOutput struct {
	Message string
}

func HelloWorkflowID(input HelloInput) string {
	name := strings.ToLower(strings.ReplaceAll(strings.TrimSpace(input.Name), " ", "-"))
	return fmt.Sprintf("hello-%s-%s", input.Language, name)
}

// Approve operation
const ApproveOperationName = "approve"

// ApproveSignalName is the Signal the Approve operation sends to the user's approval Workflow.
const ApproveSignalName = "approve"

type ApproveInput struct {
	UserID string
}

type ApproveOutput struct{}
