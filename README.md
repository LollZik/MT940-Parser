# MT940 Parser

A Java library that reads **MT940 bank statements** (the SWIFT text format exported by most banks) and turns them into immutable Java objects, then checks whether each statement is internally consistent.

```
text file  ──►  Tokenizer  ──►  Mt940Parser  ──►  List<Statement>  ──►  StatementValidator  ──►  List<ValidationIssue>
```

Built with plain Java 25 as a learning and portfolio project.

---

## Features

- Parses statements with tags `:20:`, `:25:`, `:28C:`, `:60F:`, `:61:`, `:86:`, `:62F:`
- Handles multiple statements in one file, multi-line fields, the optional second line of `:61:`
- Handles reversals (`RC`, `RD`), optional entry date with year rollover, optional funds code, `NONREF`
- Supports any file encoding chosen by the caller (e.g. UTF-8, Windows-1250), LF and CRLF line endings, UTF-8 BOM
- Parse errors report the **line number** in the file and keep the original exception as the cause
- Separate validation step: balance equation, currency consistency, date order

## Requirements

- Java 25
- Maven 3.9+

## Usage

### Build

```bash
mvn package
```

This runs all tests and creates `target/mt940-parser.jar`.

### Run

```bash
java -jar target/mt940-parser.jar <file> [charset]
```

If no charset is given, UTF-8 is used. Polish bank exports often use Windows-1250:

```bash
java -jar target/mt940-parser.jar statement.sta windows-1250
```

Validation warnings are printed to standard error.

### Exit codes

| Code | Meaning |
|---|---|
| 0 | file parsed, no validation issues |
| 1 | file parsed, validation issues found |
| 2 | invalid file (`Mt940ParseException`) |
| 3 | invalid arguments, unknown charset or file cannot be read |

## Data model

All model types are immutable records in `mt940parser.model`.

| Type | Contents                                                                                                                                                                                     |
|---|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Statement` | reference (`:20:`), account (`:25:`), statement number (`:28C:`), opening balance (`:60F:`), transactions (`:61:`), closing balance (`:62F:`), optional statement-level information (`:86:`) |
| `Balance` | debit/credit mark, date, currency, amount; `signedAmount()`                                                                                                                                  |
| `Transaction` | value date, optional entry date, mark, reversal flag, amount, transaction type, optional client reference, bank reference, supplementary details and information; `signedAmount()`           |
| `DebitCredit` | `CREDIT` / `DEBIT`; applies the sign to an amount                                                                                                                                            |

## Supported tags

| Tag                   | Meaning | Handling |
|-----------------------|---|---|
| `:20:`                | statement reference | parsed |
| `:25:`                | account identification | parsed |
| `:28C:`               | statement / sequence number | parsed (kept as text) |
| `:60F:`               | opening balance | parsed |
| `:61:`                | statement line (transaction) | parsed, including the optional second line |
| `:86:`                | information to account owner | attached to the preceding `:61:`, or to the statement when it follows `:62F:` |
| `:62F:`               | closing balance | parsed |
| `:21:`, `:64:`, `:65:` | related reference, available balances | recognised and skipped |
| Other                 | - | `Mt940ParseException` |

## Project structure

```
src/main/java/mt940parser
├── cli          Main (Command-line interface)
├── model        Statement, Balance, Transaction, DebitCredit
├── parser       Mt940Parser (public entry point), Mt940ParseException,
│                Tokenizer, FieldParser, RawField, RawStatement (internal)
└── validation   StatementValidator, ValidationIssue, IssueType
```

Only `Mt940Parser` and `Mt940ParseException` are public in the `parser` package; the tokenizer, field parsers and raw records are package-private implementation details.

## Design decisions

**Three separate stages.** The tokenizer only splits text into fields (tag, value, starting line) and knows nothing about their meaning. The parser interprets fields and builds the model. The validator checks a finished model. Each stage is tested on its own.

**Syntax errors throw, inconsistencies warn.** A file that cannot be read structurally (missing `:62F:`, invalid date, unknown tag) throws a checked `Mt940ParseException`. A statement whose balances do not add up is still returned - the validator reports it as a `ValidationIssue`, so the caller can see the data and decide what to do.

**Checked exception.** A malformed bank file is not a programming error and the caller should handle it explicitly. The exception carries the line number (`getLine()`) and the original cause (e.g. `DateTimeException`).

**Money as `BigDecimal`.** `double` cannot represent decimal fractions exactly. Amounts keep the scale from the file (`1500,` → `1500`, `10000,00` → `10000.00`); the validator compares values with `compareTo`, so the scale does not matter there.

**Immutable model.** Records with validation in compact constructors (no `null`, no negative amounts). Lists returned by the parser and the validator are unmodifiable (`List.copyOf`).

**File-format details stay in the parser.** The model contains domain concepts only: `C`/`D` become `DebitCredit`, `RC`/`RD` become `DebitCredit` + reversal flag, `NONREF` becomes an empty `Optional`, the `//` separator is not part of the bank reference.

**Optional values as `Optional`.** Entry date, references, supplementary details and information may be absent in the file, and the type says so.

**I/O separated from logic.** The core works on a list of lines; reading a file is a thin wrapper. The caller chooses the encoding.

## Format assumptions

- Two-digit years are interpreted as 2000–2099.
- The entry date (`MMDD`) has no year. It takes the year of the value date, unless the months differ by more than 10 (e.g. value date 31 Dec, entry date 2 Jan) - then the adjacent year is used. This assumes both dates are close to each other.
- The decimal comma in amounts is mandatory (`1500,` is valid, `1500` is not); the number of decimal digits is not limited.
- The second line of `:61:` follows the SWIFT definition (one line, up to 34 characters); longer or multi-line content is rejected.
- Lines of a multi-line `:86:` are joined with `\n`; the content is kept as raw text.
- A file may end without the final `-`.
- Blank lines are ignored; a UTF-8 BOM at the start of the file is removed.

## Limitations and possible extensions

- Structured parsing of `:86:` (title, counterparty, account) - subfield separators differ between banks
- Multi-part statements (`:60M:` / `:62M:`)
- SWIFT block headers (`{1:...}{2:...}{4:`)
- IBAN validation of `:25:`
- Export to JSON / CSV

## Tests

Unit tests (JUnit 6) cover every layer: model records, the tokenizer, field parsers, statement assembly and the validator. Sample statements in `src/test/resources` include multiple statements in one file, reversals, year rollover, a balance mismatch, a missing closing balance, Windows-1250 and UTF-8 with BOM, LF and CRLF. A `.gitattributes` rule keeps their line endings unchanged.