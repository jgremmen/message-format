## Antlr Message lexer/parser rule -> Pygments token

| Antlr Rule/Token/Fragment   | Pygments Token                                     |
|-----------------------------|----------------------------------------------------|
| fragment `EscapeSequence`   | `String.Escape`                                    |
| `text`                      | `String`                                           |
| `SQ_STRT message0 SQ_END`   | `String.Single`                                    |
| `DQ_STRT message0 DQ_END`   | `String.Double`                                    |
| `SQ_STRT text? SQ_END`      | `String.Single`                                    |
| `DQ_STRT text? DQ_END`      | `String.Double`                                    |
| `simpleString`              | `String`                                           |
| `P_START`                   | `Punctuation.Special`                              |
| `P_END`                     | `Punctuation.Special`                              |
| `TPL_START`                 | `Punctuation.Special`                              |
| `TPL_END`                   | `Punctuation.Special`                              |
| `PF_START`                  | `Punctuation.Special`                              |
| `PF_END`                    | `Punctuation.Special`                              |
| `COMMA`                     | `Punctuation`                                      |
| `COLON`                     | `Punctuation`                                      |
| `parameterName`             | `Name.Variable`                                    |
| `FORMAT`                    | `Keyword.Reserved`                                 |
| `parameterFormat`           | `Keyword.Reserved`, `Punctuation`, `Name.Function` |
| `templateName`              | `Name.Label`                                       |
| `templateParameterDelegate` | `Name.Variable`, `Operator`, `Name.Variable`       |
| `postFormatName`            | `Name.Function`                                    |
| `BOOL`                      | `Keyword.Constant`                                 |
| `EMPTY`                     | `Keyword.Constant`                                 |
| `NULL`                      | `Keyword.Constant`                                 |
| `NUMBER`                    | `Number.Integer`                                   |
| `L_PAREN`                   | `Punctuation`                                      |
| `R_PAREN`                   | `Punctuation`                                      |
| `EQ`                        | `Operator`                                         |
| `NE`                        | `Operator`                                         |
| `LTE`                       | `Operator`                                         |
| `LT`                        | `Operator`                                         |
| `GT`                        | `Operator`                                         |
| `GTE`                       | `Operator`                                         |
| fragment `CtrlChar`         | `Whitespace`                                       |
 