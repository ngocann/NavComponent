package me.blackdroid.processor;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.NavComponent;

@AutoService(Processor.class)
public class NavComponentProcessor extends AbstractProcessor{

    public static NavComponentProcessor instance;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        instance = this;
        this.filer = processingEnvironment.getFiler();
        this.messager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        findAnnotation(roundEnvironment);
        return true;
    }

    private boolean findAnnotation(RoundEnvironment roundEnvironment) {
        for(Element element : roundEnvironment.getElementsAnnotatedWith(NavComponent.class)){
            if(element.getKind() != ElementKind.CLASS){
                messager.printMessage(Diagnostic.Kind.ERROR, "@NavComponent should be on top of classes");
                return true;
            }
            TypeSpec typeSpec = getBuilderSpec(element);
            createFile(getPackageName(element), typeSpec);

        }
        return false;
    }

    private TypeSpec getBuilderSpec(Element annotatedElement) {
        List<Element> required = new ArrayList<>();
        List<Element> optional = new ArrayList<>();
        List<Element> all = new ArrayList<>();

        getAnnotatedFields(annotatedElement, required, optional);
        all.addAll(required);
        all.addAll(optional);

        final String name = String.format("%sNavComponent", annotatedElement.getSimpleName());
        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        //Constructor
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        for (Element e : required) {
            String paramName = getParamName(e);
            builder.addField(TypeName.get(e.asType()), paramName, Modifier.PRIVATE, Modifier.FINAL);
            constructor.addParameter(TypeName.get(e.asType()), paramName);
            constructor.addStatement("this.$N = $N", paramName, paramName);
        }
        builder.addMethod(constructor.build());

        for (Element e : optional) {
            String paramName = getParamName(e);
            builder.addField(TypeName.get(e.asType()), paramName, Modifier.PRIVATE);
            builder.addMethod(MethodSpec.methodBuilder(paramName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.get(e.asType()), paramName)
                    .addStatement("this.$N = $N", paramName, paramName)
                    .addStatement("return this")
                    .returns(ClassName.get(getPackageName(annotatedElement), name))
                    .build());
        }



        MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Context.class, "context")
                .addStatement("$T intent = new Intent(context, $T.class)", Intent.class, TypeName.get(annotatedElement.asType()));
        for (Element e : all) {
            String paramName = getParamName(e);
            buildMethod.addStatement("intent.putExtra($S, $N)", paramName, paramName);
        }
        buildMethod.returns(Intent.class)
                .addStatement("return intent");
        builder.addMethod(buildMethod.build());

        MethodSpec.Builder injectMethod = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Intent.class, "intent")
                .addParameter(TypeName.get(annotatedElement.asType()), "activity")
                .addStatement("$T extras = intent.getExtras()", Bundle.class);

        for (Element e : all) {
            String paramName = getParamName(e);
            injectMethod.beginControlFlow("if (extras.containsKey($S))", paramName)
                    .addStatement("activity.$N = ($T) extras.get($S)", e.getSimpleName().toString(), e.asType(), paramName)
                    .nextControlFlow("else")
                    .addStatement("activity.$N = null", e.getSimpleName().toString())
                    .endControlFlow();
        }
        builder.addMethod(injectMethod.build());

        for (Element e : all) {
            String paramName = e.getSimpleName().toString();
            MethodSpec.Builder getterMethod = MethodSpec
                    .methodBuilder("get" + paramName.substring(0, 1).toUpperCase() + paramName.substring(1))
                    .returns(ClassName.get(e.asType()))
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Intent.class, "intent")
                    .addStatement("$T extras = intent.getExtras()", Bundle.class)
                    .beginControlFlow("if (extras.containsKey($S))", paramName)
                    .addStatement("return ($T) extras.get($S)", e.asType(), paramName)
                    .nextControlFlow("else")
                    .addStatement("return null")
                    .endControlFlow();
            builder.addMethod(getterMethod.build());
        }

        //Start
        MethodSpec.Builder startMethod = MethodSpec.methodBuilder("start")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Context.class, "context")
                .addStatement("$T intent = new Intent(context, $T.class)", Intent.class, TypeName.get(annotatedElement.asType()));
                ;
        for (Element element : all) {
            String paramName = getParamName(element);
            startMethod.addParameter(TypeName.get(element.asType()), paramName);
            startMethod.addStatement("intent.putExtra($S, $N)",  paramName, paramName);
        }
        startMethod.addStatement("context.startActivity(intent)");
        builder.addMethod(startMethod.build());
        return builder.build();
    }

    private void getAnnotatedFields(Element annotatedElement, List<Element> required, List<Element> optional) {
        for (Element e : annotatedElement.getEnclosedElements()) {
            if (e.getAnnotation(Extra.class) != null) {
                if (hasAnnotation(e, "Nullable")) {
                    optional.add(e);
                } else {
                    required.add(e);
                }
            }
        }
    }

    private boolean hasAnnotation(Element e, String name) {
        for (AnnotationMirror annotation : e.getAnnotationMirrors()) {
            if (annotation.getAnnotationType().asElement().getSimpleName().toString().equals(name)) {
                return true;
            }
        }
        return false;
    }


    private String getPackageName(Element e) {
        while (!(e instanceof PackageElement)) {
            e = e.getEnclosingElement();
        }
        return ((PackageElement)e).getQualifiedName().toString();
    }

    private String getParamName(Element e) {
        String extraValue = e.getAnnotation(Extra.class).value();
        return extraValue !=null && !extraValue.trim().isEmpty() ? extraValue : e.getSimpleName().toString();
    }



    private void createFile(String packageName, TypeSpec typeSpec) {
        try {
            JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(NavComponent.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }


}
