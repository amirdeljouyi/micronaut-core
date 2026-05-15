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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Requires.Sdk;
import io.micronaut.core.annotation.Generated;
import io.micronaut.core.annotation.Vetoed;
import io.micronaut.core.io.service.ServiceDefinition;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.core.util.StringUtils;
import io.micronaut.core.version.VersionUtils;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ConstructorElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MemberElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.processing.BeanDefinitionCreator;
import io.micronaut.inject.processing.BeanDefinitionCreatorFactory;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.BeanElementVisitor;
import io.micronaut.inject.visitor.TypeElementQuery;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.BeanDefinitionVisitor;
import io.micronaut.inject.writer.BeanDefinitionWriter;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Executes Micronaut's visitor and bean-definition pipeline for Scala compiler data.
 */
public final class ScalaProcessingEngine {

    private final File outputDirectory;
    private final Collection<File> classpath;
    private final Map<String, String> options;
    private final Consumer<String> infoReporter;
    private final Consumer<String> warningReporter;
    private final Consumer<String> errorReporter;
    private final Map<String, ScalaClassData> sourceClasses = new LinkedHashMap<>();
    private final Set<String> generatedBeanDefinitions = new HashSet<>();
    private final Set<String> visitedTypes = new HashSet<>();
    private boolean typeVisitorsProcessed;
    private boolean beanDefinitionsProcessed;
    private @Nullable ScalaVisitorContext visitorContext;

    /**
     * @param outputDirectory The compiler class output directory
     * @param classpath The compilation classpath
     * @param options Micronaut processing options
     * @param infoReporter The info reporter
     * @param warningReporter The warning reporter
     * @param errorReporter The error reporter
     */
    public ScalaProcessingEngine(
        File outputDirectory,
        Collection<File> classpath,
        Map<String, String> options,
        Consumer<String> infoReporter,
        Consumer<String> warningReporter,
        Consumer<String> errorReporter) {
        this.outputDirectory = outputDirectory;
        this.classpath = List.copyOf(classpath);
        this.options = Map.copyOf(options);
        this.infoReporter = infoReporter;
        this.warningReporter = warningReporter;
        this.errorReporter = errorReporter;
    }

    /**
     * Adds classes observed by a Scala compiler phase.
     *
     * @param classData The classes
     */
    public void addClasses(Collection<ScalaClassData> classData) {
        for (ScalaClassData sourceClass : classData) {
            sourceClasses.putIfAbsent(sourceClass.name(), sourceClass);
        }
    }

    /**
     * Processes type element visitors once all source classes have been collected.
     */
    public void processTypeVisitors() {
        if (typeVisitorsProcessed) {
            return;
        }
        typeVisitorsProcessed = true;
        ScalaVisitorContext context = visitorContext();
        setMicronautOptionsAsSystemProperties();
        List<LoadedScalaVisitor> loadedVisitors = loadTypeElementVisitors(context);
        for (LoadedScalaVisitor loadedVisitor : loadedVisitors) {
            try {
                loadedVisitor.getVisitor().start(context);
            } catch (Throwable e) {
                context.warn("Error initializing type visitor [" + loadedVisitor.getVisitor() + "]: " + e.getMessage(), null);
            }
        }
        for (LoadedScalaVisitor loadedVisitor : loadedVisitors) {
            TypeElementQuery query = loadedVisitor.getVisitor().query();
            Set<String> visitedForVisitor = new HashSet<>();
            for (ScalaClassData classData : new LinkedHashSet<>(sourceClasses.values())) {
                if (!visitedForVisitor.add(classData.name())) {
                    continue;
                }
                ClassElement classElement = context.sourceClassElement(classData.name()).orElseThrow();
                if (!loadedVisitor.matchesClass(classElement.getAnnotationMetadata())) {
                    continue;
                }
                visitClass(loadedVisitor, classElement, query, context);
            }
        }
        for (LoadedScalaVisitor loadedVisitor : loadedVisitors) {
            try {
                loadedVisitor.getVisitor().finish(context);
            } catch (ProcessingException e) {
                reportProcessingException(e);
            } catch (Throwable e) {
                context.fail("Error finalizing type visitor [" + loadedVisitor.getVisitor() + "]: " + e.getMessage(), null);
            }
        }
    }

    /**
     * Generates bean definitions for collected source classes.
     */
    public void processBeanDefinitions() {
        if (beanDefinitionsProcessed) {
            return;
        }
        beanDefinitionsProcessed = true;
        ScalaVisitorContext context = visitorContext();
        startBeanElementVisitors(context);
        for (ScalaClassData classData : new LinkedHashSet<>(sourceClasses.values())) {
            if (classData.interfaceType() || classData.name().endsWith(BeanDefinitionVisitor.PROXY_SUFFIX)) {
                continue;
            }
            ClassElement classElement = context.sourceClassElement(classData.name()).orElseThrow();
            if (classElement.hasAnnotation(Vetoed.class) || classElement.hasAnnotation(Generated.class)) {
                continue;
            }
            try {
                BeanDefinitionCreator beanDefinitionCreator = BeanDefinitionCreatorFactory.produce(classElement, context);
                for (BeanDefinitionVisitor writer : beanDefinitionCreator.build()) {
                    if (generatedBeanDefinitions.add(writer.getBeanDefinitionName()) && writer.isEnabled()) {
                        writer.visitBeanDefinitionEnd();
                        writer.accept(context);
                    }
                }
            } catch (ProcessingException e) {
                reportProcessingException(e);
            } catch (IOException e) {
                context.fail("Unexpected error writing bean definition: " + e.getMessage(), classElement);
            }
        }
        finishBeanElementVisitors(context);
        context.finish();
        BeanDefinitionWriter.finish();
    }

