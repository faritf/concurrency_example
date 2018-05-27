package cache;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Дано: файл в формате key(строка)-value(объект, можно рассматривать как строку)
 *      Реализовать потокобезопасный сервис с кешом значений, считанных из файла, и обновлением при заданном количестве обращений.
 *      Доказать предсказуемость набора значений в кеше.
 */
@Service
public class MyCashServiceImpl implements MyCashService {
    private Resource path;
    private Integer readsLimit;
    private AtomicInteger readsCount = new AtomicInteger(0);
    private Map<String, String> cache = new HashMap<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public MyCashServiceImpl() {
    }

    public MyCashServiceImpl(Integer readsLimit, Resource path) throws IOException {
        this.readsLimit = readsLimit;
        this.path = path;
        loadCache();
    }

    public int getReadsCount(){
        return readsCount.get();
    }

    private void loadCache() throws IOException {
        try (Stream<String> stream = Files.lines(path.getFile().toPath())) {
            cache = stream.map(s -> s.split("=")).collect(Collectors.toMap(ar->ar[0], ar->ar[1]));
        }
        readsCount.set(0);
    }

    public String get(String key) throws IOException {
        String value;

        lock.readLock().lock();
        if (readsCount.get() >= readsLimit) {
            lock.readLock().unlock();
            lock.writeLock().lock();
            try {
                if (readsCount.get() >= readsLimit) {
                    loadCache();
                }
                lock.readLock().lock();
            } finally {
                lock.writeLock().unlock();
            }
        }

        try {
            value = cache.get(key);
            readsCount.incrementAndGet();
        } finally {
            lock.readLock().unlock();
        }

        return value;
    }
}
