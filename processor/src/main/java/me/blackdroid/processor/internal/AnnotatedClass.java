package me.blackdroid.processor.internal;

import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;

import me.blackdroid.annotation.Extra;
import me.blackdroid.processor.ModifierUtils;

public abstract class AnnotatedClass {
    protected String packageName;
    protected List<AnnotatedField> requiredExtraList = new ArrayList<>();
    protected Element annotatedElement;
    protected TypeName typeNameClass;
    protected String classSimpleName;
    protected ProcessingEnvironment processingEnvironment;
    protected Map<String, ExecutableElement> setterMethods = new HashMap<>();
    protected Messager messager;

    public abstract TypeSpec getTypeSpec();

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

    protected void error(Element e, String msg, Object... args) {
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

    public TypeName getClassName() {
        return typeNameClass;
    }

    protected String getPackageName(Element e) {
        while (!(e instanceof PackageElement)) {
            e = e.getEnclosingElement();
        }
        return ((PackageElement) e).getQualifiedName().toString();
    }


    protected void getExtraAnnotatedFields(Element annotatedElement) throws ProcessingException {
        for (Element e : annotatedElement.getEnclosedElements()) {
            if (e.getAnnotation(Extra.class) != null) {
                requiredExtraList.add(new ExtraAnnotatedField(e, e.getAnnotation(Extra.class)));
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

    protected boolean isSetterApplicable(AnnotatedField field, ExecutableElement setterMethod) {

        List<? extends VariableElement> parameters = setterMethod.getParameters();
        if (parameters == null || parameters.size() != 1) {
            return false;
        }

        VariableElement parameter = parameters.get(0);
        return parameter.asType().toString().equals(field.getElement().asType().toString());

    }

}
