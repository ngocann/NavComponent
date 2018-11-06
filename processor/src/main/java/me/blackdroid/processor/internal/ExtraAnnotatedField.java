package me.blackdroid.processor.internal;


import com.squareup.javapoet.TypeName;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import me.blackdroid.annotation.Extra;

public class ExtraAnnotatedField extends AnnotatedField<Extra>{

    public ExtraAnnotatedField(Element element, Extra extra) throws ProcessingException {
        super(element, extra);
    }

    @Override
    String getKeyAnnotation() {
        return this.annotation.key();
    }

    public boolean isParceler() {
        return this.annotation.parceler();
    }
}
