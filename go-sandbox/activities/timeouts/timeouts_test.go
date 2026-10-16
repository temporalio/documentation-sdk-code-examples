package timeouts_test

import (
	"errors"
	"testing"
	"time"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/temporal"
	"go.temporal.io/sdk/testsuite"

	"my-org/quickstart/activities/timeouts"
)

type TimeoutsTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestTimeoutsTestSuite(t *testing.T) {
	suite.Run(t, new(TimeoutsTestSuite))
}

func (s *TimeoutsTestSuite) Test_Workflows() {
	param := timeouts.YourActivityParam{Items: []string{"a", "b", "c"}}
	for _, wf := range []interface{}{
		timeouts.ActivityTimeoutWorkflow,
		timeouts.CustomRetryPolicyWorkflow,
		timeouts.HeartbeatTimeoutWorkflow,
	} {
		env := s.NewTestWorkflowEnvironment()
		env.RegisterActivity(timeouts.YourActivityDefinition)
		env.ExecuteWorkflow(wf, param)
		s.True(env.IsWorkflowCompleted())
		s.NoError(env.GetWorkflowError())

		var result timeouts.YourActivityResult
		s.NoError(env.GetWorkflowResult(&result))
		s.Equal(3, result.ProcessedCount)
	}
}

func (s *TimeoutsTestSuite) Test_YourActivityDefinitionHeartbeats() {
	env := s.NewTestActivityEnvironment()
	env.RegisterActivity(timeouts.YourActivityDefinition)
	var heartbeats int
	env.SetOnActivityHeartbeatListener(func(info *activity.Info, details converter.EncodedValues) {
		heartbeats++
	})

	_, err := env.ExecuteActivity(timeouts.YourActivityDefinition, timeouts.YourActivityParam{Items: []string{"a", "b"}})
	s.NoError(err)
	// The Worker throttles Heartbeats, so only the first one is guaranteed to be sent.
	s.GreaterOrEqual(heartbeats, 1)
}

func (s *TimeoutsTestSuite) Test_SampleActivityResumesFromHeartbeat() {
	env := s.NewTestActivityEnvironment()
	env.RegisterActivity(timeouts.SampleActivity)
	env.SetHeartbeatDetails(4)
	var reported []int
	env.SetOnActivityHeartbeatListener(func(info *activity.Info, details converter.EncodedValues) {
		var i int
		s.NoError(details.Get(&i))
		reported = append(reported, i)
	})

	_, err := env.ExecuteActivity(timeouts.SampleActivity, timeouts.InputParams{StartIndex: 0, EndIndex: 7})
	s.NoError(err)
	// The Activity resumes after the index in the last Heartbeat details.
	s.NotEmpty(reported)
	s.Equal(5, reported[0])
}

func (s *TimeoutsTestSuite) Test_NextRetryDelay() {
	env := s.NewTestActivityEnvironment()
	env.RegisterActivity(timeouts.NextRetryDelayActivity)

	_, err := env.ExecuteActivity(timeouts.NextRetryDelayActivity)
	var appErr *temporal.ApplicationError
	s.True(errors.As(err, &appErr))
	s.Equal("NextDelay", appErr.Type())
	s.Equal(3*time.Second, appErr.NextRetryDelay())
}
