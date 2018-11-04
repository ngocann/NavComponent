package me.blackdroid.processor;


import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;

public class ElementUtils {

    public static boolean isClass(ProcessingEnvironment env, Element classElement, List<String> classNameList) {
        for (String className : classNameList) {
           boolean isClass = isClass(env, classElement, className);
           if (isClass) {
               return true;
           }
        }
        return false;
    }

    public static boolean isClass(ProcessingEnvironment env, Element classElement, String className) {
        TypeElement typeElement = env.getElementUtils().getTypeElement(className);
        Types typeUtils = env.getTypeUtils();
        if (typeElement != null && typeUtils.isSubtype(classElement.asType(),
                typeElement.asType())) {
            return true;
        }

        return false;
    }
}
