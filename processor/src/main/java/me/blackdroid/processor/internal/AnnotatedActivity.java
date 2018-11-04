package me.blackdroid.processor.internal;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.parceler.Parcels;

import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;

public class AnnotatedActivity  extends AnnotatedClass{

    public AnnotatedActivity(Element annotatedElement, ProcessingEnvironment processingEnvironment) {
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
                if (field instanceof ExtraAnnotatedField && !((ExtraAnnotatedField) field).isParceler()) {
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
                if (field instanceof ExtraAnnotatedField && !((ExtraAnnotatedField) field).isParceler()) {
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
            if (field instanceof ExtraAnnotatedField && !((ExtraAnnotatedField) field).isParceler()) {
                startMethod.addStatement("intent.putExtra($S, $N)", field.getKey(), field.getName());
            } else {
                startMethod.addStatement("intent.putExtra($S, $T.wrap($N))", field.getKey(), Parcels.class, field.getName());
            }
        }
        startMethod.addStatement("context.startActivity(intent)");
        builder.addMethod(startMethod.build());
        return builder.build();
    }


}
