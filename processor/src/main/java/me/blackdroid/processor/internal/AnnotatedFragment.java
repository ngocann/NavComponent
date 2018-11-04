package me.blackdroid.processor.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.parceler.Parcels;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

public class AnnotatedFragment extends AnnotatedClass{

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

    public AnnotatedFragment(Element annotatedElement, ProcessingEnvironment processingEnvironment) {
        super(annotatedElement, processingEnvironment);
    }


    @Override
    public TypeSpec getTypeSpec() {
        final String name = String.format("%sNavComponent", annotatedElement.getSimpleName());
        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        //Inject Method
        MethodSpec.Builder injectMethod = MethodSpec.methodBuilder("inject")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(TypeName.get(annotatedElement.asType()), "fragment")
                .addStatement("$T bundle = fragment.getArguments()", Bundle.class);

        for (AnnotatedField field : requiredExtraList) {
            String op = getOperation(field);
            Set<Modifier> modifiers = field.getElement().getModifiers();
            String setterMethod = null;
            // Private fields and non-public fields from a different package need a setter method
            boolean useSetter = modifiers.contains(Modifier.PRIVATE)
                    || (!packageName.equals(getPackageName(field.getElement())) && !modifiers.contains(Modifier.PUBLIC));
            if (useSetter) {
                ExecutableElement setterMethodElement = findSetterForField(field);
                setterMethod = setterMethodElement.getSimpleName().toString();
                if (field instanceof ExtraAnnotatedField) {
                    injectMethod.beginControlFlow("if (bundle.containsKey($S))", field.getKey())
                            .addStatement("fragment.$N(($T) bundle.get$N($S))", setterMethod, field.getTypeName(), op,  field.getKey())
                            .nextControlFlow("else")
                            .addStatement("fragment.$N(null)", setterMethod)
                            .endControlFlow();
                } else {
                    injectMethod.beginControlFlow("if (bundle.containsKey($S))", field.getKey())
                            .addStatement("fragment.$N(($T)$T.unwrap(bundle.getParcelable($S)))", setterMethod, field.getTypeName(), Parcels.class, field.getKey())
                            .nextControlFlow("else")
                            .addStatement("fragment.$N(null)", setterMethod)
                            .endControlFlow();
                }
            } else {
                if (field instanceof ExtraAnnotatedField) {
                    injectMethod.beginControlFlow("if (bundle.containsKey($S))", field.getKey())
                            .addStatement("fragment.$N = ($T) bundle.get$N($S)", field.getName(), field.getElement().asType(),  op, field.getKey())
                            .nextControlFlow("else")
                            .addStatement("fragment.$N = null", field.getName())
                            .endControlFlow();
                } else {
                    injectMethod.beginControlFlow("if (bundle.containsKey($S))", field.getKey())
                            .addStatement("fragment.$N = $T.unwrap(bundle.getParcelable($S))", field.getName(), Parcels.class, field.getKey())
                            .nextControlFlow("else")
                            .addStatement("fragment.$N = null", field.getName())
                            .endControlFlow();

                }
            }
        }
        builder.addMethod(injectMethod.build());

        //Start
        MethodSpec.Builder startMethod = MethodSpec.methodBuilder("newInstance")
                .returns(typeNameClass)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("$T fragment = new $T()", typeNameClass, typeNameClass)
                .addStatement("$T bundle = new Bundle()", Bundle.class)
                ;
        for (AnnotatedField field : requiredExtraList) {
            startMethod.addParameter(field.getTypeName(), field.getName());
            String op = getOperation(field);
            startMethod.addStatement("bundle.put$N($S,$N)", op, field.getKey(), field.getName() );
        }
        startMethod.addStatement("fragment.setArguments(bundle)");
        startMethod.addStatement("return fragment");
        builder.addMethod(startMethod.build());
        return builder.build();
    }

    private String getOperation(AnnotatedField field) {
        String op = ARGUMENT_TYPES.get(field.getType());
        return op;
    }

}
