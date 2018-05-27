package cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class MyCashServiceImplTest {

    @Autowired
    MyCashService service;

    @Test
    public void testGetSerial() throws IOException {
        assertEquals(0, service.getReadsCount());
        assertEquals("value9", service.get("key9"));
        assertEquals(1, service.getReadsCount());
        assertEquals("value4", service.get("key4"));
        assertEquals(2, service.getReadsCount());
        assertEquals("value2", service.get("key2"));
        assertEquals(1, service.getReadsCount());
    }

}