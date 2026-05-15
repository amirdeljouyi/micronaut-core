/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.scala.processing.visitor;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.AbstractAnnotationMetadataBuilder;
import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import io.micronaut.inject.visitor.VisitorContext;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds Micronaut annotation metadata from the reduced Scala compiler model.
 */
public final class ScalaAnnotationMetadataBuilder extends AbstractAnnotationMetadataBuilder<Object, ScalaAnnotationData> {

    private final VisitorContext visitorContext;
    private final ClassLoader classLoader;
    private final Map<String, ScalaAnnotationTypeData> nativeAnnotationTypes = new LinkedHashMap<>();

    public ScalaAnnotationMetadataBuilder(VisitorContext visitorContext, ClassLoader classLoader) {
        this.visitorContext = visitorContext;
        this.classLoader = classLoader;
    }

    /**
     * Build metadata for a Scala model element.
     *
     * @param element The element
     * @return The annotation metadata
     */
    public MutableAnnotationMetadata buildMetadata(ScalaAnnotatedElementData element) {
        registerAnnotationTypes(element.annotations());
        return MutableAnnotationMetadata.of(buildInternal(element));
    }

    @Override
    protected Object getTypeForAnnotation(ScalaAnnotationData annotationMirror) {
        return annotationType(annotationMirror);
    }

    @Override
    protected boolean hasAnnotation(Object element, Class<? extends Annotation> annotation) {
        return hasAnnotation(element, annotation.getName());
    }

    @Override
    protected boolean hasAnnotation(Object element, String annotation) {
        return getAnnotationsForType(element).stream()
            .anyMatch(annotationData -> annotationData.name().equals(annotation));
    }

    @Override
    protected boolean hasAnnotations(Object element) {
        return !getAnnotationsForType(element).isEmpty();
    }

    @Override
    protected String getAnnotationTypeName(ScalaAnnotationData annotationMirror) {
        return annotationMirror.name();
    }

    @Override
    protected String getElementName(Object element) {
        if (element instanceof ScalaAnnotatedElementData annotated) {
            return annotated.name();
        }
        if (element instanceof AnnotationTypeElement annotationType) {
            return annotationType.name();
        }
        if (element instanceof AnnotationMemberElement memberElement) {
            return memberElement.name();
        }
        if (element instanceof UnresolvedAnnotationMember unresolvedAnnotationMember) {
            return unresolvedAnnotationMember.name();
        }
        return String.valueOf(element);
    }

    @Override
    protected List<? extends ScalaAnnotationData> getAnnotationsForType(Object element) {
        if (element instanceof ScalaAnnotatedElementData annotated) {
            return annotated.annotations();
        }
        if (element instanceof AnnotationTypeElement annotationType) {
            ScalaAnnotationTypeData nativeType = annotationType.nativeType();
            if (nativeType != null && !nativeType.annotations().isEmpty()) {
                return nativeType.annotations();
            }
            if (annotationType.annotationClass() != null) {
                return runtimeAnnotations(annotationType.annotationClass().getAnnotations());
            }
        }
        if (element instanceof AnnotationMemberElement memberElement) {
            ScalaAnnotationMemberData nativeMember = memberElement.nativeMember();
            if (nativeMember != null && !nativeMember.annotations().isEmpty()) {
                return nativeMember.annotations();
            }
            Method method = memberElement.method();
            return method == null ? Collections.emptyList() : runtimeAnnotations(method.getAnnotations());
        }
        return Collections.emptyList();
    }

    @Override
    protected List<Object> buildHierarchy(Object element, boolean inheritTypeAnnotations, boolean declaredOnly) {
        return List.of(element);
    }

    @Override
    protected void readAnnotationRawValues(
        Object originatingElement,
        String annotationName,
        Object member,
        String memberName,
        Object annotationValue,
        Map<CharSequence, Object> annotationValues) {
        readAnnotationRawValues(originatingElement, annotationName, member, memberName, annotationValue, annotationValues, new LinkedHashMap<>());
    }

