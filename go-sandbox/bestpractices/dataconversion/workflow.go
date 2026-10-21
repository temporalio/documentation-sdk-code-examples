package dataconversion

import "go.temporal.io/sdk/workflow"

// AddTaxWorkflow takes and returns Money, so both values use the custom Payload Converter.
func AddTaxWorkflow(ctx workflow.Context, price Money) (Money, error) {
	price.Cents += price.Cents / 10
	return price, nil
}
