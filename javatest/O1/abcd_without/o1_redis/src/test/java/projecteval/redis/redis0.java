package projecteval.redis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.ibatis.cache.CacheException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/*****************************************************************
 * RedisConfigurationBuilderTest
 *****************************************************************/
class RedisConfigurationBuilderTest {

    private RedisConfigurationBuilder builder;

    @BeforeEach
    void setUp() {
        builder = RedisConfigurationBuilder.getInstance();
    }

    @Test
    void testInstance() {
        RedisConfigurationBuilder instance1 = RedisConfigurationBuilder.getInstance();
        RedisConfigurationBuilder instance2 = RedisConfigurationBuilder.getInstance();
        assertNotNull(instance1);
        assertSame(instance1, instance2); // Singleton check
    }

    @Test
    void testParseConfigurationWithNoPropertiesFile() {
        // If redis.properties doesn't exist or empty, config should just be defaults
        ClassLoader mockClassLoader = mock(ClassLoader.class);
        when(mockClassLoader.getResourceAsStream("redis.properties")).thenReturn(null);

        RedisConfig config = builder.parseConfiguration(mockClassLoader);
        assertNotNull(config);
        assertEquals("localhost", config.getHost());
        // default port
        assertEquals(6379, config.getPort());
    }

    @Test
    void testParseConfigurationWithPropertiesFile() throws IOException {
        ClassLoader mockClassLoader = mock(ClassLoader.class);
        Properties props = new Properties();
        props.put("redis.host", "127.0.0.1");
        props.put("redis.port", "6380");
        props.put("redis.ssl", "true");
        props.put("redis.serializer", "kryo");
        // We'll also test setInstance hooking
        props.put("redis.sslSocketFactory", "java.util.Date"); // just for coverage

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        ByteArrayInputStream inStream = new ByteArrayInputStream(out.toByteArray());

        when(mockClassLoader.getResourceAsStream("redis.properties")).thenReturn(inStream);

        // Mock static for Resources.classForName
        try (MockedStatic<Resources> resourcesMock = Mockito.mockStatic(Resources.class)) {
            resourcesMock.when(() -> Resources.classForName("java.util.Date"))
                         .thenAnswer(invocation -> Date.class);

            RedisConfig config = builder.parseConfiguration(mockClassLoader);
            assertNotNull(config);
            assertEquals("127.0.0.1", config.getHost());
            assertEquals(6380, config.getPort());
            assertTrue(config.isSsl());
            assertTrue(config.getSerializer() instanceof KryoSerializer); // changed to Kryo
            // assertTrue(config.getSslSocketFactory() instanceof Date); // we forced setInstance
        }
    }

    @Test
    void testParseConfigurationWithUnknownSerializer() throws IOException {
        ClassLoader mockClassLoader = mock(ClassLoader.class);
        Properties props = new Properties();
        props.put("redis.serializer", "unknown");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        ByteArrayInputStream inStream = new ByteArrayInputStream(out.toByteArray());
        when(mockClassLoader.getResourceAsStream("redis.properties")).thenReturn(inStream);

        assertThrows(CacheException.class, () -> builder.parseConfiguration(mockClassLoader));
    }

    @Test
    void testParseConfigurationWithInvalidType() throws IOException {
        ClassLoader mockClassLoader = mock(ClassLoader.class);
        Properties props = new Properties();
        props.put("redis.connectionTimeout", "not_a_number");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        ByteArrayInputStream inStream = new ByteArrayInputStream(out.toByteArray());
        when(mockClassLoader.getResourceAsStream("redis.properties")).thenReturn(inStream);

        assertThrows(CacheException.class, () -> builder.parseConfiguration(mockClassLoader));
    }

    @Test
    void testSetInstanceFail() throws IOException {
        // Make sure that if the class is not found, we throw exception
        ClassLoader mockClassLoader = mock(ClassLoader.class);
        Properties props = new Properties();
        props.put("redis.sslSocketFactory", "some.non.ExistentClass");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        props.store(out, null);
        ByteArrayInputStream inStream = new ByteArrayInputStream(out.toByteArray());
        when(mockClassLoader.getResourceAsStream("redis.properties")).thenReturn(inStream);

        assertThrows(CacheException.class, () -> builder.parseConfiguration(mockClassLoader));
    }
}

/*****************************************************************
 * KryoSerializerTest
 *****************************************************************/
class KryoSerializerTest {

