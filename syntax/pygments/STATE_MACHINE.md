# MessageFormat Lexer State Machine

## State Diagram

```
ROOT
  ├─ %{ ──→ PARAMETER-NAME
  │           ├─ } ──→ [pop to ROOT]
  │           └─ , ──→ PARAMETER-BODY
  │                    ├─ } ──→ [pop to ROOT]
  │                    ├─ format ──→ PARAMETER-FORMAT-COLON
  │                    │              └─ : ──→ PARAMETER-FORMAT-NAME
  │                    │                       └─ nameOrKeyword ──→ [pop to PARAMETER-BODY]
  │                    ├─ ' ──→ SINGLE-QUOTE
  │                    └─ " ──→ DOUBLE-QUOTE
  │
  ├─ %[ ──→ TEMPLATE-NAME
  │           ├─ ] ──→ [pop to ROOT]
  │           └─ , ──→ TEMPLATE-BODY
  │                    ├─ ] ──→ [pop to ROOT]
  │                    ├─ ' ──→ SINGLE-QUOTE
  │                    └─ " ──→ DOUBLE-QUOTE
  │
  ├─ %( ──→ POSTFORMAT-NAME
  │           ├─ ) ──→ [pop to ROOT]
  │           └─ , ──→ POSTFORMAT-MESSAGE
  │                    ├─ ) ──→ [pop to ROOT]
  │                    ├─ , ──→ POSTFORMAT-CONFIG
  │                    │         ├─ ) ──→ [pop to ROOT]
  │                    │         ├─ ' ──→ SINGLE-QUOTE
  │                    │         └─ " ──→ DOUBLE-QUOTE
  │                    ├─ ' ──→ SINGLE-QUOTE
  │                    └─ " ──→ DOUBLE-QUOTE
  │
  ├─ ' ──→ SINGLE-QUOTE
  │         ├─ ' ──→ [pop]
  │         ├─ %{ ──→ PARAMETER-NAME
  │         ├─ %[ ──→ TEMPLATE-NAME
  │         └─ %( ──→ POSTFORMAT-NAME
  │
  └─ " ──→ DOUBLE-QUOTE
            ├─ " ──→ [pop]
            ├─ %{ ──→ PARAMETER-NAME
            ├─ %[ ──→ TEMPLATE-NAME
            └─ %( ──→ POSTFORMAT-NAME
```

## Token Mapping by State

### ROOT State
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `%{` | `Punctuation.Special` | Parameter start (P_START) |
| `%[` | `Punctuation.Special` | Template start (TPL_START) |
| `%(` | `Punctuation.Special` | Post-format start (PF_START) |
| `\\u0000-\\uFFFF` | `String.Escape` | Unicode escape |
| `\\["%'{\\[]` | `String.Escape` | Character escape |
| `[\u0000-\u001f]` | `Whitespace` | Control characters |
| other | `String` | Regular text |

### PARAMETER-NAME State (First Position)
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `}` | `Punctuation.Special` | Parameter end (P_END) |
| `,` | `Punctuation` | Transition to body |
| `true\|false` | `Name.Variable` | Boolean as parameterName |
| `null` | `Name.Variable` | Null keyword as name |
| `empty` | `Name.Variable` | Empty keyword as name |
| `format` | `Name.Variable` | Format keyword as name |
| NAME | `Name.Variable` | Parameter name |

### PARAMETER-BODY State
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `}` | `Punctuation.Special` | Parameter end |
| `,` | `Punctuation` | Separator |
| `:` | `Punctuation` | Key-value separator |
| `format` | `Keyword.Reserved` | FORMAT keyword |
| `<=\|>=\|<>\|<\|>\|=\|!` | `Operator` | Relational/equality operators |
| `true\|false` | `Keyword.Constant` | Boolean in mapKey |
| `null` | `Keyword.Constant` | Null in mapKey |
| `empty` | `Keyword.Constant` | Empty in mapKey |
| `-?[0-9]+` | `Number.Integer` | Number |
| `(\|)` | `Punctuation` | Grouped mapKeys |
| `'\|"` | `String.Single\|Double` | Quote start |
| NAME | `Name.Attribute` | Config key name |

### PARAMETER-FORMAT-COLON State
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `:` | `Punctuation` | COLON in FORMAT sequence |

### PARAMETER-FORMAT-NAME State
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `true\|false` | `Name.Function` | Boolean as format name |
| `null` | `Name.Function` | Null as format name |
| `empty` | `Name.Function` | Empty as format name |
| NAME | `Name.Function` | Format name |

### TEMPLATE-NAME State (First Position)
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `]` | `Punctuation.Special` | Template end (TPL_END) |
| `,` | `Punctuation` | Transition to body |
| `true\|false` | `Name.Label` | Boolean as templateName |
| `null` | `Name.Label` | Null as templateName |
| `empty` | `Name.Label` | Empty as templateName |
| NAME | `Name.Label` | Template name |

