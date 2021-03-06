package me.blackdroid.processor.internal;


import com.squareup.javapoet.TypeName;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public abstract class AnnotatedField<T> {

    private final String name;
    private final String key;
    private final String type;
    private final Element element;
    protected final T annotation;


    public AnnotatedField(Element element, T t)
            throws ProcessingException {
        this.annotation = t;
        this.name = element.getSimpleName().toString();
        this.key = getKey(element);
        this.type = element.asType().toString();
        this.element = element;
    }

    abstract String getKeyAnnotation();

    private String getKey(Element element) {
        String field = element.getSimpleName().toString();
        if (!"".equals(getKeyAnnotation())) {
            return getKeyAnnotation();
        }
        return getVariableName(field);
    }

    private String getFullQualifiedNameByTypeMirror(TypeMirror baggerClass)
            throws ProcessingException {
        if (baggerClass == null) {
            throw new ProcessingException(element, "Could not get the ArgsBundler class");
        }

        if (!isPublicClass((DeclaredType) baggerClass)) {
            throw new ProcessingException(element,
                    "The %s must be a public class to be a valid ArgsBundler", baggerClass.toString());
        }

        // Check if the bagger class has a default constructor
        if (!hasPublicEmptyConstructor((DeclaredType) baggerClass)) {
            throw new ProcessingException(element,
                    "The %s must provide a public empty default constructor to be a valid ArgsBundler",
                    baggerClass.toString());
        }

        return baggerClass.toString();
    }


    /**
     * Checks if a class is public
     */
    private boolean isPublicClass(DeclaredType type) {
        Element element = type.asElement();

        return element.getModifiers().contains(javax.lang.model.element.Modifier.PUBLIC);
    }

    /**
     * Checks if an public empty constructor is available
     */
    private boolean hasPublicEmptyConstructor(DeclaredType type) {
        Element element = type.asElement();

        List<? extends Element> containing = element.getEnclosedElements();

        for (Element e : containing) {
            if (e.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement c = (ExecutableElement) e;

                if ((c.getParameters() == null || c.getParameters().isEmpty()) && c.getModifiers()
                        .contains(javax.lang.model.element.Modifier.PUBLIC)) {
                    return true;
                }
            }
        }

        return false;
    }


    public String getVariableName() {
        return getVariableName(name);
    }

    public static String getVariableName(String name) {
        if (name.matches("^m[A-Z]{1}") || name.matches("^_[A-Za-z]{1}")) {
            return name.substring(1, 2).toLowerCase();
        } else if (name.matches("m[A-Z]{1}.*") || name.matches("_[A-Za-z]{1}.*")) {
            return name.substring(1, 2).toLowerCase() + name.substring(2);
        }
        return name;
    }

    public String getKey() {
        return this.key;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Element getElement() {
        return element;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return key + "/" + type;
    }

    public String getRawType() {
        if (isArray()) {
            return type.substring(0, type.length() - 2);
        }
        return type;
    }

    public boolean isArray() {
        return type.endsWith("[]");
    }

    public boolean isPrimitive() {
        return element.asType().getKind().isPrimitive();
    }

    public TypeName getTypeName() {
        return TypeName.get(element.asType());
    }



}