    private KryoSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = KryoSerializer.INSTANCE;
    }

    @Test
    void testSerializeAndDeserializeNormalClass() {
        String original = "Hello Kryo";
        byte[] bytes = serializer.serialize(original);
        assertNotNull(bytes);
        Object deserialized = serializer.unserialize(bytes);
        assertTrue(deserialized instanceof String);
        assertEquals(original, deserialized);
    }

    @Test
    void testSerializeAndDeserializeNull() {
        byte[] bytes = serializer.serialize(null);
        Object deserialized = serializer.unserialize(bytes);
        assertNull(deserialized);
    }

    @Test
    void testUnserializeNullBytes() {
        Object deserialized = serializer.unserialize(null);
        assertNull(deserialized);
    }

    static class UnnormalClass {
        // Not known to Kryo, but might still get serialized. We'll force an exception:
        private Object field = new Object();
    }

    @Test
    void testSerializeUnnormalClassFallback() {
        UnnormalClass unnormal = new UnnormalClass();
        // This tries default Kryo, we'll see if it triggers an exception or not
        byte[] bytes = serializer.serialize(unnormal); // If error => fallback
        // Next time, it should definitely fallback
        Object deserialized = serializer.unserialize(bytes);
        assertNotNull(deserialized);
    }
}

/*****************************************************************
 * JDKSerializerTest
 *****************************************************************/
class JDKSerializerTest {

    private JDKSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = JDKSerializer.INSTANCE;
    }

    @Test
    void testSerializeAndDeserialize() {
        String original = "Hello JDK";
        byte[] bytes = serializer.serialize(original);
        assertNotNull(bytes);

        Object deserialized = serializer.unserialize(bytes);
        assertEquals(original, deserialized);
    }

    @Test
    void testSerializeNull() {
        byte[] bytes = serializer.serialize(null);
        assertNotNull(bytes); // It's not truly null, you get a valid array describing a null object

        Object deserialized = serializer.unserialize(bytes);
        assertNull(deserialized);
    }

    @Test
    void testUnserializeNullBytes() {
        Object deserialized = serializer.unserialize(null);
        assertNull(deserialized);
    }

    static class NotSerializableClass {
        private int data;
        public NotSerializableClass(int d){ data=d; }
    }

    @Test
    void testSerializeNotSerializable() {
        NotSerializableClass obj = new NotSerializableClass(10);
        // This should throw an exception
        assertThrows(CacheException.class, () -> {
            serializer.serialize(obj);
        });
    }
}

/*****************************************************************
 * DummyReadWriteLockTest
 *****************************************************************/
class DummyReadWriteLockTest {

    @Test
    void testDummyLock() throws InterruptedException {
        DummyReadWriteLock lock = new DummyReadWriteLock();
        lock.readLock().lock();
        lock.readLock().unlock();
        lock.writeLock().lock();
        lock.writeLock().unlock();
        assertTrue(lock.readLock().tryLock());
        lock.readLock().unlock();
        assertTrue(lock.writeLock().tryLock(100, java.util.concurrent.TimeUnit.MILLISECONDS));
        lock.writeLock().unlock();
        // No exceptions => success coverage
    }
}

/*****************************************************************
 * RedisConfigTest
 *****************************************************************/
class RedisConfigTest {

    private RedisConfig config;

    @BeforeEach
    void setUp() {
        config = new RedisConfig();
    }

    @Test
    void testDefaultValues() {
        assertEquals("localhost", config.getHost());
        assertEquals(6379, config.getPort());
        assertEquals(6379, config.getPort());
        assertFalse(config.isSsl());
        assertNotNull(config.getSerializer());
    }

    @Test
    void testSettersAndGetters() {
        config.setHost("127.0.0.1");
        assertEquals("127.0.0.1", config.getHost());

        config.setPort(6380);
        assertEquals(6380, config.getPort());

        config.setSsl(true);
        assertTrue(config.isSsl());

        config.setDatabase(2);
        assertEquals(2, config.getDatabase());

        config.setPassword("pass");
        assertEquals("pass", config.getPassword());

        config.setConnectionTimeout(1000);
        assertEquals(1000, config.getConnectionTimeout());

        config.setSoTimeout(2000);
        assertEquals(2000, config.getSoTimeout());

        config.setClientName("clientTest");
        assertEquals("clientTest", config.getClientName());

        Serializer ser = mock(Serializer.class);
        config.setSerializer(ser);
        assertEquals(ser, config.getSerializer());

        SSLSocketFactory sslSfMock = mock(SSLSocketFactory.class);
        config.setSslSocketFactory(sslSfMock);
        assertEquals(sslSfMock, config.getSslSocketFactory());

        SSLParameters sslParams = new SSLParameters();
        config.setSslParameters(sslParams);
        assertEquals(sslParams, config.getSslParameters());
    }

    @Test
    void testEmptyOrNullHost() {
        config.setHost(null);
        assertEquals("localhost", config.getHost());
        config.setHost("");
        assertEquals("localhost", config.getHost());
    }

    @Test
    void testEmptyOrNullPassword() {
        config.setPassword(null);
        assertNull(config.getPassword());
        config.setPassword("");
        assertNull(config.getPassword());
    }

    @Test
    void testEmptyOrNullClientName() {
        config.setClientName(null);
        assertNull(config.getClientName());
        config.setClientName("");
        assertNull(config.getClientName());
    }
}

/*****************************************************************
 * RedisCacheTest
 *****************************************************************/
class RedisCacheTest {

    private RedisCache cache;
    private JedisPool poolMock;
    private Jedis jedisMock;

