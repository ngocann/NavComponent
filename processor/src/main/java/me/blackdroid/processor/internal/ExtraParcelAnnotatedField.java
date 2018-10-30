package me.blackdroid.processor.internal;


import com.squareup.javapoet.TypeName;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import me.blackdroid.annotation.Extra;
import me.blackdroid.annotation.ExtraParcel;

public class ExtraParcelAnnotatedField extends AnnotatedField<ExtraParcel>{

    public ExtraParcelAnnotatedField(Element element, ExtraParcel extraParcel) throws ProcessingException {
        super(element, extraParcel);
    }

    @Override
    String getKeyAnnotation() {
        return this.annotation.key();
    }
}
