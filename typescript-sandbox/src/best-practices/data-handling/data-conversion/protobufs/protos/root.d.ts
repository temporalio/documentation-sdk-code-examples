import * as $protobuf from "protobufjs";
import Long = require("long");

/**
 * Properties of a ProtoResult.
 * @deprecated Use ProtoResult.$Properties instead.
 */
export interface IProtoResult extends ProtoResult.$Properties {
}

/** Represents a ProtoResult. */
export class ProtoResult {

    /**
     * Constructs a new ProtoResult.
     * @param [properties] Properties to set
     */
    constructor(properties?: ProtoResult.$Properties);

    /** Unknown fields preserved while decoding when enabled */
    $unknowns?: Uint8Array[];

    /** ProtoResult sentence. */
    sentence: string;

    /**
     * Creates a new ProtoResult instance using the specified properties.
     * @param [properties] Properties to set
     * @returns ProtoResult instance
     */
    static create(properties: ProtoResult.$Shape): ProtoResult & ProtoResult.$Shape;
    static create(properties?: ProtoResult.$Properties): ProtoResult;

    /**
     * Encodes the specified ProtoResult message. Does not implicitly {@link ProtoResult.verify|verify} messages.
     * @param message ProtoResult message or plain object to encode
     * @param [writer] Writer to encode to
     * @returns Writer
     */
    static encode(message: ProtoResult.$Properties, writer?: $protobuf.Writer): $protobuf.Writer;

    /**
     * Encodes the specified ProtoResult message, length delimited. Does not implicitly {@link ProtoResult.verify|verify} messages.
     * @param message ProtoResult message or plain object to encode
     * @param [writer] Writer to encode to
     * @returns Writer
     */
    static encodeDelimited(message: ProtoResult.$Properties, writer?: $protobuf.Writer): $protobuf.Writer;

    /**
     * Decodes a ProtoResult message from the specified reader or buffer.
     * @param reader Reader or buffer to decode from
     * @param [length] Message length if known beforehand
     * @returns {ProtoResult & ProtoResult.$Shape} ProtoResult
     * @throws {Error} If the payload is not a reader or valid buffer
     * @throws {$protobuf.util.ProtocolError} If required fields are missing
     */
    static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): ProtoResult & ProtoResult.$Shape;

    /**
     * Decodes a ProtoResult message from the specified reader or buffer, length delimited.
     * @param reader Reader or buffer to decode from
     * @returns {ProtoResult & ProtoResult.$Shape} ProtoResult
     * @throws {Error} If the payload is not a reader or valid buffer
     * @throws {$protobuf.util.ProtocolError} If required fields are missing
     */
    static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): ProtoResult & ProtoResult.$Shape;

    /**
     * Verifies a ProtoResult message.
     * @param message Plain object to verify
     * @returns `null` if valid, otherwise the reason why it is not
     */
    static verify(message: { [k: string]: any }): (string|null);

    /**
     * Creates a ProtoResult message from a plain object. Also converts values to their respective internal types.
     * @param object Plain object
     * @returns ProtoResult
     */
    static fromObject(object: { [k: string]: any }): ProtoResult;

    /**
     * Creates a plain object from a ProtoResult message. Also converts values to other types if specified.
     * @param message ProtoResult
     * @param [options] Conversion options
     * @returns Plain object
     */
    static toObject(message: ProtoResult, options?: $protobuf.IConversionOptions): { [k: string]: any };

    /**
     * Converts this ProtoResult to JSON.
     * @returns JSON object
     */
    toJSON(): { [k: string]: any };

    /**
     * Gets the type url for ProtoResult
     * @param [prefix] Custom type url prefix, defaults to `"type.googleapis.com"`
     * @returns The type url
     */
    static getTypeUrl(prefix?: string): string;
}

export namespace ProtoResult {

    /** Properties of a ProtoResult. */
    interface $Properties {

        /** ProtoResult sentence */
        sentence?: (string|null);

        /** Unknown fields preserved while decoding when enabled */
        $unknowns?: Uint8Array[];
    }

    /** Shape of a ProtoResult. */
    type $Shape = ProtoResult.$Properties;
}

