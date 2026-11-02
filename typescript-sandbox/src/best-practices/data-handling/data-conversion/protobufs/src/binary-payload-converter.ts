// @@@SNIPSTART typescript-data-conversion-protobuf-binary-converter
import { ProtobufBinaryPayloadConverter } from '@temporalio/common/lib/protobufs';
import root from '../protos/root';

export const payloadConverter = new ProtobufBinaryPayloadConverter(root);
// @@@SNIPEND
