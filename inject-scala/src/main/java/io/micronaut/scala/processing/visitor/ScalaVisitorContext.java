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

import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.value.MutableConvertibleValues;
import io.micronaut.core.convert.value.MutableConvertibleValuesMap;
import io.micronaut.expressions.context.DefaultExpressionCompilationContextFactory;
import io.micronaut.expressions.context.ExpressionCompilationContextFactory;
import io.micronaut.inject.annotation.AbstractAnnotationMetadataBuilder;
import io.micronaut.inject.annotation.MutableAnnotationMetadata;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.annotation.ElementAnnotationMetadataFactory;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.DirectoryClassWriterOutputVisitor;
import io.micronaut.inject.writer.GeneratedFile;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Visitor context for Scala compiler plugin processing.
 */
public final class ScalaVisitorContext implements VisitorContext {

    private final MutableConvertibleValues<Object> attributes = new MutableConvertibleValuesMap<>();
    private final File outputDirectory;
    private final DirectoryClassWriterOutputVisitor outputVisitor;
    private final ScalaElementFactory elementFactory = new ScalaElementFactory(this);
    private final ScalaElementAnnotationMetadataFactory annotationMetadataFactory = new ScalaElementAnnotationMetadataFactory();
    private final ScalaAnnotationMetadataBuilder annotationMetadataBuilder;
    private final ExpressionCompilationContextFactory expressionCompilationContextFactory = new DefaultExpressionCompilationContextFactory(this);
    private final Map<String, ScalaClassData> sourceClasses = new LinkedHashMap<>();
    private final Map<String, ScalaClassElement> sourceElements = new LinkedHashMap<>();
    private final IdentityHashMap<Object, MutableAnnotationMetadata> elementAnnotationMetadata = new IdentityHashMap<>();
    private final Map<String, String> options;
    private final Consumer<String> infoReporter;
    private final Consumer<String> warningReporter;
    private final Consumer<String> errorReporter;
    private final ClassLoader classLoader;

    public ScalaVisitorContext(
        File outputDirectory,
        Collection<ScalaClassData> sourceClasses,
        Collection<File> classpath,
        Map<String, String> options,
        Consumer<String> infoReporter,
        Consumer<String> warningReporter,
        Consumer<String> errorReporter) {
        this.outputDirectory = outputDirectory;
        this.outputVisitor = new DirectoryClassWriterOutputVisitor(outputDirectory);
        this.options = options == null ? Collections.emptyMap() : Map.copyOf(options);
        this.infoReporter = infoReporter;
        this.warningReporter = warningReporter;
        this.errorReporter = errorReporter;
        this.classLoader = createClassLoader(classpath);
        this.annotationMetadataBuilder = new ScalaAnnotationMetadataBuilder(this);
        for (ScalaClassData sourceClass : sourceClasses) {
            this.sourceClasses.put(sourceClass.name(), sourceClass);
        }
    }

    private ClassLoader createClassLoader(Collection<File> classpath) {
        try {
            List<File> files = new ArrayList<>(classpath.size() + 1);
            files.add(outputDirectory);
            files.addAll(classpath);
            URL[] urls = files.stream()
                .map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toArray(URL[]::new);
            return new URLClassLoader(urls, getClass().getClassLoader());
        } catch (RuntimeException e) {
            return getClass().getClassLoader();
        }
    }

    Optional<ScalaClassElement> sourceClassElement(String name) {
        ScalaClassData classData = sourceClasses.get(name);
        if (classData == null) {
            return Optional.empty();
        }
        return Optional.of(sourceElements.computeIfAbsent(name, ignored -> elementFactory.newClassElementForData(classData)));
    }

    List<ScalaClassElement> sourceClassElementsEnclosedBy(String name) {
        return sourceClasses.values().stream()
            .filter(classData -> name.equals(classData.enclosingTypeName()))
            .map(classData -> sourceClassElement(classData.name()).orElseThrow())
            .toList();
    }

    @Override
    public Language getLanguage() {
        return Language.SCALA;
    }

    @Override
    public ScalaElementFactory getElementFactory() {
        return elementFactory;
    }

    @Override
    public ElementAnnotationMetadataFactory getElementAnnotationMetadataFactory() {
        return annotationMetadataFactory;
    }

    @Override
    public ExpressionCompilationContextFactory getExpressionCompilationContextFactory() {
        return expressionCompilationContextFactory;
    }