    @Override
    protected void readAnnotationRawValues(
        Object originatingElement,
        String annotationName,
        Object member,
        String memberName,
        Object annotationValue,
        Map<CharSequence, Object> annotationValues,
        Map<String, Map<CharSequence, Object>> resolvedDefaults) {
        if (memberName != null && annotationValue != null && !containsAnnotationValue(annotationValues, memberName)) {
            Object resolvedValue = normalizeValue(originatingElement, member, annotationName, memberName, annotationValue, resolvedDefaults);
            if (resolvedValue != null) {
                if (isEvaluatedExpression(resolvedValue)) {
                    resolvedValue = buildEvaluatedExpressionReference(originatingElement, annotationName, memberName, resolvedValue);
                }
                validateAnnotationValue(originatingElement, annotationName, member, memberName, resolvedValue);
                annotationValues.put(memberName, resolvedValue);
            }
        }
    }

    @Override
    protected boolean isValidationRequired(Object member) {
        return isValidationRequired(member, new ArrayList<>());
    }

    @Override
    protected void addError(Object originatingElement, String error) {
        visitorContext.fail(error, null);
    }

    @Override
    protected void addWarning(Object originatingElement, String warning) {
        visitorContext.warn(warning, null);
    }

    @Override
    protected Object readAnnotationValue(Object originatingElement, Object member, String annotationName, String memberName, Object annotationValue) {
        return normalizeValue(originatingElement, member, annotationName, memberName, annotationValue, new LinkedHashMap<>());
    }

    @Override
    protected Map<? extends Object, ?> readAnnotationDefaultValues(String annotationName, Object annotationType) {
        AnnotationTypeElement typeElement = annotationType(annotationName, annotationType);
        Map<Object, Object> values = new LinkedHashMap<>();
        ScalaAnnotationTypeData nativeType = typeElement.nativeType();
        if (nativeType != null) {
            for (ScalaAnnotationMemberData member : nativeType.members().values()) {
                Object defaultValue = member.defaultValue();
                if (isValidDefaultValue(defaultValue)) {
                    values.put(new AnnotationMemberElement(typeElement, member, findMember(typeElement.annotationClass(), member.name())), defaultValue);
                }
            }
        }
        Class<?> annotationClass = typeElement.annotationClass();
        if (annotationClass == null) {
            return values;
        }
        for (Method method : annotationClass.getDeclaredMethods()) {
            Object defaultValue = method.getDefaultValue();
            if (isValidDefaultValue(defaultValue) && !containsMember(values, method.getName())) {
                values.put(new AnnotationMemberElement(typeElement, null, method), defaultValue);
            }
        }
        return values;
    }

    @Override
    protected Map<? extends Object, ?> readAnnotationRawValues(ScalaAnnotationData annotationMirror) {
        if (annotationMirror.values().isEmpty()) {
            return Map.of();
        }
        Object annotationType = getTypeForAnnotation(annotationMirror);
        Map<Object, Object> values = new LinkedHashMap<>(annotationMirror.values().size());
        for (Map.Entry<CharSequence, Object> entry : annotationMirror.values().entrySet()) {
            Object member = getAnnotationMember(annotationType, entry.getKey());
            values.put(member == null ? new UnresolvedAnnotationMember(entry.getKey().toString()) : member, entry.getValue());
        }
        return values;
    }

