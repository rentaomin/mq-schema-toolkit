# mq-schema-toolkit

Schema-driven toolkit for IBM MQ fixed-length messages (JDK21 + Spring Boot 3.x).

## Requirements

- JDK 21
- Maven 3.9+ (or use the Maven wrapper `mvnw`)

## Build and Test

Windows (PowerShell):

```powershell
.\mvnw -q clean test
```

macOS/Linux:

```bash
./mvnw -q clean test
```

If you see `release 21 is not supported`, install JDK 21 and ensure `JAVA_HOME` points to it.

## Spring Boot API

The REST API is served from the `mq-web` module.

Windows (PowerShell):

```powershell
.\mvnw -q -pl mq-web -am spring-boot:run
```

macOS/Linux:

```bash
./mvnw -q -pl mq-web -am spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/api/health
```

## Configuration

Default Spring Boot config lives in `mq-web/src/main/resources/application.yml` and can be overridden by environment variables.
Schema repository configuration is read from `schema-repo-example/config/` (or your configured baseDir):

- `converter-mapping.yaml` datatype -> converter mapping and overrides
- `groupid-extraction.yaml` groupId extraction rules
- `protocol.yaml` protocol field lengths and byte order
- `importer.yaml` header mapping + extra columns capture
- `xml-template.yaml` XML namespace and converter tags (optional template path)

The XML generator uses a FreeMarker template at `mq-codegen/src/main/resources/templates/converter-xml.ftl`.
You can override it by setting `templatePath` in `xml-template.yaml` or `mqtool.xmlTemplatePath` in `application.yml`.
Extra Excel columns are preserved into IR under `extensions` on segments/fields.
Shared header key/value pairs are stored under `sharedHeader` on the schema.
Marshal/unmarshal can be customized by providing a Spring `MessageCodec` bean (default is `MessageConverter`).

### REST Examples

Import Excel:

```bash
curl -X POST http://localhost:8080/api/import-excel ^
  -H "Content-Type: application/json" ^
  -d "{\"excelPath\":\"sample/create_app.xlsx\",\"baseDir\":\"schema-repo-example\"}"
```

Generate Java:

```bash
curl -X POST http://localhost:8080/api/gen-java ^
  -H "Content-Type: application/json" ^
  -d "{\"baseDir\":\"schema-repo-example\",\"basePackage\":\"com.rtm.mq.generated\"}"
```

Generate XML:

```bash
curl -X POST http://localhost:8080/api/gen-xml ^
  -H "Content-Type: application/json" ^
  -d "{\"baseDir\":\"schema-repo-example\",\"basePackage\":\"com.rtm.mq.generated\"}"
```

Generate OpenAPI:

```bash
curl -X POST http://localhost:8080/api/gen-openapi ^
  -H "Content-Type: application/json" ^
  -d "{\"baseDir\":\"schema-repo-example\"}"
```

Diff:

```bash
curl -X POST http://localhost:8080/api/diff ^
  -H "Content-Type: application/json" ^
  -d "{\"schemaPath\":\"schema-repo-example/schemas/SampleOp-response-v1.0.yaml\",\"expectedPath\":\"schema-repo-example/messages/expected.bin\",\"actualPath\":\"schema-repo-example/messages/actual.bin\",\"rootClass\":\"com.rtm.mq.generated.SampleOpResponse\",\"outputPath\":\"schema-repo-example/reports/diff-report.html\"}"
```

## CLI Usage

The CLI is in module `mq-cli` and runs as `mqtool`.

Build all modules and run the CLI:

Windows (PowerShell):

```powershell
.\mvnw -q -pl mq-cli -am package
java -cp mq-cli\target\classes;mq-codegen\target\classes;mq-spec-ingest-excel\target\classes;mq-ir-model\target\classes;mq-runtime-converter\target\classes;mq-diff\target\classes com.rtm.mq.cli.MqTool --help
```

macOS/Linux:

```bash
./mvnw -q -pl mq-cli -am package
java -cp mq-cli/target/classes:mq-codegen/target/classes:mq-spec-ingest-excel/target/classes:mq-ir-model/target/classes:mq-runtime-converter/target/classes:mq-diff/target/classes com.rtm.mq.cli.MqTool --help
```

## Example Workflow (sample/create_app.xlsx)

The `schema-repo-example/` directory is the default baseDir layout.

1) Import Excel to IR YAML:

```bash
java -cp <classpath> com.rtm.mq.cli.MqTool import-excel --excel sample/create_app.xlsx --baseDir schema-repo-example
```

2) Generate Java POJOs:

```bash
java -cp <classpath> com.rtm.mq.cli.MqTool gen-java --baseDir schema-repo-example --basePackage com.rtm.mq.generated
```

3) Generate converter XML:

```bash
java -cp <classpath> com.rtm.mq.cli.MqTool gen-xml --baseDir schema-repo-example --basePackage com.rtm.mq.generated
```

4) Generate OpenAPI:

```bash
java -cp <classpath> com.rtm.mq.cli.MqTool gen-openapi --baseDir schema-repo-example
```

## Diff Example

Given two binary message files and a schema:

```bash
java -cp <classpath> com.rtm.mq.cli.MqTool diff --schema schema-repo-example/schemas/SampleOp-response-v1.0.yaml --expected schema-repo-example/messages/expected.bin --actual schema-repo-example/messages/actual.bin --class com.rtm.mq.generated.SampleOpResponse --output schema-repo-example/reports/diff-report.html
```

Open the HTML file to view path-level diffs with byte offsets.

## Directory Layout (default)

- `schema-repo-example/config/` config YAMLs
- `schema-repo-example/schemas/` IR YAML schemas
- `schema-repo-example/generated/` generated Java/XML/OpenAPI
- `schema-repo-example/reports/` diff report output
