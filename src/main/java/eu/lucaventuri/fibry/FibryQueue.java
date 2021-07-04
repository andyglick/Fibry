package eu.lucaventuri.fibry;

import eu.lucaventuri.functional.Either3;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

public class FibryQueue<T, R, S> extends LinkedBlockingDeque<Either3<Consumer<S>, T, MessageWithAnswer<T, R>>> implements MiniFibryQueue<T,R,S> {
    public FibryQueue() {
    }

    public FibryQueue(int capacity) {
        super(capacity);
    }
}
