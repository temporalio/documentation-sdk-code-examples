// Package dataconversion shows how to add a custom Payload Converter to a Data Converter.
package dataconversion

import (
	"fmt"

	commonpb "go.temporal.io/api/common/v1"
	"go.temporal.io/sdk/converter"
)

// Money is an application type that the custom Payload Converter serializes.
type Money struct {
	Cents    int64
	Currency string
}

const moneyEncoding = "text/x-money"

// moneyPayloadConverter stores Money as a "12.34 USD" string, and leaves every other type
// for the next Payload Converter in the Data Converter.
type moneyPayloadConverter struct{}

// YourCustomPayloadConverter returns the custom Payload Converter for Money values.
func YourCustomPayloadConverter() converter.PayloadConverter {
	return moneyPayloadConverter{}
}

func (moneyPayloadConverter) ToPayload(value interface{}) (*commonpb.Payload, error) {
	m, ok := value.(Money)
	if !ok {
		if p, isPtr := value.(*Money); isPtr && p != nil {
			m, ok = *p, true
		}
	}
	if !ok {
		// Returning nil lets the Data Converter try the next Payload Converter.
		return nil, nil
	}
	data := fmt.Sprintf("%d.%02d %s", m.Cents/100, m.Cents%100, m.Currency)
	return &commonpb.Payload{
		Metadata: map[string][]byte{converter.MetadataEncoding: []byte(moneyEncoding)},
		Data:     []byte(data),
	}, nil
}

func (moneyPayloadConverter) FromPayload(payload *commonpb.Payload, valuePtr interface{}) error {
	m, ok := valuePtr.(*Money)
	if !ok {
		return fmt.Errorf("%s payload can only decode into *Money, not %T", moneyEncoding, valuePtr)
	}
	var whole, cents int64
	if _, err := fmt.Sscanf(string(payload.GetData()), "%d.%02d %s", &whole, &cents, &m.Currency); err != nil {
		return fmt.Errorf("unable to decode money payload: %w", err)
	}
	m.Cents = whole*100 + cents
	return nil
}

func (moneyPayloadConverter) ToString(payload *commonpb.Payload) string {
	return string(payload.GetData())
}

func (moneyPayloadConverter) Encoding() string {
	return moneyEncoding
}