/**
 * Properties of a BinaryMessage.
 * @deprecated Use BinaryMessage.$Properties instead.
 */
export interface IBinaryMessage extends BinaryMessage.$Properties {
}

/** Represents a BinaryMessage. */
export class BinaryMessage {

    /**
     * Constructs a new BinaryMessage.
     * @param [properties] Properties to set
     */
    constructor(properties?: BinaryMessage.$Properties);

    /** Unknown fields preserved while decoding when enabled */
    $unknowns?: Uint8Array[];

    /** BinaryMessage data. */
    data: Uint8Array;

    /**
     * Creates a new BinaryMessage instance using the specified properties.
     * @param [properties] Properties to set
     * @returns BinaryMessage instance
     */
    static create(properties: BinaryMessage.$Shape): BinaryMessage & BinaryMessage.$Shape;
    static create(properties?: BinaryMessage.$Properties): BinaryMessage;

    /**
     * Encodes the specified BinaryMessage message. Does not implicitly {@link BinaryMessage.verify|verify} messages.
     * @param message BinaryMessage message or plain object to encode
     * @param [writer] Writer to encode to
     * @returns Writer
     */
    static encode(message: BinaryMessage.$Properties, writer?: $protobuf.Writer): $protobuf.Writer;

    /**
     * Encodes the specified BinaryMessage message, length delimited. Does not implicitly {@link BinaryMessage.verify|verify} messages.
     * @param message BinaryMessage message or plain object to encode
     * @param [writer] Writer to encode to
     * @returns Writer
     */
    static encodeDelimited(message: BinaryMessage.$Properties, writer?: $protobuf.Writer): $protobuf.Writer;

    /**
     * Decodes a BinaryMessage message from the specified reader or buffer.
     * @param reader Reader or buffer to decode from
     * @param [length] Message length if known beforehand
     * @returns {BinaryMessage & BinaryMessage.$Shape} BinaryMessage
     * @throws {Error} If the payload is not a reader or valid buffer
     * @throws {$protobuf.util.ProtocolError} If required fields are missing
     */
    static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): BinaryMessage & BinaryMessage.$Shape;

    /**
     * Decodes a BinaryMessage message from the specified reader or buffer, length delimited.
     * @param reader Reader or buffer to decode from
     * @returns {BinaryMessage & BinaryMessage.$Shape} BinaryMessage
     * @throws {Error} If the payload is not a reader or valid buffer
     * @throws {$protobuf.util.ProtocolError} If required fields are missing
     */
    static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): BinaryMessage & BinaryMessage.$Shape;

    /**
     * Verifies a BinaryMessage message.
     * @param message Plain object to verify
     * @returns `null` if valid, otherwise the reason why it is not
     */
    static verify(message: { [k: string]: any }): (string|null);

    /**
     * Creates a BinaryMessage message from a plain object. Also converts values to their respective internal types.
     * @param object Plain object
     * @returns BinaryMessage
     */
    static fromObject(object: { [k: string]: any }): BinaryMessage;

    /**
     * Creates a plain object from a BinaryMessage message. Also converts values to other types if specified.
     * @param message BinaryMessage
     * @param [options] Conversion options
     * @returns Plain object
     */
    static toObject(message: BinaryMessage, options?: $protobuf.IConversionOptions): { [k: string]: any };

    /**
     * Converts this BinaryMessage to JSON.
     * @returns JSON object
     */
    toJSON(): { [k: string]: any };

    /**
     * Gets the type url for BinaryMessage
     * @param [prefix] Custom type url prefix, defaults to `"type.googleapis.com"`
     * @returns The type url
     */
    static getTypeUrl(prefix?: string): string;
}

export namespace BinaryMessage {

    /** Properties of a BinaryMessage. */
    interface $Properties {

        /** BinaryMessage data */
        data?: (Uint8Array|null);

        /** Unknown fields preserved while decoding when enabled */
        $unknowns?: Uint8Array[];
    }

    /** Shape of a BinaryMessage. */
    type $Shape = BinaryMessage.$Properties;
}

/** Namespace foo. */
export namespace foo {

