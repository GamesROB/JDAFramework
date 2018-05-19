package me.deprilula28.jdacmdframework.annotations;

import me.deprilula28.jdacmdframework.CommandContext;
import net.dv8tion.jda.core.entities.User;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AnnotationConstants {
    static Map<Class<?>, String> annotationTypeMap = new HashMap<>();
    static Map<Class<?>, Method> methodAnnotationTypes = new HashMap<>();

    static {
        annotationTypeMap.put(String.class, "next");
        annotationTypeMap.put(User.class, "nextUser");
        annotationTypeMap.put(List.class, "remaining");
        annotationTypeMap.put(int.class, "nextInt");
        annotationTypeMap.put(double.class, "nextDouble");

        Class<?> contextClass = CommandContext.class;
        annotationTypeMap.forEach((key, value) -> {
            try {
                methodAnnotationTypes.put(key, contextClass.getDeclaredMethod(value));
            } catch (Exception e) {
                System.err.println("Failed to load reflection constant:");
                e.printStackTrace();
            }
        });
    }
}
