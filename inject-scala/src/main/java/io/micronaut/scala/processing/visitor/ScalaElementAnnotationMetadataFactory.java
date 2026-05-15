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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.GenericElement;
import io.micronaut.inject.ast.annotation.ElementAnnotationMetadata;
import io.micronaut.inject.ast.annotation.ElementAnnotationMetadataFactory;
import io.micronaut.inject.annotation.MutableAnnotationMetadata;

/**
 * Element annotation metadata factory for Scala elements.
 */
public final class ScalaElementAnnotationMetadataFactory implements ElementAnnotationMetadataFactory {

    private final boolean readOnly;

    public ScalaElementAnnotationMetadataFactory() {
        this(false);
    }

    private ScalaElementAnnotationMetadataFactory(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public ElementAnnotationMetadata build(Element element) {
        return buildMutable(element.getAnnotationMetadata());
    }

    @Override
    public ElementAnnotationMetadata buildTypeAnnotations(ClassElement element) {
        return buildMutable(AnnotationMetadata.EMPTY_METADATA);
    }

    @Override
    public ElementAnnotationMetadata buildGenericTypeAnnotations(GenericElement element) {
        return buildMutable(AnnotationMetadata.EMPTY_METADATA);
    }

    @Override
    public ElementAnnotationMetadata buildMutable(AnnotationMetadata annotationMetadata) {
        MutableAnnotationMetadata mutable = MutableAnnotationMetadata.of(annotationMetadata);
        return new SimpleElementAnnotationMetadata(mutable, readOnly);
    }

    @Override
    public ElementAnnotationMetadataFactory readOnly() {
        return new ScalaElementAnnotationMetadataFactory(true);
    }
}