    /** Namespace bar. */
    namespace bar {

        /**
         * Properties of a ProtoInput.
         * @deprecated Use foo.bar.ProtoInput.$Properties instead.
         */
        interface IProtoInput extends foo.bar.ProtoInput.$Properties {
        }

        /** Represents a ProtoInput. */
        class ProtoInput {

            /**
             * Constructs a new ProtoInput.
             * @param [properties] Properties to set
             */
            constructor(properties?: foo.bar.ProtoInput.$Properties);

            /** Unknown fields preserved while decoding when enabled */
            $unknowns?: Uint8Array[];

            /** ProtoInput name. */
            name: string;

            /** ProtoInput age. */
            age: number;

            /**
             * Creates a new ProtoInput instance using the specified properties.
             * @param [properties] Properties to set
             * @returns ProtoInput instance
             */
            static create(properties: foo.bar.ProtoInput.$Shape): foo.bar.ProtoInput & foo.bar.ProtoInput.$Shape;
            static create(properties?: foo.bar.ProtoInput.$Properties): foo.bar.ProtoInput;

            /**
             * Encodes the specified ProtoInput message. Does not implicitly {@link foo.bar.ProtoInput.verify|verify} messages.
             * @param message ProtoInput message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            static encode(message: foo.bar.ProtoInput.$Properties, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Encodes the specified ProtoInput message, length delimited. Does not implicitly {@link foo.bar.ProtoInput.verify|verify} messages.
             * @param message ProtoInput message or plain object to encode
             * @param [writer] Writer to encode to
             * @returns Writer
             */
            static encodeDelimited(message: foo.bar.ProtoInput.$Properties, writer?: $protobuf.Writer): $protobuf.Writer;

            /**
             * Decodes a ProtoInput message from the specified reader or buffer.
             * @param reader Reader or buffer to decode from
             * @param [length] Message length if known beforehand
             * @returns {foo.bar.ProtoInput & foo.bar.ProtoInput.$Shape} ProtoInput
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): foo.bar.ProtoInput & foo.bar.ProtoInput.$Shape;

            /**
             * Decodes a ProtoInput message from the specified reader or buffer, length delimited.
             * @param reader Reader or buffer to decode from
             * @returns {foo.bar.ProtoInput & foo.bar.ProtoInput.$Shape} ProtoInput
             * @throws {Error} If the payload is not a reader or valid buffer
             * @throws {$protobuf.util.ProtocolError} If required fields are missing
             */
            static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): foo.bar.ProtoInput & foo.bar.ProtoInput.$Shape;

            /**
             * Verifies a ProtoInput message.
             * @param message Plain object to verify
             * @returns `null` if valid, otherwise the reason why it is not
             */
            static verify(message: { [k: string]: any }): (string|null);

            /**
             * Creates a ProtoInput message from a plain object. Also converts values to their respective internal types.
             * @param object Plain object
             * @returns ProtoInput
             */
            static fromObject(object: { [k: string]: any }): foo.bar.ProtoInput;

            /**
             * Creates a plain object from a ProtoInput message. Also converts values to other types if specified.
             * @param message ProtoInput
             * @param [options] Conversion options
             * @returns Plain object
             */
            static toObject(message: foo.bar.ProtoInput, options?: $protobuf.IConversionOptions): { [k: string]: any };

            /**
             * Converts this ProtoInput to JSON.
             * @returns JSON object
             */
            toJSON(): { [k: string]: any };

            /**
             * Gets the type url for ProtoInput
             * @param [prefix] Custom type url prefix, defaults to `"type.googleapis.com"`
             * @returns The type url
             */
            static getTypeUrl(prefix?: string): string;
        }

        namespace ProtoInput {

            /** Properties of a ProtoInput. */
            interface $Properties {

                /** ProtoInput name */
                name?: (string|null);

                /** ProtoInput age */
                age?: (number|null);

                /** Unknown fields preserved while decoding when enabled */
                $unknowns?: Uint8Array[];
            }

            /** Shape of a ProtoInput. */
            type $Shape = foo.bar.ProtoInput.$Properties;
        }
    }
}
