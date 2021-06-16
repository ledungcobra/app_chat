package utils;

@FunctionalInterface
public interface UnitOfWork2<T> {
    T doWork();
}
