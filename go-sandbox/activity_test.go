package greeting

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/testsuite"
	"go.temporal.io/sdk/worker"
)

type ActivityTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestActivityTestSuite(t *testing.T) {
	suite.Run(t, new(ActivityTestSuite))
}

// func (s *ActivityTestSuite) TestGreet() {
// 	env := s.NewTestActivityEnvironment()
// 	env.RegisterActivity(Greet)

// 	result, err := env.ExecuteActivity(Greet, "Temporal")

// 	s.NoError(err)

// 	var greeting string
// 	s.NoError(result.Get(&greeting))
// 	s.Equal("Hello Temporal", greeting)
// }

func (s *ActivityTestSuite) Test_HeatbeatActivity() {
	testSuite := &testsuite.WorkflowTestSuite{}
	env := testSuite.NewTestActivityEnvironment()

	env.RegisterActivity(Greet)
	var heartBeatCount int
	env.SetOnActivityHeartbeatListener(func(activityInfo *activity.Info, details converter.EncodedValues) {
		var val string
		err := details.Get(&val)
		s.NoError(err)
		s.Equal("status-report-to-workflow", val)
		heartBeatCount++
	})

	_, err := env.ExecuteActivity(Greet, "Temporal")
	s.NoError(err)
	s.Equal(1, heartBeatCount)
}

func (s *UnitTestSuite) Test_CancelActivity() {
    testSuite := &testsuite.WorkflowTestSuite{}
    env := testSuite.NewTestActivityEnvironment()

    ctx, cancel := context.WithCancel(context.Background())

    env.SetWorkerOptions(worker.Options{
        BackgroundActivityContext: ctx,
    })
    env.RegisterActivity(Greet)

    done := make(chan struct{})

    go func() {
        defer close(done)
        // Cancel the activity after 5s
        time.Sleep(5 * time.Second)
        cancel()
    }()

    _, err := env.ExecuteActivity(Greet, "Temporal")

    <-done
    
    // Expect the activity to return a cancled error
    s.Error(err)
}