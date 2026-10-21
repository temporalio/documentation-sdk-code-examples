package dataconversion

import "go.temporal.io/sdk/converter"

// ReplaceDefaultDataConverter returns a Data Converter that uses only the custom Payload Converter.
func ReplaceDefaultDataConverter() converter.DataConverter {
	// @@@SNIPSTART go-data-conversion-replace-default
	dataConverter := converter.NewCompositeDataConverter(YourCustomPayloadConverter())
	// @@@SNIPEND
	return dataConverter
}

// ExtendDefaultDataConverter returns the default Data Converter with the custom Payload Converter
// added just before the JSON fallback.
func ExtendDefaultDataConverter() converter.DataConverter {
	// @@@SNIPSTART go-data-conversion-extend-default
	dataConverter := converter.NewCompositeDataConverter(
		converter.NewNilPayloadConverter(),
		converter.NewByteSlicePayloadConverter(),
		converter.NewProtoJSONPayloadConverter(),
		converter.NewProtoPayloadConverter(),
		YourCustomPayloadConverter(),
		converter.NewJSONPayloadConverter(),
	)
	// @@@SNIPEND
	return dataConverter
}
