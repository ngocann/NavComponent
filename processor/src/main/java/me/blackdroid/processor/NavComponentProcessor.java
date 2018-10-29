package me.blackdroid.processor;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import me.blackdroid.annotation.NavComponent;
import me.blackdroid.processor.internal.AnnotatedActivity;
import me.blackdroid.processor.internal.ProcessingException;

@AutoService(Processor.class)
public class NavComponentProcessor extends AbstractProcessor{

    public static ProcessingEnvironment instance;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        instance = processingEnvironment;
        this.filer = processingEnvironment.getFiler();
        this.messager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        try {
            findAnnotation(roundEnvironment);
        } catch (ProcessingException e) {
            e.printStackTrace();
        }
        return true;
    }

    private boolean findAnnotation(RoundEnvironment roundEnvironment) throws ProcessingException {
        List<AnnotatedActivity> annotatedActivityList = new ArrayList<>();
        for(Element element : roundEnvironment.getElementsAnnotatedWith(NavComponent.class)){
            if(element.getKind() != ElementKind.CLASS){
                messager.printMessage(Diagnostic.Kind.ERROR, "@NavComponent should be on top of classes");
                return true;
            }
            AnnotatedActivity annotatedActivity = new AnnotatedActivity(element, instance);
            annotatedActivityList.add(annotatedActivity);
        }

        for (AnnotatedActivity annotatedActivity : annotatedActivityList) {
            String packageName = annotatedActivity.getPackageName();
            TypeSpec typeSpec = annotatedActivity.getTypeSpec2();
            createFile(packageName, typeSpec);
        }
        return false;
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
