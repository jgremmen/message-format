# Bitmask

/// note
This formatter is **not** included in the `DefaultFormatterService`. You must register it explicitly
by adding a `BitmaskFormatter` instance to your formatter service.
///

The named formatter `bitmask` is selected explicitly by writing `format:bitmask` in the message parameter
configuration. It converts integral numeric values into a `BitSet` and then delegates the actual formatting to
the [BitSet type-based formatter](../typed/bit-set.md). This allows you to use all `BitSet` formatting features
(set-bit mode, binary string mode, list separators) on plain integer values that represent bit flags.

The formatter accepts all Java integral types: `byte`, `short`, `int`, `long` (and their boxed equivalents),
`char`/`Character` and `BigInteger`. Negative values are treated as unsigned bit patterns, so all bits are
preserved as-is going through the conversion. A `BitSet` formatter **must** be registered for the `bitmask`
formatter to work.


## Set-Bit Mode

The most common use of `bitmask` is labeling individual bits. Each number map key represents a bit index, and the
associated message is the label for that bit. The `bitset` configuration key controls the bit ordering: `lsb-set`
(least significant bit first, the default) or `msb-set` (most significant bit first). Only bits that are set in
the value **and** have a matching number map key produce output; all other bits are silently skipped.

```java
// 0x15 = 0b10101 → bits 0, 2, 4 are set
messageSupport
    .message("%{flags,format:bitmask,0:read,2:execute,4:admin}")
    .with("flags", 0x15)
    .format();
// "read, execute, admin"
```

The default ordering is `lsb-set`. To reverse the order, set `bitset` to `msb-set`.

```java
// 0x15 = bits 0, 2, 4 are set
messageSupport
    .message("%{flags,format:bitmask,bitset:'msb-set',0:read,2:execute,4:admin}")
    .with("flags", 0x15)
    .format();
// "admin, execute, read"
```

Bits that are set in the value but do not have a corresponding number map key are skipped. In the following
example, bits 1 and 3 are set but have no label, so none of them appear in the output.

```java
// 0x0E = 0b1110 → bits 1, 2, 3 are set
messageSupport
    .message("%{flags,format:bitmask,0:read,2:write,4:admin}")
    .with("flags", 0x0E)
    .format();
// "write"
```


## List Configuration Keys

Because set-bit mode produces a list of labels, all list configuration keys from the
[BitSet formatter](../typed/bit-set.md) apply. The most commonly used are:

- `list-sep` controls the separator between labels (default: `", "`)
- `list-sep-last` controls the separator before the last label

```java
// 0x8000_0000_0000_00A5 → bits 0, 2, 5, 7, 63 are set
messageSupport
    .message("%{v,format:bitmask,bitset:'lsb-set',list-sep-last:' and ',0:b0,2:b2,5:b5,7:b7,63:b63}")
    .with("v", 0x8000_0000_0000_00A5L)
    .format();
// "b0, b2, b5, b7 and b63"
```


## Binary String Mode

The `bitmask` formatter also supports binary string mode by setting `bitset` to `lsb-bits` or `msb-bits`. In
this mode the entire bit pattern of the numeric value is rendered as a sequence of characters. The `bit0` and
`bit1` configuration keys control the characters used for unset and set bits (default: `0` and `1`).

```java
// 0x95 = 0b10010101 → bits 0, 2, 4, 7 are set
messageSupport
    .message("%{v,format:bitmask,bitset:'msb-bits'}")
    .with("v", (byte)0x95)
    .format();
// "10010101"

messageSupport
    .message("%{v,format:bitmask,bitset:'lsb-bits'}")
    .with("v", (byte)0x95)
    .format();
// "10101001"
```


## Supported Types

All examples above use `int` or `long` values, but the formatter handles all integral types. Negative values are
treated as unsigned bit patterns, preserving all bits.

```java
// byte -1 = 0xFF = all 8 bits set
messageSupport
    .message("%{v,format:bitmask,0:b0,1:b1,2:b2,3:b3,4:b4,5:b5,6:b6,7:b7}")
    .with("v", (byte)-1)
    .format();
// "b0, b1, b2, b3, b4, b5, b6, b7"
```

`BigInteger` values that exceed the range of `long` are fully supported.

```java
// 2^100 + 1 → bits 0 and 100 are set
messageSupport
    .message("%{v,format:bitmask,bitset:'msb-set',0:low,100:high}")
    .with("v", BigInteger.ONE.shiftLeft(100).or(BigInteger.ONE))
    .format();
// "high, low"
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to produce
specific text.

```java
messageSupport
    .message("%{flags,format:bitmask,null:'no flags',0:read,2:write}")
    .with("flags", null)
    .format();
// "no flags"
```
