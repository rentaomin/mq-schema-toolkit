package com.rtm.mq.web;

import com.rtm.mq.spec.excel.ImportResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;

/**
 * REST API for toolkit operations.
 */
@RestController
@RequestMapping("/api")
public class ToolkitController {
    private final ToolkitService service;
    private final ToolkitProperties properties;

    public ToolkitController(ToolkitService service, ToolkitProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/import-excel")
    public ResponseEntity<?> importExcel(@RequestBody ImportExcelRequest request) throws Exception {
        Path baseDir = baseDirOrDefault(request.baseDir());
        Path schemaDir = schemaDirOrDefault(baseDir, request.schemaDir());
        ImportResult result = service.importExcel(
                Path.of(request.excelPath()),
                baseDir,
                schemaDir
        );
        return ResponseEntity.ok(Map.of(
                "requestSchemaId", result.request() != null ? result.request().getSchemaId() : null,
                "responseSchemaId", result.response() != null ? result.response().getSchemaId() : null
        ));
    }

    @PostMapping("/gen-java")
    public ResponseEntity<?> genJava(@RequestBody GenJavaRequest request) throws Exception {
        Path baseDir = baseDirOrDefault(request.baseDir());
        Path schemaDir = schemaDirOrDefault(baseDir, request.schemaDir());
        service.generateJava(
                baseDir,
                schemaDir,
                request.basePackage() != null ? request.basePackage() : properties.getBasePackage()
        );
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/gen-xml")
    public ResponseEntity<?> genXml(@RequestBody GenXmlRequest request) throws Exception {
        Path baseDir = baseDirOrDefault(request.baseDir());
        Path schemaDir = schemaDirOrDefault(baseDir, request.schemaDir());
        service.generateXml(
                baseDir,
                schemaDir,
                request.basePackage() != null ? request.basePackage() : properties.getBasePackage()
        );
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/gen-openapi")
    public ResponseEntity<?> genOpenApi(@RequestBody GenOpenApiRequest request) throws Exception {
        Path baseDir = baseDirOrDefault(request.baseDir());
        Path schemaDir = schemaDirOrDefault(baseDir, request.schemaDir());
        service.generateOpenApi(
                baseDir,
                schemaDir
        );
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/diff")
    public ResponseEntity<?> diff(@RequestBody DiffRequest request) throws Exception {
        service.diffMessages(
                Path.of(request.schemaPath()),
                Path.of(request.expectedPath()),
                Path.of(request.actualPath()),
                request.rootClass(),
                request.outputPath() != null ? Path.of(request.outputPath()) : null
        );
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private Path baseDirOrDefault(String baseDir) {
        String resolved = baseDir != null ? baseDir : properties.getBaseDir();
        return Path.of(resolved);
    }

    private Path schemaDirOrDefault(Path baseDir, String schemaDir) {
        if (schemaDir != null) {
            return Path.of(schemaDir);
        }
        if (properties.getSchemaDir() != null) {
            return Path.of(properties.getSchemaDir());
        }
        return baseDir.resolve("schemas");
    }

    public record ImportExcelRequest(String excelPath, String baseDir, String schemaDir) {
    }

    public record GenJavaRequest(String baseDir, String schemaDir, String basePackage) {
    }

    public record GenXmlRequest(String baseDir, String schemaDir, String basePackage) {
    }

    public record GenOpenApiRequest(String baseDir, String schemaDir) {
    }

    public record DiffRequest(String schemaPath,
                              String expectedPath,
                              String actualPath,
                              String rootClass,
                              String outputPath) {
    }
}
