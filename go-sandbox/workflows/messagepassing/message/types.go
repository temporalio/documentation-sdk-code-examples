package message

import (
	"context"
	"fmt"
)

const TaskQueue = "message-passing"

type YourWorkflowParam struct {
	Name string
}

type MySignal struct {
	Message string
}

type UpdateInput struct {
	Value string
}

type Data struct {
	X int
	Y int
}

// CallGreetingService stands in for a call to a translation service.
func CallGreetingService(ctx context.Context, language Language) (string, error) {
	return fmt.Sprintf("Hello in %s", language), nil
}

// FetchData stands in for a call that returns data for the handler to store.
func FetchData(ctx context.Context, name string) (Data, error) {
	return Data{X: len(name), Y: len(name) * 2}, nil
}
