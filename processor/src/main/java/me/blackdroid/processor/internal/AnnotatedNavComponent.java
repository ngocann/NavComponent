package me.blackdroid.processor.internal;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;

public class AnnotatedNavComponent {
    String packageName;
    List<Element> required = new ArrayList<>();
    List<Element> optional = new ArrayList<>();
    List<Element> all = new ArrayList<>();

    public AnnotatedNavComponent(Element annotatedElement) {

        getAnnotatedFields(annotatedElement, required, optional);
        all.addAll(required);
        all.addAll(optional);
    }


}
