package me.deprilula28.jdacmdframework;

import net.dv8tion.jda.api.requests.RestAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class RequestPromise<T> {
    private final List<Consumer<T>> extraHandlers = new ArrayList<>();
    private final List<Consumer<Throwable>> errorHandlers = new ArrayList<>();

    private boolean finished;
    private boolean success;

    private T response;
    private Throwable responseError;

    private Consumer<T> consumerSuccess = t -> handleThings(t, true);
    private Consumer<Throwable> consumerFailure = throwable -> handleThings(throwable, false);

    private void handleThings(Object t, boolean success) {
        synchronized (success ? extraHandlers : errorHandlers) {
            finished = true;
            this.success = success;
            if (success) extraHandlers.forEach(cur -> cur.accept((T) t));
            else {
                System.err.println("Exception thrown by JDA Rest Action:");
                ((Throwable) t).printStackTrace();
                errorHandlers.forEach(cur -> cur.accept((Throwable) t));
            }

            if (success) response = (T) t;
            else responseError = (Throwable) t;

            errorHandlers.clear();
            extraHandlers.clear();
        }
    }

    public static <T> RequestPromise<T> valueProvided(T value) {
        RequestPromise<T> promise = new RequestPromise<>();
        promise.consumerSuccess.accept(value);
        return promise;
    }

    public static <T> RequestPromise<T> forAction(RestAction<T> restAction) {
        RequestPromise<T> promise = new RequestPromise<>();
        restAction.queue(promise.consumerSuccess, promise.consumerFailure);
        return promise;
    }

    public RequestPromise<T> then(Consumer<T> consumer) {
        synchronized (extraHandlers) {
            if (finished && success) consumer.accept(response);
            else extraHandlers.add(consumer);
        }
        return this;
    }

    public RequestPromise<T> failure(Consumer<Throwable> consumer) {
        synchronized (errorHandlers) {
            if (finished && !success) consumer.accept(responseError);
            else errorHandlers.add(consumer);
        }
        return this;
    }

    public <A> RequestPromise<A> morph(Function<T, A> morphFunction) {
        RequestPromise<A> promise = new RequestPromise<>();
        then(object -> promise.consumerSuccess.accept(morphFunction.apply(object)));
        return promise;
    }

    public <A> RequestPromise<A> morphAction(Function<T, RestAction<A>> morphFunction) {
        RequestPromise<A> promise = new RequestPromise<>();
        then(object -> morphFunction.apply(object).queue(promise.consumerSuccess, promise.consumerFailure));
        return promise;
    }

    public T await() {
        while (!finished)
            try {
                wait(30L);
            } catch (InterruptedException e) {}
        if (!success) throw new RuntimeException(responseError);
        return response;
    }
}
