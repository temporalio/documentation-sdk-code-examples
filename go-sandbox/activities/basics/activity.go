// @@@SNIPSTART go-activities-basics
package activities

import (
	"context"

	"go.temporal.io/sdk/activity"
)

func YourSimpleActivityDefinition(ctx context.Context) error {
    return nil
}

type YourActivityParam struct {
	ActivityParamX string
	ActivityParamY int
}

type YourActivityObject struct {
    Message *string
    Number  *int
}

type YourActivityResultObject struct {
	ResultFieldX string
	ResultFieldY int
}

func (a *YourActivityObject) YourActivityDefinition(ctx context.Context, param YourActivityParam) (*YourActivityResultObject, error) {
    // Use Activities for calling external APIs.
    // This is just an example of using the logger to print "Hello World!"
    logger := activity.GetLogger(ctx)
    logger.Info("The message is:", param.ActivityParamX)
    logger.Info("The number is:", param.ActivityParamY)
    // Return data using a Struct so that the function signature is backward compatible.
    result := &YourActivityResultObject{
        ResultFieldX: "Success",
        ResultFieldY: 1,
    }
    // Return the results back to the Workflow Execution.
    // The results persist within the Event History of the Workflow Execution.
    return result, nil
}
// @@@SNIPEND