package me.blackdroid.processor.internal;

import android.content.Context;
import android.content.Intent;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;

import me.blackdroid.annotation.Extra;

public class AnnotatedNavComponent {
    private List<AnnotatedActivity> annotatedActivityList;
    private String packageName = "me.blackdroid.navcomponent";

    public AnnotatedNavComponent(List<AnnotatedActivity> annotatedActivityList) {
        this.annotatedActivityList = annotatedActivityList;
    }

    public String getPackageName() {
        return packageName;
    }

    public TypeSpec getTypeSpec() {
        TypeSpec.Builder builder = TypeSpec.classBuilder("NavComponents")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        for (AnnotatedActivity annotatedActivity : annotatedActivityList) {
            String nameMethod = String.format("start%s",annotatedActivity.getClassSimpleName() );
            MethodSpec.Builder startMethod = MethodSpec.methodBuilder(nameMethod)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(Context.class, "context")
                    .addStatement("$T intent = new Intent(context, $T.class)", Intent.class, annotatedActivity.typeNameClass);
            for (Element element : annotatedActivity.getAll()) {
                String paramName = getParamName(element);
                startMethod.addParameter(TypeName.get(element.asType()), paramName);
                startMethod.addStatement("intent.putExtra($S, $N)",  paramName, paramName);
            }
            startMethod.addStatement("context.startActivity(intent)");
            builder.addMethod(startMethod.build());
        }
        return builder.build();
    }

    private String getParamName(Element e) {
        String extraValue = e.getAnnotation(Extra.class).value();
        return extraValue !=null && !extraValue.trim().isEmpty() ? extraValue : e.getSimpleName().toString();
    }
}
