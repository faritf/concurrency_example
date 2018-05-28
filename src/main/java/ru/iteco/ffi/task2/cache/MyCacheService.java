package ru.iteco.ffi.task2.cache;

import java.io.IOException;

public interface MyCacheService {
    String get(String key) throws IOException;

    int getReadsCount();
}
