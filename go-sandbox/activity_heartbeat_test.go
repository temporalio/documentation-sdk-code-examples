package quickstart_test

import (
	"context"
	"time"

	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/testsuite"
	"go.temporal.io/sdk/worker"

	"my-org/quickstart"
)

// @@@SNIPSTART go-testing-activity-heartbeat
func (s *ActivityTestSuite) Test_HeartbeatActivity() {
	testSuite := &testsuite.WorkflowTestSuite{}
	env := testSuite.NewTestActivityEnvironment()

	env.RegisterActivity(quickstart.GreetWithHeartbeat)

	var heartBeatCount int
	env.SetOnActivityHeartbeatListener(func(activityInfo *activity.Info, details converter.EncodedValues) {
		var val string
		err := details.Get(&val)
		s.NoError(err)
		s.Equal("status-report-to-workflow", val)
		heartBeatCount++
	})

	_, err := env.ExecuteActivity(quickstart.GreetWithHeartbeat, "Temporal")
	s.NoError(err)
	s.Equal(1, heartBeatCount)
}
// @@@SNIPEND

// @@@SNIPSTART go-testing-activity-cancel
func (s *ActivityTestSuite) Test_CancelActivity() {
	testSuite := &testsuite.WorkflowTestSuite{}
	env := testSuite.NewTestActivityEnvironment()

	ctx, cancel := context.WithCancel(context.Background())

	env.SetWorkerOptions(worker.Options{
		BackgroundActivityContext: ctx,
	})
	env.RegisterActivity(quickstart.LongRunningGreet)

	done := make(chan struct{})

	go func() {
		defer close(done)
		// Cancel the activity after 2s
		time.Sleep(2 * time.Second)
		cancel()
	}()

	_, err := env.ExecuteActivity(quickstart.LongRunningGreet, "Temporal")

	<-done

	// Expect the activity to return a canceled error
	s.Error(err)
}
// @@@SNIPEND
