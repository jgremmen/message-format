# BitSet

This formatter is included in the `DefaultFormatterService`.

The `BitSetFormatter` handles `java.util.BitSet` values. It is automatically selected whenever a parameter value
is a `BitSet`. The formatter supports two fundamentally different output modes: a set-bit mode that renders named
labels for each set bit, and a binary string mode that renders the entire bit set as a sequence of characters.

The `bitset` configuration key controls which mode is used.


## Set-Bit Mode (`lsb-set` / `msb-set`)

In set-bit mode, the formatter iterates over the set bits in the `BitSet` and looks up each bit's index among the
number map keys defined in the parameter configuration. Only bits that have a matching number map key produce
output; unmatched bits are silently skipped. The matched messages are formatted individually and joined into a
single text string using the standard list configuration keys.

The default mode is `lsb-set` (least significant bit first). You can also use `msb-set` to output set bits from
highest to lowest.

```java
BitSet permissions = new BitSet();
permissions.set(0);
permissions.set(2);
permissions.set(4);

messageSupport
    .message("%{perms,0:read,2:execute,4:admin}")
    .with("perms", permissions)
    .format();
// "read, execute, admin"
```

The second example demonstrates how `list-sep-last` works together with set-bit mode to produce a natural-language
list:

```java
BitSet toppings = new BitSet();
toppings.set(0);
toppings.set(2);
toppings.set(3);

messageSupport
    .message("Pizza with %{t,list-sep-last:' and ',0:cheese,1:ham,2:mushrooms,3:olives}")
    .with("t", toppings)
    .format();
// "Pizza with cheese, mushrooms and olives"
```

### Bit Ordering

With `lsb-set` (the default), bits are output from lowest index to highest. With `msb-set`, the order is
reversed.

```java
BitSet bits = new BitSet();
bits.set(0);
bits.set(2);
bits.set(4);
bits.set(6);
bits.set(8);

messageSupport
    .message("%{bs,bitset:'lsb-set',0:zero,2:two,4:four,6:six,8:eight}")
    .with("bs", bits)
    .format();
// "zero, two, four, six, eight"

messageSupport
    .message("%{bs,bitset:'msb-set',0:zero,2:two,4:four,6:six,8:eight}")
    .with("bs", bits)
    .format();
// "eight, six, four, two, zero"
```

### Unmatched Bits

Set bits that do not have a corresponding number map key are skipped entirely. This means the output only
includes bits you explicitly define labels for.

```java
BitSet bits = new BitSet();
bits.set(1);
bits.set(5);
bits.set(10);
bits.set(15);

messageSupport
    .message("%{bs,0:zero,1:one,2:two}")
    .with("bs", bits)
    .format();
// "one"
```

### List Configuration Keys

Because set-bit mode produces a list of labels, all list configuration keys apply:

- `list-sep` is the separator between labels (default: `", "`)
- `list-sep-last` is the separator before the last label
- `list-max-size` is the maximum number of labels to include
- `list-value-more` is the overflow text appended when truncated
- `list-unique` suppresses duplicate label texts

```java
BitSet bits = new BitSet();
bits.set(1);
bits.set(2);
bits.set(3);
bits.set(5);
bits.set(8);

messageSupport
    .message("%{bs,list-unique:true,(1,3,5):same,2:two,8:eight}")
    .with("bs", bits)
    .format();
// "same, two, eight"
```


## Binary String Mode (`lsb-bits` / `msb-bits`)

In binary string mode, the formatter renders the entire bit set as a sequence of characters, one per bit position
from index 0 up to the highest set bit. Each position outputs either the `bit0` character (for unset bits) or the
`bit1` character (for set bits).

Use `lsb-bits` for least-significant-bit-first order (index 0 on the left) or `msb-bits` for
most-significant-bit-first order (highest bit on the left).

```java
BitSet bits = new BitSet();
bits.set(0);
bits.set(3);
bits.set(5);

messageSupport
    .message("%{bs,bitset:'lsb-bits'}")
    .with("bs", bits)
    .format();
// "100101"

messageSupport
    .message("%{bs,bitset:'msb-bits'}")
    .with("bs", bits)
    .format();
// "101001"
```

### Custom Bit Characters

The `bit0` and `bit1` configuration keys control the text used for unset and set bits respectively. They default
to `0` and `1`.

```java
BitSet bits = new BitSet();
bits.set(1);
bits.set(2);
bits.set(4);

messageSupport
    .message("%{bs,bitset:'lsb-bits',bit0:X,bit1:O}")
    .with("bs", bits)
    .format();
// "XOOXO"

messageSupport
    .message("%{bs,bitset:'msb-bits',bit0:X,bit1:O}")
    .with("bs", bits)
    .format();
// "OXOOX"
```

### Empty BitSet

When the bit set has no bits set, the binary string mode produces an empty string (since the logical length is
zero).

```java
messageSupport
    .message("%{bs,bitset:'lsb-bits'}")
    .with("bs", new BitSet())
    .format();
// ""
```


## Null Handling

A `null` parameter value produces an empty string by default. You can provide a `null` map key to handle this
case explicitly.

```java
messageSupport
    .message("%{bs,null:'no flags'}")
    .with("bs", (BitSet) null)
    .format();
// "no flags"
```
