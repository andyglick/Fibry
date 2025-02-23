package eu.lucaventuri.functional;

import eu.lucaventuri.common.ConsumerEx;
import eu.lucaventuri.common.Exceptions;

import java.util.Optional;

/**
 * Functional Either, containing or one type or another
 */
public record Either<L, R>(L left, R right) {

    public static <L, R> Either<L, R> left(L value) {
        Exceptions.assertAndThrow(value != null, "Left value is null!");

        return new Either<>(value, null);
    }

    public static <L, R> Either<L, R> right(R value) {
        Exceptions.assertAndThrow(value != null, "Right value is null!");

        return new Either<>(null, value);
    }

    @SuppressWarnings("unused")
    public boolean isLeft() {
        return left != null;
    }

    @SuppressWarnings("unused")
    public boolean isRight() {
        return right != null;
    }

    @SuppressWarnings("unused")
    public Optional<L> leftOpt() {
        return Optional.ofNullable(left);
    }

    @SuppressWarnings("unused")
    public Optional<R> rightOpt() {
        return Optional.ofNullable(right);
    }

    @SuppressWarnings("unused")
    public <E extends Throwable> void ifLeft(ConsumerEx<L, E> consumer) throws E {
        if (left != null)
            consumer.accept(left);
    }

    @SuppressWarnings("unused")
    public <E extends Throwable> void ifRight(ConsumerEx<R, E> consumer) throws E {
        if (right != null)
            consumer.accept(right);
    }

    public <E extends Throwable> void ifEither(ConsumerEx<L, E> consumerLeft, ConsumerEx<R, E> consumerRight) throws E {
        if (left != null)
            consumerLeft.accept(left);
        else
            consumerRight.accept(right);
    }
}
