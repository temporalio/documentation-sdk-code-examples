package sideeffects

import (
	"fmt"
	"math/rand"

	"go.temporal.io/sdk/workflow"
)

// SideEffectWorkflow records a random number in the Event History so replays get the same value.
func SideEffectWorkflow(ctx workflow.Context) (int, error) {
	// @@@SNIPSTART go-side-effect-correct
	encodedRandom := workflow.SideEffect(ctx, func(ctx workflow.Context) interface{} {
		return rand.Intn(100)
	})

	var random int
	if err := encodedRandom.Get(&random); err != nil {
		return 0, err
	}
	// ...
	// @@@SNIPEND
	return random, nil
}

// IncorrectSideEffectWorkflow shows what not to do. Don't register it on a Worker.
func IncorrectSideEffectWorkflow(ctx workflow.Context) (int, error) {
	// @@@SNIPSTART go-side-effect-incorrect
	// Warning: This is an incorrect example.
	// This code is non-deterministic.
	var random int
	workflow.SideEffect(ctx, func(ctx workflow.Context) interface{} {
		random = rand.Intn(100)
		return nil
	})
	// random will always be 0 in replay, so this code is non-deterministic.
	// @@@SNIPEND
	return random, nil
}

// configuredNumber stands in for reading a value that can change between Workflow Tasks,
// such as a configuration setting.
var configuredNumber = func() int { return 42 }

// MutableSideEffectWorkflow records the configured number, and records it again only when it changes.
func MutableSideEffectWorkflow(ctx workflow.Context) (int, error) {
	// @@@SNIPSTART go-mutable-side-effect
	get := func(ctx workflow.Context) interface{} {
		return configuredNumber()
	}
	eq := func(a, b interface{}) bool {
		return a.(int) == b.(int)
	}

	var number int
	if err := workflow.MutableSideEffect(ctx, "configureNumber", get, eq).Get(&number); err != nil {
		return 0, fmt.Errorf("can't decode number: %w", err)
	}
	// @@@SNIPEND
	return number, nil
}
