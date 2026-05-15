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
import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.annotation.MutableAnnotationMetadataDelegate;

/**
 * Scala method element.
 */
public class ScalaMethodElement extends AbstractScalaMemberElement implements MethodElement {

    protected final ScalaClassElement declaringType;
    protected final ScalaVisitorContext visitorContext;
    protected final ScalaMethodData methodData;
    private ParameterElement[] parameters;

    ScalaMethodElement(ScalaClassElement declaringType, ScalaMethodData methodData, ScalaVisitorContext visitorContext) {
        this(declaringType, methodData, visitorContext, visitorContext.getScalaAnnotationMetadataBuilder().buildMetadata(methodData));
    }

    ScalaMethodElement(ScalaClassElement declaringType, ScalaMethodData methodData, ScalaVisitorContext visitorContext, AnnotationMetadata annotationMetadata) {
        super(
            declaringType,
            methodData.name(),
            methodData.nativeType(),
            methodData.modifiers(),
            MutableAnnotationMetadata.of(annotationMetadata)
        );
        this.declaringType = declaringType;
        this.visitorContext = visitorContext;
        this.methodData = methodData;
        this.parameters = methodData.parameters().stream()
            .map(parameter -> new ScalaParameterElement(this, parameter, visitorContext))
            .toArray(ParameterElement[]::new);
    }

    @Override
    public MutableAnnotationMetadataDelegate<AnnotationMetadata> getMethodAnnotationMetadata() {
        return new SimpleElementAnnotationMetadata(
            io.micronaut.inject.annotation.MutableAnnotationMetadata.of(getAnnotationMetadata()),
            false
        );
    }

    @Override
    public ClassElement getReturnType() {
        return visitorContext.getElementFactory().newClassElement(methodData.returnType());
    }

    @Override
    public ParameterElement[] getParameters() {
        return parameters;
    }

    @Override
    public MethodElement withParameters(ParameterElement... newParameters) {
        ScalaMethodElement methodElement = new ScalaMethodElement(declaringType, methodData, visitorContext, getAnnotationMetadata());
        methodElement.parameters = newParameters;
        return methodElement;
    }

    @Override
    public MethodElement withAnnotationMetadata(AnnotationMetadata annotationMetadata) {
        return new ScalaMethodElement(declaringType, methodData, visitorContext, annotationMetadata);
    }

    @Override
    public boolean overrides(MethodElement overridden) {
        return false;
    }

    @Override
    public boolean hides(MethodElement hiddenMethod) {
        return false;
    }
}