### TEMPLATE-BODY State
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `]` | `Punctuation.Special` | Template end |
| `,` | `Punctuation` | Separator |
| `:` | `Punctuation` | Key-value separator |
| `=` | `Operator` | Delegation operator |
| `true\|false` | `Keyword.Constant` | Boolean value |
| `null` | `Keyword.Constant` | Null value |
| `empty` | `Keyword.Constant` | Empty value |
| `-?[0-9]+` | `Number.Integer` | Number |
| `'\|"` | `String.Single\|Double` | Quote start |
| NAME | `Name.Attribute` | Config/delegate name |

### POSTFORMAT-NAME State (First Position)
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `)` | `Punctuation.Special` | Post-format end (PF_END) |
| `,` | `Punctuation` | Transition to message |
| `true\|false` | `Name.Function` | Boolean as postFormatName |
| `null` | `Name.Function` | Null as name |
| `empty` | `Name.Function` | Empty as name |
| `format` | `Name.Function` | Format as name |
| NAME | `Name.Function` | Post-format name |

### POSTFORMAT-MESSAGE State
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `)` | `Punctuation.Special` | Post-format end |
| `,` | `Punctuation` | Transition to config |
| `'\|"` | `String.Single\|Double` | quotedMessage start |

### POSTFORMAT-CONFIG State
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `)` | `Punctuation.Special` | Post-format end |
| `,` | `Punctuation` | Separator |
| `:` | `Punctuation` | Key-value separator |
| `true\|false` | `Keyword.Constant` | Boolean value |
| `null` | `Keyword.Constant` | Null value |
| `empty` | `Keyword.Constant` | Empty value |
| `-?[0-9]+` | `Number.Integer` | Number |
| `'\|"` | `String.Single\|Double` | Quote start |
| NAME | `Name.Attribute` | Config key name |

### SINGLE-QUOTE / DOUBLE-QUOTE States
| Pattern | Token Type | Description |
|---------|-----------|-------------|
| `'\|"` | `String.Single\|Double` | Quote end |
| `%{` | `Punctuation.Special` | Nested parameter |
| `%[` | `Punctuation.Special` | Nested template |
| `%(` | `Punctuation.Special` | Nested post-format |
| `\\u0000-\\uFFFF` | `String.Escape` | Unicode escape |
| `\\["%'{\\[]` | `String.Escape` | Character escape |
| `[\u0000-\u001f]` | `Whitespace` | Control characters |
| other | `String` | String content |

## Examples

### Example 1: Post-Format with Nested Parameter
```
%(clip,'%{s,empty:x}',clip:34)
```

**Token sequence:**
1. `%(` → `Punctuation.Special` [push POSTFORMAT-NAME]
2. `clip` → `Name.Function` [postFormatName]
3. `,` → `Punctuation` [pop to POSTFORMAT-MESSAGE]
4. `'` → `String.Single` [push SINGLE-QUOTE]
5. `%{` → `Punctuation.Special` [push PARAMETER-NAME]
6. `s` → `Name.Variable` [parameterName]
7. `,` → `Punctuation` [pop to PARAMETER-BODY]
8. `empty` → `Keyword.Constant` [mapKey]
9. `:` → `Punctuation`
10. `x` → `Name.Attribute` [simpleString in mapEntry]
11. `}` → `Punctuation.Special` [pop to SINGLE-QUOTE]
12. `'` → `String.Single` [pop to POSTFORMAT-MESSAGE]
13. `,` → `Punctuation` [pop to POSTFORMAT-CONFIG]
14. `clip` → `Name.Attribute` [configDefinition NAME]
15. `:` → `Punctuation`
16. `34` → `Number.Integer`
17. `)` → `Punctuation.Special` [pop to ROOT]

### Example 2: Parameter with FORMAT Keyword
```
%{p,format:mychar}
```

**Token sequence:**
1. `%{` → `Punctuation.Special` [push PARAMETER-NAME]
2. `p` → `Name.Variable` [parameterName]
3. `,` → `Punctuation` [pop to PARAMETER-BODY]
4. `format` → `Keyword.Reserved` [FORMAT] [push PARAMETER-FORMAT-COLON]
5. `:` → `Punctuation` [pop to PARAMETER-FORMAT-NAME]
6. `mychar` → `Name.Function` [format name] [pop to PARAMETER-BODY]
7. `}` → `Punctuation.Special` [pop to ROOT]

### Example 3: Template with Parameter Delegation
```
%[tpl,param=delegated]
```

**Token sequence:**
1. `%[` → `Punctuation.Special` [push TEMPLATE-NAME]
2. `tpl` → `Name.Label` [templateName]
3. `,` → `Punctuation` [pop to TEMPLATE-BODY]
4. `param` → `Name.Attribute` [simpleString]
5. `=` → `Operator`
6. `delegated` → `Name.Attribute` [simpleString]
7. `]` → `Punctuation.Special` [pop to ROOT]

