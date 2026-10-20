package versioning_test

import (
	"context"

	"testing"

	"github.com/stretchr/testify/suite"
	"go.temporal.io/sdk/activity"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/testsuite"
	"go.temporal.io/sdk/workflow"

	"my-org/quickstart/workflows/versioning"
)

type VersioningTestSuite struct {
	suite.Suite
	testsuite.WorkflowTestSuite
}

func TestVersioningTestSuite(t *testing.T) {
	suite.Run(t, new(VersioningTestSuite))
}

func (s *VersioningTestSuite) newEnv() *testsuite.TestWorkflowEnvironment {
	env := s.NewTestWorkflowEnvironment()
	for _, a := range []interface{}{versioning.ActivityA, versioning.ActivityB, versioning.ActivityC,
		versioning.ActivityD, versioning.ActivityE, versioning.SendTextMessage, versioning.SendTweet} {
		env.RegisterActivity(a)
	}
	return env
}

// run executes wf with GetVersion for changeID mocked to return version.
// minSupported and maxSupported must match the values in the Workflow's GetVersion call.
func (s *VersioningTestSuite) run(wf interface{}, changeID string, minSupported, maxSupported, version workflow.Version) string {
	env := s.newEnv()
	if changeID != "" {
		env.OnGetVersion(changeID, minSupported, maxSupported).Return(version)
	}
	env.ExecuteWorkflow(wf, "data")
	s.True(env.IsWorkflowCompleted())
	s.NoError(env.GetWorkflowError())
	var result string
	s.NoError(env.GetWorkflowResult(&result))
	return result
}

func (s *VersioningTestSuite) Test_Original() {
	s.Equal("B:A:data", s.run(versioning.YourWorkflow, "", 0, 0, 0))
}

func (s *VersioningTestSuite) Test_EachVersionRunsItsBranch() {
	cases := []struct {
		name     string
		wf       interface{}
		changeID string
		min, max workflow.Version
		version  workflow.Version
		want     string
	}{
		{"V1 default", versioning.YourWorkflowV1, "Step1", workflow.DefaultVersion, 1, workflow.DefaultVersion, "B:A:data"},
		{"V1 v1", versioning.YourWorkflowV1, "Step1", workflow.DefaultVersion, 1, 1, "B:C:data"},
		{"V2 default", versioning.YourWorkflowV2, "Step1", workflow.DefaultVersion, 2, workflow.DefaultVersion, "B:A:data"},
		{"V2 v1", versioning.YourWorkflowV2, "Step1", workflow.DefaultVersion, 2, 1, "B:C:data"},
		{"V2 v2", versioning.YourWorkflowV2, "Step1", workflow.DefaultVersion, 2, 2, "B:D:data"},
		{"V3 v1", versioning.YourWorkflowV3, "Step1", 1, 2, 1, "B:C:data"},
		{"V3 v2", versioning.YourWorkflowV3, "Step1", 1, 2, 2, "B:D:data"},
		{"V4 v2", versioning.YourWorkflowV4, "Step1", 2, 2, 2, "B:D:data"},
		{"V5 default", versioning.YourWorkflowV5, "Step1-fix2", workflow.DefaultVersion, 1, workflow.DefaultVersion, "B:D:data"},
		{"V5 v1", versioning.YourWorkflowV5, "Step1-fix2", workflow.DefaultVersion, 1, 1, "B:E:data"},
	}
	for _, c := range cases {
		s.Run(c.name, func() {
			s.Equal(c.want, s.run(c.wf, c.changeID, c.min, c.max, c.version))
		})
	}
}

func (s *VersioningTestSuite) Test_NewExecutionsGetMaxVersion() {
	// Without a mock, a new Workflow Execution records the maxSupported version.
	s.Equal("B:D:data", s.run(versioning.YourWorkflowV2, "", 0, 0, 0))
}

func (s *VersioningTestSuite) Test_NotificationWorkflow() {
	for version, activityName := range map[workflow.Version]string{2: "SendTextMessage", 3: "SendTweet"} {
		env := s.newEnv()
		env.OnGetVersion("ChangedNotificationActivityType", 2, 3).Return(version)
		called := ""
		env.SetOnActivityStartedListener(func(info *activity.Info, ctx context.Context, args converter.EncodedValues) {
			called = info.ActivityType.Name
		})
		env.ExecuteWorkflow(versioning.NotificationWorkflow)
		s.NoError(env.GetWorkflowError())
		s.Equal(activityName, called)
	}
}
