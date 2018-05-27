package cache;

import java.io.IOException;

public interface MyCashService {
    String get(String key) throws IOException;
    int getReadsCount();
}
