package ru.iteco.ffi.task2.cache;

import org.apache.log4j.Logger;
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
 * Threadsafe cache of key-value file. Reloads on given amount of reads.
 */
@Service
public class MyCacheServiceImpl implements MyCacheService {
    private static Logger log = Logger.getLogger(MyCacheServiceImpl.class);

    private Resource path;
    private Integer readsLimit;
    private AtomicInteger readsCount = new AtomicInteger(0);
    private Map<String, String> cache = new HashMap<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public MyCacheServiceImpl(Integer readsLimit, Resource path) throws IOException {
        this.readsLimit = readsLimit;
        this.path = path;
        loadCache();
    }

    /**
     * Get current reads count
     *
     * @return
     */
    public int getReadsCount() {
        return readsCount.get();
    }

    private void loadCache() throws IOException {
        try (Stream<String> stream = Files.lines(path.getFile().toPath())) {
            cache = stream.map(s -> s.split("=")).collect(Collectors.toMap(ar -> ar[0], ar -> ar[1]));
        }
        log.debug("loaded cache on readsCount=" + readsCount.get());
        readsCount.set(0);
    }

    /**
     * Get cashed value
     *
     * @param key
     * @return
     * @throws IOException
     */
    public String get(String key) throws IOException {
        String value;

        do {
            lock.readLock().lock();

            // check if cache expired
            if (readsCount.get() >= readsLimit) {
                lock.readLock().unlock();

                // get lock on cache updating
                lock.writeLock().lock();
                try {

                    // check if steel need to update cache
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
            } finally {
                lock.readLock().unlock();
            }

            // this is needed because two thread can go through cache expire check on readsCount = readsLimit - 1
            // one of them will have to reload cache and get value again
        } while (readsCount.incrementAndGet() > readsLimit);

        return value;
    }
}
