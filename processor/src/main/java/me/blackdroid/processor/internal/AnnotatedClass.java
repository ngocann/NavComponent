package me.blackdroid.processor.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.ExtraParcel;
import me.blackdroid.processor.ModifierUtils;

public class AnnotatedClass {
    String packageName;
    private List<AnnotatedField> requiredExtraList = new ArrayList<>();
    private Element annotatedElement;
    TypeName typeNameClass;
    private String classSimpleName;
    private ProcessingEnvironment processingEnvironment;
    private Map<String, ExecutableElement> setterMethods = new HashMap<>();
    private Messager messager;

    public AnnotatedClass(Element annotatedElement, ProcessingEnvironment processingEnvironment) {
        this.messager = processingEnvironment.getMessager();
        this.annotatedElement = annotatedElement;
        this.packageName = getPackageName(annotatedElement);
        typeNameClass = TypeName.get(annotatedElement.asType());
        classSimpleName = annotatedElement.getSimpleName().toString();
        this.processingEnvironment = processingEnvironment;
        for (Element element : annotatedElement.getEnclosedElements()) {
            checkAndAddSetterMethod(element);
        }
        try {
            getExtraAnnotatedFields(annotatedElement);
        } catch (ProcessingException e) {
            e.printStackTrace();
        }
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.WARNING,
                String.format(msg, args),
                e);
    }

    public void checkAndAddSetterMethod(Element classMember) {

        if (classMember.getKind() == ElementKind.METHOD) {
            ExecutableElement methodElement = (ExecutableElement) classMember;
            String methodName = methodElement.getSimpleName().toString();
            if (methodName.startsWith("set")) {
                ExecutableElement existingSetter = setterMethods.get(methodName);
                if (existingSetter != null) {
                    // Check for better visibility
                    if (ModifierUtils.compareModifierVisibility(methodElement, existingSetter) == -1) {
                        // this method has better visibility so use this one
                        setterMethods.put(methodName, methodElement);
                    }
                } else {
                    setterMethods.put(methodName, methodElement);
                }
            }
        }

    }

    public String getPackageName() {
        return packageName;
    }


    public TypeSpec getTypeSpec2() {
        final String name = String.format("%sNavComponent", annotatedElement.getSimpleName());
        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        //Constructor
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        for (AnnotatedField e : requiredExtraList) {
            builder.addField(e.getTypeName(), e.getName(), Modifier.PRIVATE);
            constructor.addParameter(e.getTypeName(), e.getName());
            constructor.addStatement("this.$N = $N", e.getName(), e.getName());
        }
        builder.addMethod(constructor.build());

        //Constructor2
        MethodSpec.Builder constructorInject = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        constructorInject.addParameter(TypeName.get(annotatedElement.asType()), "activity")
                .addStatement("inject(activity)");
        builder.addMethod(constructorInject.build());

        //Inject Method
        MethodSpec.Builder injectMethod = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(TypeName.get(annotatedElement.asType()), "activity")
                .addStatement("$T intent = activity.getIntent()", Intent.class)
                .addStatement("$T extras = intent.getExtras()", Bundle.class);

        for (AnnotatedField field : requiredExtraList) {
            Set<Modifier> modifiers = field.getElement().getModifiers();
            String setterMethod = null;
            // Private fields and non-public fields from a different package need a setter method
            boolean useSetter = modifiers.contains(Modifier.PRIVATE)
                    || (!packageName.equals(getPackageName(field.getElement())) && !modifiers.contains(Modifier.PUBLIC));

            if (useSetter) {
                ExecutableElement setterMethodElement = findSetterForField(field);
                setterMethod = setterMethodElement.getSimpleName().toString();
                if (field instanceof ExtraAnnotatedField) {
                    injectMethod.beginControlFlow("if (extras.containsKey($S))", field.getKey())
                            .addStatement("activity.$N(($T) extras.get($S))", setterMethod, field.getTypeName(), field.getKey())
                            .nextControlFlow("else")
                            .addStatement("activity.$N(null)", setterMethod)
                            .endControlFlow();
                } else {
                    injectMethod.beginControlFlow("if (extras.containsKey($S))", field.getKey())
                            .addStatement("activity.$N(($T)$T.unwrap(extras.getParcelable($S)))", setterMethod, field.getTypeName(), Parcels.class, field.getKey())
                            .nextControlFlow("else")
                            .addStatement("activity.$N(null)", setterMethod)
                            .endControlFlow();
                }
            } else {
                if (field instanceof ExtraAnnotatedField) {
                    injectMethod.beginControlFlow("if (extras.containsKey($S))", field.getKey())
                            .addStatement("activity.$N = ($T) extras.get($S)", field.getName(), field.getElement().asType(), field.getKey())
                            .nextControlFlow("else")
                            .addStatement("activity.$N = null", field.getName())
                            .endControlFlow();
                } else {
                    injectMethod.beginControlFlow("if (extras.containsKey($S))", field.getKey())
                            .addStatement("activity.$N = $T.unwrap(extras.getParcelable($S))", field.getName(), Parcels.class, field.getKey())
                            .nextControlFlow("else")
                            .addStatement("activity.$N = null", field.getName())
                            .endControlFlow();

                }
            }
        }
        builder.addMethod(injectMethod.build());

        //Start
        MethodSpec.Builder startMethod = MethodSpec.methodBuilder("start")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Context.class, "context")
                .addStatement("$T intent = new Intent(context, $T.class)", Intent.class, typeNameClass);
        for (AnnotatedField field : requiredExtraList) {
            startMethod.addParameter(field.getTypeName(), field.getName());
            if (field instanceof ExtraAnnotatedField) {
                startMethod.addStatement("intent.putExtra($S, $N)", field.getKey(), field.getName());
            } else if (field instanceof ExtraParcelAnnotatedField) {
                startMethod.addStatement("intent.putExtra($S, $T.wrap($N))", field.getKey(), Parcels.class, field.getName());
            }
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
        return ((PackageElement) e).getQualifiedName().toString();
    }


    private void getExtraAnnotatedFields(Element annotatedElement) throws ProcessingException {
        for (Element e : annotatedElement.getEnclosedElements()) {
            if (e.getAnnotation(Extra.class) != null) {
                requiredExtraList.add(new ExtraAnnotatedField(e, e.getAnnotation(Extra.class)));
            } else if (e.getAnnotation(ExtraParcel.class) != null) {
                requiredExtraList.add(new ExtraParcelAnnotatedField(e, e.getAnnotation(ExtraParcel.class)));
            }
        }
    }

    public ExecutableElement findSetterForField(AnnotatedField field) {

        String fieldName = field.getVariableName();
        StringBuilder builder = new StringBuilder("set");
        if (fieldName.length() == 1) {
            builder.append(fieldName.toUpperCase());
        } else {
            builder.append(Character.toUpperCase(fieldName.charAt(0)));
            builder.append(fieldName.substring(1));
        }

        String methodName = builder.toString();
        ExecutableElement setterMethod = setterMethods.get(methodName);
        if (setterMethod != null && isSetterApplicable(field, setterMethod)) {
            return setterMethod; // setter method found
        }

        // Search for setter method with hungarian notion check
        if (field.getName().length() > 1 && field.getName().matches("m[A-Z].*")) {
            // m not in lower case
            String hungarianMethodName = "set" + field.getName();
            setterMethod = setterMethods.get(hungarianMethodName);
            if (setterMethod != null && isSetterApplicable(field, setterMethod)) {
                return setterMethod; // setter method found
            }

            // M in upper case
            hungarianMethodName = "set" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
            setterMethod = setterMethods.get(hungarianMethodName);
            if (setterMethod != null && isSetterApplicable(field, setterMethod)) {
                return setterMethod; // setter method found
            }
        }

        // Kotlin special boolean character treatment
        // Fields prefixed with "is" are not accessible through "setIsFoo" but with "setFoo"
        if (field.getName().length() > 1 && field.getName().matches("is[A-Z].*")) {
            String setterName = "set" + field.getName().substring(2);
            setterMethod = setterMethods.get(setterName);
            if (setterMethod != null && isSetterApplicable(field, setterMethod)) {
                return setterMethod; // setter method found
            }
        }
        error(field.getElement(), "aaafindSetterForField");
        return null;


    }

    private boolean isSetterApplicable(AnnotatedField field, ExecutableElement setterMethod) {

        List<? extends VariableElement> parameters = setterMethod.getParameters();
        if (parameters == null || parameters.size() != 1) {
            return false;
        }

        VariableElement parameter = parameters.get(0);
        return parameter.asType().equals(field.getElement().asType());

    }

}
