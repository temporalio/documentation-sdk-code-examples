// @@@SNIPSTART go-message-greeting-workflow
package message

import (
	"fmt"
	"maps"
	"slices"

	"go.temporal.io/sdk/workflow"
)

type Language string

const Chinese Language = "chinese"
const English Language = "english"
const French Language = "french"
const Spanish Language = "spanish"
const Portuguese Language = "portuguese"

const GetLanguagesQuery = "GetLanguages"
const ApproveSignal = "approve"
const SetLanguageUpdate = "set-language"

type GetLanguagesInput struct {
	IncludeUnsupported bool
}

type ApproveInput struct {
	Name string
}

func GreetingWorkflow(ctx workflow.Context) (string, error) {
	logger := workflow.GetLogger(ctx)
	approverName := ""
	language := English
	greeting := map[Language]string{English: "Hello", Chinese: "你好，世界"}

	err := workflow.SetQueryHandler(ctx, GetLanguagesQuery, func(input GetLanguagesInput) ([]Language, error) {
		// 👉 A Query handler returns a value: it can inspect but must not mutate the Workflow state.
		if input.IncludeUnsupported {
			return []Language{Chinese, English, French, Spanish, Portuguese}, nil
		}
		// Sort the map keys so the result is the same every time.
		return slices.Sorted(maps.Keys(greeting)), nil
	})
	if err != nil {
		return "", err
	}

	err = workflow.SetUpdateHandlerWithOptions(ctx, SetLanguageUpdate, func(ctx workflow.Context, newLanguage Language) (Language, error) {
		// 👉 An Update handler can mutate the Workflow state and return a value.
		var previousLanguage Language
		previousLanguage, language = language, newLanguage
		return previousLanguage, nil
	}, workflow.UpdateHandlerOptions{
		Validator: func(ctx workflow.Context, newLanguage Language) error {
			if _, ok := greeting[newLanguage]; !ok {
				// 👉 In an Update validator you return any error to reject the Update.
				return fmt.Errorf("%s unsupported language", newLanguage)
			}
			return nil
		},
	})
	if err != nil {
		return "", err
	}

	// Block until the language is approved
	var approveInput ApproveInput
	workflow.GetSignalChannel(ctx, ApproveSignal).Receive(ctx, &approveInput)
	approverName = approveInput.Name
	logger.Info("Received approval", "Approver", approverName)

	return greeting[language], nil
}
// @@@SNIPEND