    private ScalaVisitorContext visitorContext() {
        ScalaVisitorContext context = visitorContext;
        if (context == null) {
            context = new ScalaVisitorContext(
                outputDirectory,
                sourceClasses.values(),
                classpath,
                options,
                infoReporter,
                warningReporter,
                errorReporter
            );
            visitorContext = context;
        }
        return context;
    }

    private void visitClass(
        LoadedScalaVisitor loadedVisitor,
        ClassElement classElement,
        TypeElementQuery query,
        ScalaVisitorContext context) {
        if (!visitedTypes.add(loadedVisitor.getVisitor().getClass().getName() + ':' + classElement.getName())) {
            return;
        }
        try {
            loadedVisitor.getVisitor().visitClass(classElement, context);
        } catch (Exception e) {
            throw new ProcessingException(classElement, e.getMessage(), e);
        }
        if (query.includesConstructors()) {
            for (ConstructorElement constructorElement : classElement.getEnclosedElements(ElementQuery.CONSTRUCTORS)) {
                if (loadedVisitor.matchesElement(constructorElement.getAnnotationMetadata())) {
                    try {
                        loadedVisitor.getVisitor().visitConstructor(constructorElement, context);
                    } catch (Exception e) {
                        throw new ProcessingException(constructorElement, e.getMessage(), e);
                    }
                }
            }
        }
        if (query.includesFields() || query.includesMethods()) {
            ElementQuery<? extends MemberElement> memberQuery;
            if (query.includesFields() && query.includesMethods()) {
                memberQuery = ElementQuery.ALL_FIELD_AND_METHODS;
            } else if (query.includesFields()) {
                memberQuery = ElementQuery.ALL_FIELDS;
            } else {
                memberQuery = ElementQuery.ALL_METHODS;
            }
            for (MemberElement memberElement : classElement.getEnclosedElements(memberQuery)) {
                if (!loadedVisitor.matchesElement(memberElement.getAnnotationMetadata())) {
                    continue;
                }
                try {
                    if (memberElement instanceof FieldElement fieldElement) {
                        loadedVisitor.getVisitor().visitField(fieldElement, context);
                    } else if (memberElement instanceof MethodElement methodElement) {
                        loadedVisitor.getVisitor().visitMethod(methodElement, context);
                    }
                } catch (Exception e) {
                    throw new ProcessingException(memberElement, e.getMessage(), e);
                }
            }
        }
    }

    private void reportProcessingException(ProcessingException exception) {
        if (exception.getMessage() == null) {
            errorReporter.accept("Unknown error processing Scala element");
        } else {
            errorReporter.accept(exception.getMessage());
        }
    }

    private List<LoadedScalaVisitor> loadTypeElementVisitors(ScalaVisitorContext context) {
        Map<String, TypeElementVisitor<?, ?>> visitors = new LinkedHashMap<>(10);
        for (ServiceDefinition<TypeElementVisitor> definition : SoftServiceLoader.load(TypeElementVisitor.class, context.getProcessingClassLoader())) {
            if (!definition.isPresent()) {
                continue;
            }
            TypeElementVisitor<?, ?> visitor;
            try {
                visitor = definition.load();
            } catch (Throwable e) {
                warningReporter.accept("TypeElementVisitor [" + definition.getName() + "] will be ignored due to loading error: " + e.getMessage());
                continue;
            }
            if (visitor == null || !visitor.isEnabled() || !meetsRequires(visitor)) {
                continue;
            }
            visitors.put(definition.getName(), visitor);
        }
        List<LoadedScalaVisitor> loadedVisitors = new ArrayList<>(visitors.size());
        for (TypeElementVisitor<?, ?> visitor : visitors.values()) {
            try {
                loadedVisitors.add(new LoadedScalaVisitor(visitor));
            } catch (TypeNotPresentException | NoClassDefFoundError ignored) {
                // The visitor references annotations that are not available on this compilation classpath.
            }
        }
        OrderUtil.reverseSort(loadedVisitors);
        return loadedVisitors;
    }

    private boolean meetsRequires(TypeElementVisitor<?, ?> visitor) {
        Requires requires = visitor.getClass().getAnnotation(Requires.class);
        if (requires == null || requires.sdk() != Sdk.MICRONAUT) {
            return true;
        }
        String version = requires.version();
        if (StringUtils.isEmpty(version) || VersionUtils.isAtLeastMicronautVersion(version)) {
            return true;
        }
        warningReporter.accept("TypeElementVisitor [" + visitor.getClass().getName() + "] will be ignored because Micronaut version [" + VersionUtils.MICRONAUT_VERSION + "] must be at least " + version);
        return false;
    }

    private void setMicronautOptionsAsSystemProperties() {
        options.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(VisitorContext.MICRONAUT_BASE_OPTION_NAME))
            .forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
    }

    private void startBeanElementVisitors(ScalaVisitorContext context) {
        for (BeanElementVisitor<?> visitor : BeanElementVisitor.VISITORS) {
            if (visitor.isEnabled()) {
                try {
                    visitor.start(context);
                } catch (Exception e) {
                    context.fail("Error initializing bean element visitor [" + visitor.getClass().getName() + "]: " + e.getMessage(), null);
                }
            }
        }
    }

    private void finishBeanElementVisitors(ScalaVisitorContext context) {
        for (BeanElementVisitor<?> visitor : BeanElementVisitor.VISITORS) {
            if (visitor.isEnabled()) {
                try {
                    visitor.finish(context);
                } catch (Exception e) {
                    context.fail("Error finalizing bean element visitor [" + visitor.getClass().getName() + "]: " + e.getMessage(), null);
                }
            }
        }
    }

}
