# Codex Build Prompt: MQ Schema Toolkit (JDK21 + Spring Boot 3.x)

You are implementing a **multi-module Maven project** called `mq-schema-toolkit` for IBM MQ fixed-length messages.

The goal is a schema-driven toolchain that:
- Imports Excel message specs into an IR YAML schema (request/response separated)
- Generates Java POJOs (with public getters/setters)
- Generates MQ converter XML DSL (inbound/outbound separated, similar to provided sample DSL)
- Generates OpenAPI 3.x YAML from IR
- Provides a runtime SDK to marshal/unmarshal fixed-length MQ messages using IR
- Provides a diff engine that highlights differences and maps them back to raw offsets/lengths
- Uses **file-system storage only**; schemas are Git-managed (no database)

---

## A. Non-negotiable confirmed requirements

### Runtime/Platform
- JDK: **21**
- Spring Boot: **3.x** (prefer 3.2+)
- Storage: file system only (Git-managed schema repo). No DB.

### Message/Protocol rules (frozen)
- Messages are **fixed-length concatenation**, no delimiter.
- Segments are logical groupings.
- Each segment begins with **protocol fields**:
    - `groupid`: String, **10 bytes**, value extracted from Excel column `description`
    - `occurenceCount`: Unsigned integer, **4 bytes**, **binary uint32**, **BIG_ENDIAN default**, configurable.
- Strings:
    - Right pad with spaces
    - Null string = all spaces
- Integers:
    - Binary unsigned integer
    - Use **Java long** to represent unsigned values
    - Read uint32: `long v = Integer.toUnsignedLong(buffer.getInt())`
    - Write uint32: validate 0..0xFFFFFFFF then `buffer.putInt((int)v)`
- Dynamic arrays:
    - Each segment has its own `occurenceCount`
    - repeat rules:
        - `1..1` => composite object
        - `0..N` or `1..N` => List
- Java POJOs:
    - Must generate **public getters and setters** for all business fields
    - Must NOT expose `groupid` / `occurenceCount` as business fields by default (protocol-only)

---

## B. Required outputs

### 1) Excel -> IR YAML
- request and response separated
- file naming:
    - `<operationId>-request-v<version>.yaml`
    - `<operationId>-response-v<version>.yaml`
- schemaId naming:
    - `<operationId>:<direction>:v<version>` where direction is `request|response`
- IR must preserve:
    - field name, original name, lengthBytes, datatype, required(O/M), nullable, examples, format/converter overrides
    - segment name, groupIdValue extracted from description, repeat rule inferred from occurrence notation

### 2) IR -> Java POJOs
- One class per segment
- Nested segments:
    - 1..1 => field type is segment class
    - 0..N / 1..N => `List<SegmentClass>`
- All business properties must have public getter/setter
- Do not generate groupid/occurenceCount properties by default

### 3) IR -> Converter XML DSL (two files)
Generate:
- `mq-request-converters.xml` (outbound)
- `mq-response-converters.xml` (inbound)

XML must resemble the sample DSL style (DataField / CompositeField / RepeatingField).
Key rules:
- For each segment, emit protocol fields **first** in order:
    1) groupid as DataField length=10 transitory defaultValue=groupIdValue converter=stringFieldConverter
    2) occurenceCount as DataField length=4 transitory converter=counterFieldConverter (binary uint32)
- RepeatingField must be **dynamic**: do **not** hardcode fixedCount.
  Runtime uses the segment’s occurenceCount.

### 4) IR -> OpenAPI 3.x YAML
- Map each segment to a schema under `components.schemas`
- requestBody references request root schema
- 200 response references response root schema
- Use required/maxLength/example/description derived from IR

### 5) Runtime SDK: schema-driven converter
- `marshal(Object pojo, MessageSchema schema) -> byte[]`
- `unmarshal(byte[] bytes, MessageSchema schema, Class<T>) -> ConversionResult<T>`
- Must generate FieldTrace:
    - path, startOffset, length
    - Paths include list indices: `/SegmentA[0]/childB[2]/fieldX`
- Must validate:
    - groupid discriminator matches schema (strict mode default, allow config to WARN)
    - single segment must have occurenceCount==1

### 6) Diff engine + HTML report
- Parse expected/actual into neutral tree (Map/List) plus trace map
- Diff by path:
    - missing required, mismatched values, type errors
    - arrays diff by index (provide extension hook for key-based matching later)
- Map diffs to offsets using trace
- Render HTML report:
    - diff table (path/kind/expected/actual/offsets)
    - (optional) show raw message panes with highlighted spans

### 7) CLI (picocli)
Commands:
- `import-excel`
- `validate`
- `gen-java`
- `gen-xml`
- `gen-openapi`
- `diff`

Defaults align with `schema-repo-example/` directory layout.
All commands support explicit `--baseDir` / `--schemaDir` overrides.

---

## C. Extensibility (must implement)
### Converter mapping config
File: `config/converter-mapping.yaml`
- datatype -> converter mapping
- field-level override by path/name
- fallback converter

### Groupid extraction config
File: `config/groupid-extraction.yaml`
- list of regex patterns to extract groupid from description
- fallback: first token length=10

### Generation report
Output: `generation-report.json` containing:
- extracted groupid per segment path
- any fallback converter usage with field path and reason
- warnings/errors with context

---

## D. Excel input assumptions (based on sample)
- Sheets: Shared Header / Request / Response
- Table headers include:
    - Seg lvl, Field Name, Length, Messaging Datatype, Opt(O/M), Null, Sample Value, description (or synonyms)
- Segment rows: length and datatype are blank
- Field rows: length+datatype present
- Seg lvl defines nesting (build AST using a stack)

Implement a tolerant header mapper:
- normalize column names (lowercase, remove spaces/punct)
- allow synonyms (datatype, messagingdatatype, etc.)
  If required columns missing: fail with actionable error.

---

## E. Module boundaries (do not collapse)
Keep Maven multi-module:
- mq-ir-model
- mq-spec-ingest-excel
- mq-codegen
- mq-runtime-converter
- mq-diff
- mq-cli
- mq-web (optional stub)

---

## F. Acceptance criteria
- `mvn -q test` passes
- `mqtool --help` works
- Provide at least one end-to-end example in `schema-repo-example/`:
    - minimal IR YAML
    - generated Java/XML/OpenAPI
    - diff report from two sample messages
- Errors are clear and actionable
- No database dependencies
- Public APIs have Javadoc
