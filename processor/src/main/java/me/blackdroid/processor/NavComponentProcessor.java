package me.blackdroid.processor;


import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.tools.Diagnostic;

import me.blackdroid.annotation.NavComponent;
import me.blackdroid.processor.internal.AnnotatedActivity;
import me.blackdroid.processor.internal.AnnotatedClass;
import me.blackdroid.processor.internal.AnnotatedFragment;

@AutoService(Processor.class)
public class NavComponentProcessor extends AbstractProcessor{

    public static ProcessingEnvironment instance;
    private Filer filer;
    private Messager messager;
    private static final Map<String, String> ARGUMENT_TYPES =
            new HashMap<String, String>(20);
    static {
        ARGUMENT_TYPES.put("java.lang.String", "String");
        ARGUMENT_TYPES.put("int", "Int");
        ARGUMENT_TYPES.put("java.lang.Integer", "Int");
        ARGUMENT_TYPES.put("long", "Long");
        ARGUMENT_TYPES.put("java.lang.Long", "Long");
        ARGUMENT_TYPES.put("double", "Double");
        ARGUMENT_TYPES.put("java.lang.Double", "Double");
        ARGUMENT_TYPES.put("short", "Short");
        ARGUMENT_TYPES.put("java.lang.Short", "Short");
        ARGUMENT_TYPES.put("float", "Float");
        ARGUMENT_TYPES.put("java.lang.Float", "Float");
        ARGUMENT_TYPES.put("byte", "Byte");
        ARGUMENT_TYPES.put("java.lang.Byte", "Byte");
        ARGUMENT_TYPES.put("boolean", "Boolean");
        ARGUMENT_TYPES.put("java.lang.Boolean", "Boolean");
        ARGUMENT_TYPES.put("char", "Char");
        ARGUMENT_TYPES.put("java.lang.Character", "Char");
        ARGUMENT_TYPES.put("java.lang.CharSequence", "CharSequence");
        ARGUMENT_TYPES.put("android.os.Bundle", "Bundle");
        ARGUMENT_TYPES.put("android.os.Parcelable", "Parcelable");
    }

    private static List<String> fragmentClass = Arrays.asList("android.app.Fragment","android.support.v4.app.Fragment","androidx.fragment.app.Fragment");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        instance = processingEnvironment;
        this.filer = processingEnvironment.getFiler();
        this.messager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        findAnnotation(roundEnvironment);
        return true;
    }

    private boolean isClassFragment(Element element) {
        return ElementUtils.isClass(instance, element, fragmentClass);
    }
    private boolean findAnnotation(RoundEnvironment roundEnvironment){
        List<AnnotatedClass> annotatedClassList = new ArrayList<>();
        for(Element element : roundEnvironment.getElementsAnnotatedWith(NavComponent.class)){
            if(element.getKind() != ElementKind.CLASS){
                messager.printMessage(Diagnostic.Kind.ERROR, "@NavComponent should be on top of classes");
                return true;
            }
            if (isClassFragment(element)) {
                annotatedClassList.add(new AnnotatedFragment(element, instance));
            }else {
                annotatedClassList.add(new AnnotatedActivity(element, instance));
            }
        }
        for (AnnotatedClass annotatedActivity : annotatedClassList) {
            String packageName = annotatedActivity.getPackageName();
            TypeSpec typeSpec = annotatedActivity.getTypeSpec();
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


