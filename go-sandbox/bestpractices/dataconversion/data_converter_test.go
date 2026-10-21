package dataconversion

import (
	"testing"

	"github.com/stretchr/testify/require"
	"go.temporal.io/sdk/converter"
	"go.temporal.io/sdk/testsuite"
)

func TestExtendedConverterUsesCustomEncodingForMoney(t *testing.T) {
	dc := ExtendDefaultDataConverter()

	payload, err := dc.ToPayload(Money{Cents: 1234, Currency: "USD"})
	require.NoError(t, err)
	require.Equal(t, moneyEncoding, string(payload.Metadata[converter.MetadataEncoding]))
	require.Equal(t, "12.34 USD", string(payload.Data))

	var decoded Money
	require.NoError(t, dc.FromPayload(payload, &decoded))
	require.Equal(t, Money{Cents: 1234, Currency: "USD"}, decoded)
}

func TestExtendedConverterKeepsDefaultsForOtherTypes(t *testing.T) {
	dc := ExtendDefaultDataConverter()
	payload, err := dc.ToPayload(map[string]int{"items": 3})
	require.NoError(t, err)
	require.Equal(t, "json/plain", string(payload.Metadata[converter.MetadataEncoding]))
}

func TestReplacedConverterHandlesOnlyMoney(t *testing.T) {
	dc := ReplaceDefaultDataConverter()
	_, err := dc.ToPayload(Money{Cents: 500, Currency: "EUR"})
	require.NoError(t, err)
	// With the defaults replaced, no Payload Converter can encode other types.
	_, err = dc.ToPayload("a string")
	require.Error(t, err)
}

func TestWorkflowWithCustomDataConverter(t *testing.T) {
	var suite testsuite.WorkflowTestSuite
	env := suite.NewTestWorkflowEnvironment()
	env.SetDataConverter(ExtendDefaultDataConverter())
	env.ExecuteWorkflow(AddTaxWorkflow, Money{Cents: 1000, Currency: "USD"})
	require.NoError(t, env.GetWorkflowError())
	var result Money
	require.NoError(t, env.GetWorkflowResult(&result))
	require.Equal(t, Money{Cents: 1100, Currency: "USD"}, result)
}
