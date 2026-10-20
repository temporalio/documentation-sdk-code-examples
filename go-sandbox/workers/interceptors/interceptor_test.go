package interceptors

import (
	"bytes"
	"context"
	"log"
	"os"
	"strings"
	"testing"

	"github.com/stretchr/testify/require"
	"go.temporal.io/sdk/client"
	"go.temporal.io/sdk/interceptor"
	"go.temporal.io/sdk/mocks"
)

// recordingNext stands in for the rest of the interceptor chain.
type recordingNext struct {
	interceptor.ClientOutboundInterceptor
	called bool
}

func (r *recordingNext) ExecuteWorkflow(ctx context.Context, in *interceptor.ClientExecuteWorkflowInput) (client.WorkflowRun, error) {
	r.called = true
	return &mocks.WorkflowRun{}, nil
}

func TestLoggingClientInterceptorLogsAndForwards(t *testing.T) {
	var buf bytes.Buffer
	log.SetOutput(&buf)
	defer log.SetOutput(os.Stderr)

	next := &recordingNext{}
	outbound := loggingClientInterceptor{}.InterceptClient(next)
	_, err := outbound.ExecuteWorkflow(context.Background(), &interceptor.ClientExecuteWorkflowInput{
		Options:      &client.StartWorkflowOptions{ID: "order-123"},
		WorkflowType: "OrderWorkflow",
	})

	require.NoError(t, err)
	require.True(t, next.called, "the interceptor must forward to Next")
	require.True(t, strings.Contains(buf.String(), `starting workflow "order-123" (OrderWorkflow)`), buf.String())
}