    @Override
    protected <K extends Annotation> Optional<AnnotationValue<K>> getAnnotationValues(Object originatingElement, Object member, Class<K> annotationType) {
        if (member instanceof AnnotationMemberElement memberElement) {
            ScalaAnnotationMemberData nativeMember = memberElement.nativeMember();
            if (nativeMember != null) {
                for (ScalaAnnotationData annotation : nativeMember.annotations()) {
                    if (annotation.name().equals(annotationType.getName())) {
                        Map<CharSequence, Object> values = new LinkedHashMap<>();
                        for (Map.Entry<? extends Object, ?> entry : readAnnotationRawValues(annotation).entrySet()) {
                            Object annotationMember = entry.getKey();
                            readAnnotationRawValues(
                                originatingElement,
                                annotationType.getName(),
                                annotationMember,
                                getAnnotationMemberName(annotationMember),
                                entry.getValue(),
                                values);
                        }
                        return Optional.of(AnnotationValue.builder(annotationType).members(values).build());
                    }
                }
            }
            Method method = memberElement.method();
            if (method == null) {
                return Optional.empty();
            }
            String annotationName = annotationType.getName();
            for (Annotation annotation : method.getAnnotations()) {
                if (annotation.annotationType().getName().equals(annotationName)) {
                    Map<CharSequence, Object> values = new LinkedHashMap<>();
                    ScalaAnnotationData annotationData = runtimeAnnotation(annotation);
                    for (Map.Entry<? extends Object, ?> entry : readAnnotationRawValues(annotationData).entrySet()) {
                        Object annotationMember = entry.getKey();
                        readAnnotationRawValues(
                            originatingElement,
                            annotationName,
                            annotationMember,
                            getAnnotationMemberName(annotationMember),
                            entry.getValue(),
                            values);
                    }
                    return Optional.of(AnnotationValue.builder(annotationType).members(values).build());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    protected String getAnnotationMemberName(Object member) {
        if (member instanceof AnnotationMemberElement memberElement) {
            return memberElement.name();
        }
        if (member instanceof UnresolvedAnnotationMember unresolvedAnnotationMember) {
            return unresolvedAnnotationMember.name();
        }
        return String.valueOf(member);
    }

    @Override
    protected @Nullable String getRepeatableName(ScalaAnnotationData annotationMirror) {
        return repeatableContainerName(annotationType(annotationMirror));
    }

    @Override
    protected @Nullable String getRepeatableContainerNameForType(Object annotationType) {
        return repeatableContainerName(annotationType(annotationTypeName(annotationType), annotationType));
    }

    @Override
    protected Optional<Object> getAnnotationMirror(String annotationName) {
        return Optional.of(annotationType(annotationName));
    }

    @Override
    protected @Nullable String getOriginatingClassName(Object originatingElement) {
        if (originatingElement instanceof ScalaClassData classData) {
            return classData.name();
        }
        return null;
    }

    @Override
    protected @Nullable Object getAnnotationMember(Object annotationElement, CharSequence member) {
        AnnotationTypeElement typeElement = annotationType(annotationTypeName(annotationElement), annotationElement);
        ScalaAnnotationTypeData nativeType = typeElement.nativeType();
        if (nativeType != null) {
            ScalaAnnotationMemberData nativeMember = nativeType.members().get(member.toString());
            if (nativeMember != null) {
                return new AnnotationMemberElement(typeElement, nativeMember, findMember(typeElement.annotationClass(), nativeMember.name()));
            }
        }
        Class<?> annotationClass = typeElement.annotationClass();
        if (annotationClass == null) {
            return null;
        }
        for (Method method : annotationClass.getDeclaredMethods()) {
            if (method.getName().contentEquals(member)) {
                return new AnnotationMemberElement(typeElement, null, method);
            }
        }
        return null;
    }

    @Override
    protected VisitorContext getVisitorContext() {
        return visitorContext;
    }

    @Override
    protected RetentionPolicy getRetentionPolicy(Object annotation) {
        AnnotationTypeElement annotationType = annotationType(annotationTypeName(annotation), annotation);
        ScalaAnnotationTypeData nativeType = annotationType.nativeType();
        if (nativeType != null && nativeType.retentionPolicyName() != null) {
            return RetentionPolicy.valueOf(nativeType.retentionPolicyName());
        }
        Class<?> annotationClass = annotationType.annotationClass();
        if (annotationClass == null) {
            return RetentionPolicy.RUNTIME;
        }
        Retention retention = annotationClass.getAnnotation(Retention.class);
        return retention == null ? RetentionPolicy.CLASS : retention.value();
    }

    @Override
    protected boolean isExcludedAnnotation(Object element, String annotationName) {
        if (element instanceof AnnotationTypeElement && annotationName.startsWith("java.lang.annotation.")) {
            return false;
        }
        return super.isExcludedAnnotation(element, annotationName);
    }

    private boolean isValidationRequired(Object member, List<String> visited) {
        for (ScalaAnnotationData annotation : getAnnotationsForType(member)) {
            String annotationName = annotation.name();
            if (annotationName.startsWith("jakarta.validation")) {
                return true;
            }
            if (!visited.contains(annotationName)) {
                visited.add(annotationName);
                if (isValidationRequired(getTypeForAnnotation(annotation), visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Object normalizeValue(
        Object originatingElement,
        Object member,
        String annotationName,
        String memberName,
        Object value,
        Map<String, Map<CharSequence, Object>> resolvedDefaults) {
        if (member instanceof AnnotationMemberElement memberElement && memberElement.nativeMember() != null) {
            return normalizeNativeValue(originatingElement, memberElement.nativeMember(), value, resolvedDefaults);
        }
        Method method = member instanceof AnnotationMemberElement memberElement ? memberElement.method() : null;
        Class<?> expectedType = method == null ? null : method.getReturnType();
        Object resolvedValue = normalizeValue(originatingElement, expectedType, annotationName, memberName, value, resolvedDefaults);
        return resolvedValue == null ? value : resolvedValue;
    }

    private Object normalizeNativeValue(
        Object originatingElement,
        ScalaAnnotationMemberData nativeMember,
        Object value,
        Map<String, Map<CharSequence, Object>> resolvedDefaults) {
        if (nativeMember.array()) {
            List<Object> values = arrayValues(value);
            if (nativeMember.classType()) {
                AnnotationClassValue<?>[] converted = new AnnotationClassValue<?>[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    converted[i] = annotationClassValue(values.get(i));
                }
                return converted;
            }
            if (nativeMember.enumType()) {
                String[] converted = new String[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    converted[i] = enumValue(values.get(i));
                }
                return converted;
            }
            if (nativeMember.annotationType()) {
                AnnotationValue<?>[] converted = new AnnotationValue<?>[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    converted[i] = nestedAnnotationValue(originatingElement, nativeMember.typeName(), values.get(i), resolvedDefaults);
                }
                return converted;
            }
            return value;
        }
        if (nativeMember.classType()) {
            return annotationClassValue(value);
        }
        if (nativeMember.enumType()) {
            return enumValue(value);
        }
        if (nativeMember.annotationType()) {
            return nestedAnnotationValue(originatingElement, nativeMember.typeName(), value, resolvedDefaults);
        }
        return value;
    }

    @Nullable
    private Object normalizeValue(
        Object originatingElement,
        @Nullable Class<?> expectedType,
        String annotationName,
        String memberName,
        @Nullable Object value,
        Map<String, Map<CharSequence, Object>> resolvedDefaults) {
        if (value == null) {
            return null;
        }
        if (expectedType != null && expectedType.isArray()) {
            return normalizeArrayValue(originatingElement, expectedType.getComponentType(), annotationName, memberName, value, resolvedDefaults);
        }
        if (expectedType != null && Class.class.equals(expectedType)) {
            return annotationClassValue(value);
        }
        if (expectedType != null && expectedType.isEnum()) {
            return enumValue(value);
        }
        if (expectedType != null && expectedType.isAnnotation()) {
            return nestedAnnotationValue(originatingElement, expectedType, value, resolvedDefaults);
        }
        if (value instanceof Class<?> || value instanceof AnnotationClassValue<?>) {
            return annotationClassValue(value);
        }
        if (value instanceof Class<?>[] types) {
            return annotationClassValues(types);
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Annotation annotation) {
            return nestedAnnotationValue(originatingElement, annotation.annotationType(), annotation, resolvedDefaults);
        }
        if (value instanceof Annotation[] annotations) {
            AnnotationValue<?>[] values = new AnnotationValue<?>[annotations.length];
            for (int i = 0; i < annotations.length; i++) {
                values[i] = nestedAnnotationValue(originatingElement, annotations[i].annotationType(), annotations[i], resolvedDefaults);
            }
            return values;
        }
        if (value instanceof AnnotationValue<?> annotationValue) {
            return nestedAnnotationValue(originatingElement, annotationValue, resolvedDefaults);
        }
        if (value instanceof AnnotationValue<?>[] annotationValues) {
            AnnotationValue<?>[] values = new AnnotationValue<?>[annotationValues.length];
            for (int i = 0; i < annotationValues.length; i++) {
                values[i] = nestedAnnotationValue(originatingElement, annotationValues[i], resolvedDefaults);
            }
            return values;
        }
        return value;
    }

    private Object normalizeArrayValue(
        Object originatingElement,
        Class<?> componentType,
        String annotationName,
        String memberName,
        Object value,
        Map<String, Map<CharSequence, Object>> resolvedDefaults) {
        List<Object> values = arrayValues(value);
        if (Class.class.equals(componentType)) {
            AnnotationClassValue<?>[] converted = new AnnotationClassValue<?>[values.size()];
            for (int i = 0; i < values.size(); i++) {
                converted[i] = annotationClassValue(values.get(i));
            }
            return converted;
        }
        if (componentType.isEnum()) {
            String[] converted = new String[values.size()];
            for (int i = 0; i < values.size(); i++) {
                converted[i] = enumValue(values.get(i));
            }
            return converted;
        }
        if (componentType.isAnnotation()) {
            AnnotationValue<?>[] converted = new AnnotationValue<?>[values.size()];
            for (int i = 0; i < values.size(); i++) {
                converted[i] = nestedAnnotationValue(originatingElement, componentType, values.get(i), resolvedDefaults);
            }
            return converted;
        }
        Object converted = Array.newInstance(componentType, values.size());
        for (int i = 0; i < values.size(); i++) {
            Object normalized = normalizeValue(originatingElement, componentType, annotationName, memberName, values.get(i), resolvedDefaults);
            Array.set(converted, i, normalized);
        }
        return converted;
    }

    private List<Object> arrayValues(Object value) {
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(Array.get(value, i));
            }
            return values;
        }
        return List.of(value);
    }

    private AnnotationClassValue<?> annotationClassValue(Object value) {
        if (value instanceof AnnotationClassValue<?> annotationClassValue) {
            return annotationClassValue;
        }
        if (value instanceof Class<?> type) {
            return new AnnotationClassValue<>(type);
        }
        return new AnnotationClassValue<>(String.valueOf(value));
    }

    private AnnotationClassValue<?>[] annotationClassValues(Class<?>[] types) {
        AnnotationClassValue<?>[] values = new AnnotationClassValue<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            values[i] = new AnnotationClassValue<>(types[i]);
        }
        return values;
    }

    private String enumValue(Object value) {
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return String.valueOf(value);
    }

    private AnnotationValue<?> nestedAnnotationValue(
        Object originatingElement,
        Class<?> expectedType,
        Object value,
        Map<String, Map<CharSequence, Object>> resolvedDefaults) {
        return nestedAnnotationValue(originatingElement, expectedType.getName(), value, resolvedDefaults);
    }

    private AnnotationValue<?> nestedAnnotationValue(
        Object originatingElement,
        String expectedTypeName,
        Object value,
        Map<String, Map<CharSequence, Object>> resolvedDefaults) {
        if (value instanceof Annotation annotation) {
            return readNestedAnnotationValue(originatingElement, runtimeAnnotation(annotation), resolvedDefaults);
        }
        if (value instanceof AnnotationValue<?> annotationValue) {
            return nestedAnnotationValue(originatingElement, annotationValue, resolvedDefaults);
        }
        if (value instanceof ScalaAnnotationData annotationData) {
            return readNestedAnnotationValue(originatingElement, annotationData, resolvedDefaults);
        }
        return AnnotationValue.builder(expectedTypeName).build();
    }

    private AnnotationValue<?> nestedAnnotationValue(
        Object originatingElement,
        AnnotationValue<?> annotationValue,
        Map<String, Map<CharSequence, Object>> resolvedDefaults) {
        return readNestedAnnotationValue(
            originatingElement,
            new ScalaAnnotationData(annotationValue.getAnnotationName(), annotationValue.getValues()),
            resolvedDefaults);
    }

    private List<ScalaAnnotationData> runtimeAnnotations(Annotation[] annotations) {
        if (annotations.length == 0) {
            return List.of();
        }
        List<ScalaAnnotationData> annotationData = new ArrayList<>(annotations.length);
        for (Annotation annotation : annotations) {
            annotationData.add(runtimeAnnotation(annotation));
        }
        return List.copyOf(annotationData);
    }

    private boolean containsAnnotationValue(Map<CharSequence, Object> annotationValues, String memberName) {
        for (CharSequence key : annotationValues.keySet()) {
            if (memberName.contentEquals(key)) {
                return true;
            }
        }
        return false;
    }

    private ScalaAnnotationData runtimeAnnotation(Annotation annotation) {
        return new ScalaAnnotationData(annotation.annotationType().getName(), readRuntimeAnnotationValues(annotation));
    }

    private Map<CharSequence, Object> readRuntimeAnnotationValues(Annotation annotation) {
        Map<CharSequence, Object> values = new LinkedHashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            try {
                Object value = method.invoke(annotation);
                if (value != null && !annotationValueEquals(value, method.getDefaultValue())) {
                    values.put(method.getName(), value);
                }
            } catch (ReflectiveOperationException ignored) {
                // Annotation metadata is best effort for the first Scala adapter proof of concept.
            }
        }
        return Map.copyOf(values);
    }

    private boolean annotationValueEquals(@Nullable Object left, @Nullable Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        Class<?> leftType = left.getClass();
        Class<?> rightType = right.getClass();
        if (!leftType.isArray() || !rightType.isArray()) {
            return left.equals(right);
        }
        int length = Array.getLength(left);
        if (length != Array.getLength(right)) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (!annotationValueEquals(Array.get(left, i), Array.get(right, i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidDefaultValue(@Nullable Object defaultValue) {
        if (defaultValue == null) {
            return false;
        }
        return !(defaultValue instanceof String string) || !string.isEmpty();
    }

    private AnnotationTypeElement annotationType(ScalaAnnotationData annotation) {
        ScalaAnnotationTypeData nativeType = annotation.annotationType();
        if (nativeType != null) {
            registerAnnotationType(nativeType);
        } else {
            nativeType = nativeAnnotationTypes.get(annotation.name());
        }
        return new AnnotationTypeElement(annotation.name(), nativeType == null ? loadClass(annotation.name()) : null, nativeType);
    }

    private AnnotationTypeElement annotationType(String annotationName) {
        ScalaAnnotationTypeData nativeType = nativeAnnotationTypes.get(annotationName);
        return new AnnotationTypeElement(annotationName, nativeType == null ? loadClass(annotationName) : null, nativeType);
    }

    private AnnotationTypeElement annotationType(String annotationName, @Nullable Object annotationType) {
        if (annotationType instanceof AnnotationTypeElement annotationTypeElement) {
            return annotationTypeElement;
        }
        return annotationType(annotationName);
    }

    private void registerAnnotationTypes(List<ScalaAnnotationData> annotations) {
        for (ScalaAnnotationData annotation : annotations) {
            registerAnnotationType(annotation.annotationType());
        }
    }

    private void registerAnnotationType(@Nullable ScalaAnnotationTypeData annotationType) {
        if (annotationType == null || nativeAnnotationTypes.putIfAbsent(annotationType.name(), annotationType) != null) {
            return;
        }
        registerAnnotationTypes(annotationType.annotations());
        for (ScalaAnnotationMemberData member : annotationType.members().values()) {
            registerAnnotationTypes(member.annotations());
        }
    }

    private boolean containsMember(Map<Object, Object> values, String memberName) {
        for (Object member : values.keySet()) {
            if (member instanceof AnnotationMemberElement memberElement && memberElement.name().equals(memberName)) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Method findMember(@Nullable Class<?> annotationClass, String memberName) {
        if (annotationClass == null) {
            return null;
        }
        try {
            return annotationClass.getDeclaredMethod(memberName);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private String annotationTypeName(Object annotationType) {
        if (annotationType instanceof AnnotationTypeElement annotationTypeElement) {
            return annotationTypeElement.name();
        }
        if (annotationType instanceof Class<?> type) {
            return type.getName();
        }
        if (annotationType instanceof String name) {
            return name;
        }
        return String.valueOf(annotationType);
    }

    @Nullable
    private Class<?> loadClass(String name) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Nullable
    private String repeatableContainerName(AnnotationTypeElement annotationType) {
        ScalaAnnotationTypeData nativeType = annotationType.nativeType();
        if (nativeType != null && nativeType.repeatableContainerName() != null) {
            return nativeType.repeatableContainerName();
        }
        Class<?> annotationClass = annotationType.annotationClass();
        if (annotationClass == null) {
            return null;
        }
        Repeatable repeatable = annotationClass.getAnnotation(Repeatable.class);
        return repeatable == null ? null : repeatable.value().getName();
    }

    private record AnnotationTypeElement(String name, @Nullable Class<?> annotationClass, @Nullable ScalaAnnotationTypeData nativeType) {
    }

    private record AnnotationMemberElement(
        AnnotationTypeElement annotationType,
        @Nullable ScalaAnnotationMemberData nativeMember,
        @Nullable Method method) {

        String name() {
            return nativeMember == null ? Objects.requireNonNull(method).getName() : nativeMember.name();
        }
    }

    private record UnresolvedAnnotationMember(String name) {
    }
}
