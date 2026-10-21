package mycodecpackage

import (
	"testing"

	"github.com/stretchr/testify/require"
	commonpb "go.temporal.io/api/common/v1"
	"go.temporal.io/sdk/converter"
)

func TestCodecRoundTrip(t *testing.T) {
	payload, err := converter.GetDefaultDataConverter().ToPayload("Hello Temporal")
	require.NoError(t, err)

	encoded, err := Codec{}.Encode([]*commonpb.Payload{payload})
	require.NoError(t, err)
	require.Equal(t, "binary/snappy", string(encoded[0].Metadata[converter.MetadataEncoding]))

	decoded, err := Codec{}.Decode(encoded)
	require.NoError(t, err)
	require.Equal(t, payload.Data, decoded[0].Data)
}

func TestDecodeLeavesOtherEncodings(t *testing.T) {
	payload, err := converter.GetDefaultDataConverter().ToPayload(42)
	require.NoError(t, err)
	decoded, err := Codec{}.Decode([]*commonpb.Payload{payload})
	require.NoError(t, err)
	require.Same(t, payload, decoded[0])
}

func TestDataConverterRoundTrip(t *testing.T) {
	payload, err := DataConverter.ToPayload(map[string]string{"name": "Temporal"})
	require.NoError(t, err)
	require.Equal(t, "binary/snappy", string(payload.Metadata[converter.MetadataEncoding]))

	var value map[string]string
	require.NoError(t, DataConverter.FromPayload(payload, &value))
	require.Equal(t, "Temporal", value["name"])
}
