package ru.iteco.ffi.task2.cache;

import java.io.IOException;
import java.nio.file.Path;

public interface MyCacheService {
    String get(String key) throws IOException;
    int getReadsCount();
}
