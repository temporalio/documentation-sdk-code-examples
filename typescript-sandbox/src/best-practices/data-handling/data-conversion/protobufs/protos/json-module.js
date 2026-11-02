/*eslint-disable block-scoped-var, id-length, no-control-regex, no-magic-numbers, no-mixed-operators, no-prototype-builtins, no-redeclare, no-shadow, no-var, sort-vars, default-case, jsdoc/require-param*/
"use strict";

var $protobuf = require("protobufjs/light");

var $root = ($protobuf.roots["protobuf-sample"] || ($protobuf.roots["protobuf-sample"] = new $protobuf.Root()))
.addJSON({
  "ProtoResult": {
    "fields": {
      "sentence": {
        "type": "string",
        "id": 1
      }
    }
  },
  "BinaryMessage": {
    "fields": {
      "data": {
        "type": "bytes",
        "id": 1
      }
    }
  },
  "foo": {
    "nested": {
      "bar": {
        "nested": {
          "ProtoInput": {
            "fields": {
              "name": {
                "type": "string",
                "id": 1
              },
              "age": {
                "type": "int32",
                "id": 2
              }
            }
          }
        }
      }
    }
  }
});

/**
 * Reflected root namespace.
 * @type {$protobuf.Root}
 */
module.exports = $root;
