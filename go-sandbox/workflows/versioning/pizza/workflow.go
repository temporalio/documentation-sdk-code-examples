package pizza

import "go.temporal.io/sdk/workflow"

type PizzaOrder struct {
	OrderNumber string
}

type OrderConfirmation struct {
	OrderNumber string
	Status      string
}

// @@@SNIPSTART go-versioning-cutover
func PizzaWorkflow(ctx workflow.Context, order PizzaOrder) (OrderConfirmation, error) {
	// this function contains the original code
	return OrderConfirmation{OrderNumber: order.OrderNumber, Status: "SUCCESS"}, nil
}

func PizzaWorkflowV2(ctx workflow.Context, order PizzaOrder) (OrderConfirmation, error) {
	// this function contains the updated code
	return OrderConfirmation{OrderNumber: order.OrderNumber, Status: "CONFIRMED"}, nil
}
// @@@SNIPEND
