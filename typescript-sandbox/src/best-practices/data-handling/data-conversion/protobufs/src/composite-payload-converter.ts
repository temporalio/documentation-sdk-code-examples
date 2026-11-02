// @@@SNIPSTART typescript-data-conversion-protobuf-composite-converter
import {
  BinaryPayloadConverter,
  CompositePayloadConverter,
  JsonPayloadConverter,
  UndefinedPayloadConverter,
} from '@temporalio/common';
import { ProtobufBinaryPayloadConverter } from '@temporalio/common/lib/protobufs';
import root from '../protos/root';

export const payloadConverter = new CompositePayloadConverter(
  new UndefinedPayloadConverter(),
  new BinaryPayloadConverter(),
  new ProtobufBinaryPayloadConverter(root),
  new JsonPayloadConverter(),
);
// @@@SNIPEND
