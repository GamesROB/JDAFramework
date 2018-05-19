package me.deprilula28.jdacmdframework.annotations;

import lombok.AllArgsConstructor;
import me.deprilula28.jdacmdframework.Command;
import me.deprilula28.jdacmdframework.CommandContext;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReflectionExecutor implements Command.Executor {
    private Object invokeWith;
    private List<Method> paramGetters;
    private Method use;

    @AllArgsConstructor
    public class InvalidReflectionClassException extends RuntimeException {
        private String message;

        @Override
        public String getMessage() {
            return message;
        }
    }

    public ReflectionExecutor(Class<?> clazz, Command command) {
        invokeWith = new Object();
        loadWith(clazz, true, command);
    }

    public ReflectionExecutor(Object instance, Command command) {
        invokeWith = instance;
        loadWith(instance.getClass(), false, command);
    }

    private void loadWith(Class<?> clazz, boolean shouldBeStatic, Command command) {
        Arrays.stream(clazz.getMethods()).filter(cur -> (Modifier.isStatic(cur.getModifiers()) == shouldBeStatic) &&
                Modifier.isPublic(cur.getModifiers()) && cur.isAnnotationPresent(CommandExecutor.class)).findFirst()
                .ifPresent(method -> {
            Stream<Parameter> params = Arrays.stream(method.getParameters()).skip(1);
            if (params.anyMatch(cur -> !AnnotationConstants.annotationTypeMap.containsKey(cur.getType())))
                throw new InvalidReflectionClassException("Can't make reflection executor with method '"
                        + clazz.getName() + "." + method.toString() + "', it contains an unsupported parameter.");

            paramGetters = params.map(param ->
                    AnnotationConstants.methodAnnotationTypes.get(param.getType())).collect(Collectors.toList());
            use = method;
        });
        if (use == null) throw new InvalidReflectionClassException("Can't make reflection executor out of " + clazz.getName()
                + " because no methods fit 'public" + (shouldBeStatic ? " static'" : "'"));

        Arrays.stream(clazz.getDeclaredClasses()).filter(cur -> Modifier.isStatic(cur.getModifiers()) &&
                Modifier.isPublic(cur.getModifiers()) && cur.isAnnotationPresent(SubCommand.class)).findFirst()
                .ifPresent(subCommandClass -> {
            String aliases = subCommandClass.getAnnotation(SubCommand.class).aliases();
            ReflectionExecutor childExecutor = new ReflectionExecutor(subCommandClass.getClass(), command);
            command.sub(aliases, childExecutor);
        });
    }

    @Override
    public Object execute(CommandContext context) {
        List<Object> objects = new ArrayList<>();
        objects.add(context);

        paramGetters.forEach(cur -> {
            try {
                objects.add(cur.invoke(context));
            } catch (Exception e) {
                objects.add(null);
            }
        });
        try {
            return use.invoke(invokeWith, objects.toArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
