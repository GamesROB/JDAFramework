package me.deprilula28.jdacmdframework;

import java.util.function.Function;

public class Utility {
    public static <T> T rethrow(Function<Exception, RuntimeException> rethrow, Function<Void, T> function) {
        try {
            return function.apply(null);
        } catch (Exception ex) {
            throw rethrow.apply(ex);
        }
    }
}
