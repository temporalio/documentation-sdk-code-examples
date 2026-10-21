// Package mycodecpackage wraps the default Data Converter with a custom Payload Codec.
package mycodecpackage

import "go.temporal.io/sdk/converter"

// @@@SNIPSTART go-codec-data-converter
// Create an instance of Data Converter with your codec.
var DataConverter = converter.NewCodecDataConverter(
	converter.GetDefaultDataConverter(),
	NewPayloadCodec(),
)

// Create an instance of PayloadCodec.
func NewPayloadCodec() converter.PayloadCodec {
	return &Codec{}
}
// @@@SNIPEND
