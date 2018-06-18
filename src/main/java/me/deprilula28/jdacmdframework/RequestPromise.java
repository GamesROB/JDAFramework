package me.deprilula28.jdacmdframework;

import net.dv8tion.jda.core.requests.RestAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class RequestPromise<T> implements Consumer<T> {
    private final List<Consumer<T>> extraHandlers = new ArrayList<>();
    private Optional<T> response = Optional.empty();

    public static <T> RequestPromise<T> valueProvided(T value) {
        RequestPromise<T> promise = new RequestPromise<>();
        promise.accept(value);
        return promise;
    }

    public static <T> RequestPromise<T> forAction(RestAction<T> restAction) {
        RequestPromise<T> promise = new RequestPromise<>();
        restAction.queue(promise);
        return promise;
    }

    public RequestPromise<T> then(Consumer<T> consumer) {
        synchronized (extraHandlers) {
            if (response.isPresent()) consumer.accept(response.get());
            else extraHandlers.add(consumer);
        }
        return this;
    }

    public <A> RequestPromise<A> morph(Function<T, A> morphFunction) {
        RequestPromise<A> promise = new RequestPromise<>();
        then(object -> promise.accept(morphFunction.apply(object)));
        return promise;
    }

    public <A> RequestPromise<A> morphAction(Function<T, RestAction<A>> morphFunction) {
        RequestPromise<A> promise = new RequestPromise<>();
        then(object -> morphFunction.apply(object).queue(promise));
        return promise;
    }

    public T await() {
        try {
            while (!response.isPresent()) wait(30L);
            return response.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void accept(T t) {
        synchronized (extraHandlers) {
            response = Optional.of(t);
            extraHandlers.forEach(cur -> cur.accept(t));
            extraHandlers.clear();
        }

    }
}
