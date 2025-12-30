package com.rtm.mq.toolkit.codegen;

import com.rtm.mq.toolkit.ir.FieldNode;
import com.rtm.mq.toolkit.ir.NameUtils;
import com.rtm.mq.toolkit.ir.SchemaElement;
import com.rtm.mq.toolkit.ir.SegmentNode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates Java POJOs for schema segments.
 */
public final class JavaPojoGenerator {
    public void generate(SegmentNode root, Path outputDir, String basePackage) throws IOException {
        Map<String, SegmentNode> segments = new LinkedHashMap<>();
        collectSegments(root, segments);

        for (SegmentNode segment : segments.values()) {
            TypeSpec typeSpec = buildType(segment, basePackage);
            JavaFile javaFile = JavaFile.builder(basePackage, typeSpec)
                    .indent("    ")
                    .build();
            javaFile.writeTo(outputDir);
        }
    }

    private void collectSegments(SegmentNode segment, Map<String, SegmentNode> segments) {
        segments.putIfAbsent(segment.getName(), segment);
        for (SchemaElement element : segment.getElements()) {
            if (element instanceof SegmentNode child) {
                collectSegments(child, segments);
            }
        }
    }

    private TypeSpec buildType(SegmentNode segment, String basePackage) {
        TypeSpec.Builder builder = TypeSpec.classBuilder(segment.getName())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Auto-generated segment for $L.\n",
                        segment.getOriginalName() != null ? segment.getOriginalName() : segment.getName());

        for (SchemaElement element : segment.getElements()) {
            if (element instanceof FieldNode field) {
                if (field.isProtocol()) {
                    continue;
                }
                addField(builder, field, resolveFieldType(field));
            } else if (element instanceof SegmentNode childSegment) {
                String propertyName = NameUtils.toLowerCamel(childSegment.getName());
                TypeName typeName = resolveSegmentType(childSegment, basePackage);
                addField(builder, propertyName, typeName);
            }
        }

        return builder.build();
    }

    private void addField(TypeSpec.Builder builder, FieldNode field, TypeName typeName) {
        addField(builder, field.getName(), typeName);
    }

    private void addField(TypeSpec.Builder builder, String fieldName, TypeName typeName) {
        FieldSpec fieldSpec = FieldSpec.builder(typeName, fieldName, Modifier.PRIVATE).build();
        builder.addField(fieldSpec);

        String methodSuffix = NameUtils.toUpperCamel(fieldName);
        builder.addMethod(MethodSpec.methodBuilder("get" + methodSuffix)
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addJavadoc("Gets $L.\n", fieldName)
                .addStatement("return $N", fieldName)
                .build());

        builder.addMethod(MethodSpec.methodBuilder("set" + methodSuffix)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(typeName, fieldName)
                .addJavadoc("Sets $L.\n", fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build());
    }

    private TypeName resolveFieldType(FieldNode field) {
        String datatype = field.getDatatype() != null ? field.getDatatype().trim().toLowerCase() : "";
        if (datatype.contains("unsigned") || datatype.contains("integer") || datatype.contains("int")) {
            return ClassName.get(Long.class);
        }
        if (datatype.contains("decimal") || datatype.contains("bigdecimal")) {
            return ClassName.get(BigDecimal.class);
        }
        return ClassName.get(String.class);
    }

    private TypeName resolveSegmentType(SegmentNode segment, String basePackage) {
        ClassName segmentType = ClassName.get(basePackage, segment.getName());
        if (segment.getOccurrence() != null && segment.getOccurrence().isRepeating()) {
            return ParameterizedTypeName.get(ClassName.get(List.class), segmentType);
        }
        return segmentType;
    }
}
