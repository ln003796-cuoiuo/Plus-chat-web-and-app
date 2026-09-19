package org.pluschapluschat.russenger;

public interface GenericProvider<F, T> {
    T provide(F obj);
}
