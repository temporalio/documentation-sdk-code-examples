package interceptors

import (
	"context"
	"log"

	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/interceptor"
)

// @@@SNIPSTART go-interceptor-client
type loggingClientInterceptor struct {
	interceptor.ClientInterceptorBase
}

func (loggingClientInterceptor) InterceptClient(
	next interceptor.ClientOutboundInterceptor,
) interceptor.ClientOutboundInterceptor {
	return &loggingClientOutboundInterceptor{
		ClientOutboundInterceptorBase: interceptor.ClientOutboundInterceptorBase{Next: next},
	}
}

type loggingClientOutboundInterceptor struct {
	interceptor.ClientOutboundInterceptorBase
}

func (l *loggingClientOutboundInterceptor) ExecuteWorkflow(
	ctx context.Context,
	in *interceptor.ClientExecuteWorkflowInput,
) (client.WorkflowRun, error) {
	log.Printf("starting workflow %q (%s)", in.Options.ID, in.WorkflowType)
	return l.Next.ExecuteWorkflow(ctx, in)
}
// @@@SNIPEND

// NewLoggingClientInterceptor returns a Client interceptor that logs each Workflow start.
func NewLoggingClientInterceptor() interceptor.ClientInterceptor {
	return &loggingClientInterceptor{}
}

// Dial creates a Temporal Client that logs each Workflow start.
func Dial() (client.Client, error) {
	// @@@SNIPSTART go-interceptor-client-register
	c, err := client.Dial(client.Options{
		Interceptors: []interceptor.ClientInterceptor{&loggingClientInterceptor{}},
	})
	// @@@SNIPEND
	return c, err
}