    @Override
    public AbstractAnnotationMetadataBuilder<?, ?> getAnnotationMetadataBuilder() {
        return annotationMetadataBuilder;
    }

    public ScalaAnnotationMetadataBuilder getScalaAnnotationMetadataBuilder() {
        return annotationMetadataBuilder;
    }

    MutableAnnotationMetadata annotationMetadata(ScalaAnnotatedElementData element) {
        return elementAnnotationMetadata.computeIfAbsent(
            annotationMetadataKey(element),
            ignored -> annotationMetadataBuilder.buildMetadata(element)
        );
    }

    private Object annotationMetadataKey(ScalaAnnotatedElementData element) {
        Object nativeType = element.nativeType();
        return nativeType == null ? element : nativeType;
    }

    ClassLoader getProcessingClassLoader() {
        return classLoader;
    }

    @Override
    public Optional<ClassElement> getClassElement(String name) {
        Optional<ScalaClassElement> sourceElement = sourceClassElement(name);
        if (sourceElement.isPresent()) {
            return Optional.of(sourceElement.get());
        }
        try {
            return Optional.of(ClassElement.of(Class.forName(name, false, classLoader)));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ClassElement> getClassElement(String name, ElementAnnotationMetadataFactory annotationMetadataFactory) {
        return getClassElement(name);
    }

    @Override
    public ClassElement[] getClassElements(String aPackage, String... stereotypes) {
        return sourceClasses.values().stream()
            .filter(classData -> io.micronaut.core.naming.NameUtils.getPackageName(classData.name()).equals(aPackage))
            .map(classData -> sourceClassElement(classData.name()).orElseThrow())
            .filter(classElement -> stereotypes.length == 0 || java.util.Arrays.stream(stereotypes).anyMatch(classElement::hasStereotype))
            .toArray(ClassElement[]::new);
    }

    @Override
    public Map<String, String> getOptions() {
        return options;
    }

    @Override
    public void info(String message, @Nullable Element element) {
        infoReporter.accept(message);
    }

    @Override
    public void info(String message) {
        infoReporter.accept(message);
    }

    @Override
    public void fail(String message, @Nullable Element element) {
        errorReporter.accept(message);
        throw new ProcessingException(element, message);
    }

    @Override
    public void warn(String message, @Nullable Element element) {
        warningReporter.accept(message);
    }

    @Override
    public <T> Optional<T> get(CharSequence name, ArgumentConversionContext<T> conversionContext) {
        return attributes.get(name, conversionContext);
    }

    @Override
    public Set<String> names() {
        return attributes.names();
    }

    @Override
    public Collection<Object> values() {
        return attributes.values();
    }

    @Override
    public MutableConvertibleValues<Object> put(CharSequence key, @Nullable Object value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public MutableConvertibleValues<Object> remove(CharSequence key) {
        attributes.remove(key);
        return this;
    }

    @Override
    public MutableConvertibleValues<Object> clear() {
        attributes.clear();
        return this;
    }

    @Override
    public OutputStream visitClass(String classname, Element... originatingElements) throws IOException {
        return outputVisitor.visitClass(classname, originatingElements);
    }

    @Override
    public void visitServiceDescriptor(String type, String classname) {
        outputVisitor.visitServiceDescriptor(type, classname);
    }

    @Override
    public void visitServiceDescriptor(String type, String classname, Element originatingElement) {
        outputVisitor.visitServiceDescriptor(type, classname, originatingElement);
    }

    @Override
    public Optional<GeneratedFile> visitMetaInfFile(String path, Element... originatingElements) {
        return outputVisitor.visitMetaInfFile(path, originatingElements);
    }

    @Override
    public Optional<GeneratedFile> visitGeneratedFile(String path) {
        return outputVisitor.visitGeneratedFile(path);
    }

    @Override
    public Optional<GeneratedFile> visitGeneratedFile(String path, Element... originatingElements) {
        return outputVisitor.visitGeneratedFile(path, originatingElements);
    }

    @Override
    public void finish() {
        outputVisitor.finish();
    }

    @Override
    public Map<String, Set<String>> getServiceEntries() {
        return outputVisitor.getServiceEntries();
    }

    @Override
    public Optional<Path> getClassesOutputPath() {
        return Optional.of(outputDirectory.toPath());
    }
}