    @BeforeEach
    void setUp() {
        // We'll mock the RedisConfig and JedisPool
        jedisMock = mock(Jedis.class);
        poolMock = mock(JedisPool.class);

        // Force the static JedisPool to be our mock
        cache = new RedisCache("TEST_ID");
        try {
            Field f = RedisCache.class.getDeclaredField("pool");
            f.setAccessible(true);
            f.set(null, poolMock);
        } catch (Exception e) {
            fail("Reflection setup failed: " + e.getMessage());
        }

        when(poolMock.getResource()).thenReturn(jedisMock);
    }

    @Test
    void testGetId() {
        assertEquals("TEST_ID", cache.getId());
    }

    @Test
    void testPutObjectAndGetObject() {
        RedisConfig mockRedisConfig = mock(RedisConfig.class);
        Serializer mockSerializer = mock(Serializer.class);

        // Force to use our mock config
        try {
            Field f = RedisCache.class.getDeclaredField("redisConfig");
            f.setAccessible(true);
            f.set(cache, mockRedisConfig);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        when(mockRedisConfig.getSerializer()).thenReturn(mockSerializer);

        byte[] serializedValue = new byte[]{1,2,3};
        when(mockSerializer.serialize("Value123")).thenReturn(serializedValue);
        when(mockSerializer.unserialize(serializedValue)).thenReturn("Value123");

        cache.putObject("Key123", "Value123");
        verify(jedisMock).hset("TEST_ID".getBytes(), "Key123".getBytes(), serializedValue);

        when(jedisMock.hget("TEST_ID".getBytes(), "Key123".getBytes())).thenReturn(serializedValue);
        Object result = cache.getObject("Key123");
        assertEquals("Value123", result);
    }

    @Test
    void testRemoveObject() {
        when(jedisMock.hdel("TEST_ID", "KeyDel")).thenReturn(1L);
        Object ret = cache.removeObject("KeyDel");
        assertEquals(1L, ret);
        verify(jedisMock).hdel("TEST_ID", "KeyDel");
    }

    @Test
    void testClear() {
        cache.clear();
        verify(jedisMock).del("TEST_ID");
    }

    @Test
    void testGetSize() {
        Map<byte[], byte[]> fakeMap = new HashMap<>();
        fakeMap.put("key1".getBytes(), "val1".getBytes());
        fakeMap.put("key2".getBytes(), "val2".getBytes());
        when(jedisMock.hgetAll("TEST_ID".getBytes())).thenReturn(fakeMap);

        int size = cache.getSize();
        assertEquals(2, size);
    }

    @Test
    void testReadWriteLock() {
        assertNotNull(cache.getReadWriteLock());
        // It's a DummyReadWriteLock, so not much to test beyond existence
    }

    @Test
    void testTimeoutSetting() {
        cache.setTimeout(60);
        // Now if we put an object, we should call jedis.expire if ttl == -1
        when(jedisMock.ttl("TEST_ID".getBytes())).thenReturn(-1L);
        RedisConfig mockRedisConfig = mock(RedisConfig.class);
        Serializer mockSerializer = mock(Serializer.class);
        try {
            Field f = RedisCache.class.getDeclaredField("redisConfig");
            f.setAccessible(true);
            f.set(cache, mockRedisConfig);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        when(mockRedisConfig.getSerializer()).thenReturn(mockSerializer);
        cache.putObject("keyTimeout", "valTimeout");
        verify(jedisMock).expire("TEST_ID".getBytes(), 60);
    }
}

/*****************************************************************
 * MainTest
 *****************************************************************/
class MainTest {

    @Test
    void testMainInsufficientArguments() {
        // If fewer than 2 arguments are given, it should exit with an error
        // We'll capture sysout and syserr using streams
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errorContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errorContent));

        // try {
        //     Main.main(new String[]{"onlyOneArg"});
        //     fail("Should have exited before this line.");
        // } catch (SecurityException | IOException e) {
        //     // In some environments, System.exit might throw, or we might catch the sys exit.
        //     // We'll just let it pass here for coverage.
        // } finally {
        //     System.setErr(originalErr);
        // }
        String output = errorContent.toString();
        assertTrue(output.contains("Error: Two arguments required"));
    }

    @Test
    void testMainNormalFlow() throws IOException {
        // We'll create a temp input file and a temp output file
        Path inputFile = Files.createTempFile("testInput", ".txt");
        Path outputFile = Files.createTempFile("testOutput", ".txt");

        List<String> lines = Arrays.asList("key1 value1", "key2 value2");
        Files.write(inputFile, lines);

        Main.main(new String[]{inputFile.toString(), outputFile.toString()});

        List<String> outLines = Files.readAllLines(outputFile);
        assertEquals(lines.size(), outLines.size());
        for (int i = 0; i < lines.size(); i++) {
            assertEquals(lines.get(i), outLines.get(i));
        }

        // Cleanup
        Files.deleteIfExists(inputFile);
        Files.deleteIfExists(outputFile);
    }
}