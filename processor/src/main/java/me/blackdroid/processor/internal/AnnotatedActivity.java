package me.blackdroid.processor.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.ExtraParcel;

public class AnnotatedActivity {
    String packageName;
    private List<Element> required = new ArrayList<>();
    private List<Element> optional = new ArrayList<>();
    private List<Element> parcels = new ArrayList<>();
    private List<Element> all = new ArrayList<>();
    private Element annotatedElement;
    TypeName typeNameClass;
    private String classSimpleName;
    private ProcessingEnvironment processingEnvironment;

    public AnnotatedActivity(Element annotatedElement, ProcessingEnvironment processingEnvironment) {
        this.annotatedElement = annotatedElement;
        this.packageName = getPackageName(annotatedElement);
        typeNameClass = TypeName.get(annotatedElement.asType());
        classSimpleName = annotatedElement.getSimpleName().toString();
        this.processingEnvironment = processingEnvironment;
        getAnnotatedFields(annotatedElement, required, optional);
        all.addAll(required);
        all.addAll(optional);
    }

    public String getClassSimpleName() {
        return classSimpleName;
    }

    public String getPackageName() {
        return packageName;
    }

    public TypeName getTypeNameClass() {
        return typeNameClass;
    }

    public List<Element> getAll() {
        return all;
    }

    public TypeSpec getTypeSpec() {
        final String name = String.format("%sNavComponent", annotatedElement.getSimpleName());
        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        //Constructor
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        for (Element e : required) {
            String paramName = getParamName(e);
            builder.addField(TypeName.get(e.asType()), paramName, Modifier.PRIVATE);
            constructor.addParameter(TypeName.get(e.asType()), paramName);
            constructor.addStatement("this.$N = $N", paramName, paramName);
        }
        for (Element e : parcels) {
            String paramName = e.getSimpleName().toString();
            builder.addField(TypeName.get(e.asType()), paramName, Modifier.PRIVATE);
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
        //Constructor2
        MethodSpec.Builder constructorInject = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        constructorInject.addParameter(TypeName.get(annotatedElement.asType()), "activity")
                .addStatement("inject(activity)");
        builder.addMethod(constructorInject.build());


        //Build Method
        MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Context.class, "context")
                .addStatement("$T intent = new Intent(context, $T.class)", Intent.class, getClassName() );
        for (Element e : all) {
            String paramName = getParamName(e);
            buildMethod.addStatement("intent.putExtra($S, $N)", paramName, paramName);
        }
        for (Element e : parcels) {
            String paramName = e.getSimpleName().toString();
            buildMethod.addStatement("intent.putExtra($S, $T.wrap($N))", paramName, Parcels.class,  paramName);
        }

        buildMethod.returns(Intent.class)
                .addStatement("return intent");
        builder.addMethod(buildMethod.build());

        //Inject Method
        MethodSpec.Builder injectMethod = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(annotatedElement.asType()), "activity")
                .addStatement("$T intent = activity.getIntent()", Intent.class)
                .addStatement("$T extras = intent.getExtras()", Bundle.class);

        for (Element e : all) {
            String paramName = getParamName(e);
            injectMethod.beginControlFlow("if (extras.containsKey($S))", paramName)
                    .addStatement("activity.$N = ($T) extras.get($S)", e.getSimpleName().toString(), e.asType(), paramName)
                    .nextControlFlow("else")
                    .addStatement("activity.$N = null", e.getSimpleName().toString())
                    .endControlFlow();
        }
        for (Element e : parcels) {
            String paramName = e.getSimpleName().toString();
            injectMethod.beginControlFlow("if (extras.containsKey($S))", paramName)
                    .addStatement("activity.$N = Parcels.unwrap(extras.getParcelable($S))", e.getSimpleName().toString(), paramName)
                    .nextControlFlow("else")
                    .addStatement("activity.$N = null", e.getSimpleName().toString())
                    .endControlFlow();
        }
        builder.addMethod(injectMethod.build());

        //Getter Method
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
                .addStatement("$T intent = new Intent(context, $T.class)", Intent.class, typeNameClass);
        for (Element element : all) {
            String paramName = getParamName(element);
            startMethod.addParameter(TypeName.get(element.asType()), paramName);
            startMethod.addStatement("intent.putExtra($S, $N)",  paramName, paramName);
        }
        for (Element element : parcels) {
            String paramName = element.getSimpleName().toString();
            startMethod.addParameter(TypeName.get(element.asType()), paramName);
            startMethod.addStatement("intent.putExtra($S, $T.wrap($N))",  paramName,Parcels.class,  paramName);
        }

        startMethod.addStatement("context.startActivity(intent)");
        builder.addMethod(startMethod.build());
        return builder.build();
    }

    public TypeName getClassName() {
        return typeNameClass;
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



    private void getAnnotatedFields(Element annotatedElement, List<Element> required, List<Element> optional) {
        for (Element e : annotatedElement.getEnclosedElements()) {
            if (e.getAnnotation(Extra.class) != null ) {
                if (hasAnnotation(e, "Nullable")) {
                    optional.add(e);
                } else {
                    required.add(e);
                }
            }
            if (e.getAnnotation(ExtraParcel.class) != null) {
                parcels.add(e);
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

}
