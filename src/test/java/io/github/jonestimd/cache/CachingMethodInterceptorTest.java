package io.github.jonestimd.cache;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

public class CachingMethodInterceptorTest {
    private Map<String, List<String>> invocationKeys = new HashMap<String, List<String>>();
    private CachingMethodInterceptor interceptor = new CachingMethodInterceptor(new CachingTestTarget());

    @Test
    public void testPassthruWhenNoAnnotations() throws Throwable {
        Method method = CachingTestInterface.class.getMethod("getUncacheable", String.class);
        String[] args = { "key1" };

        interceptor.invoke(null, method, args);
        interceptor.invoke(null, method, args);

        assertThat(invocationKeys).hasSize(1);
        assertThat(invocationKeys.get("getUncacheable")).hasSize(2);
    }

    @Test
    public void testCacheForAnnotationOnInterface() throws Throwable {
        checkCacheable(CachingTestInterface.class.getMethod("getCacheable", String.class));
    }

    @Test
    public void testCacheForAnnotationOnImplementation() throws Throwable {
        checkCacheable(CachingTestInterface.class.getMethod("getMaybeCacheable", String.class));
    }

    private void checkCacheable(Method method) throws Throwable {
        String[] args = { "key1" };

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1");

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1");

        args[0] = "key2";

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1", "key2");
    }

    @Test
    public void testCachingWithNonkeyArguments() throws Throwable {
        Method method = CachingTestInterface.class.getMethod("getWithExtraArguments", String.class, String.class);
        String[] args = { "nonkey1", "key1" };

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1");

        args[0] = "nonkey2";

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1");

        args[1] = "key2";

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1", "key2");
    }

    @Test
    public void testCachingWithComplexKey() throws Throwable {
        Method method = CachingTestInterface.class.getMethod("getComplexKey", String.class, String.class);
        String[] args = { "key1a", "key1b" };

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1a,key1b");

        args[0] = "key2a";

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1a,key1b", "key2a,key1b");

        args[1] = "key2b";

        interceptor.invoke(null, method, args);

        assertThat(invocationKeys.get(method.getName())).containsExactly("key1a,key1b", "key2a,key1b", "key2a,key2b");
    }

    private void addInvocation(String methodName, String key) {
        List<String> keys = invocationKeys.get(methodName);
        if (keys == null) {
            keys = new ArrayList<String>();
            invocationKeys.put(methodName, keys);
        }
        keys.add(key);
    }

    @Test
    public void doesNotCacheNullResult() throws Exception {
        CachingTestInterface mock = mock(CachingTestInterface.class);
        when(mock.getCacheable("key")).thenReturn(null, "cached", "not cached");
        CachingTestInterface proxy = (CachingTestInterface) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{CachingTestInterface.class}, new CachingMethodInterceptor(mock));

        assertThat(proxy.getCacheable("key")).isNull();
        assertThat(proxy.getCacheable("key")).isEqualTo("cached");
        assertThat(proxy.getCacheable("key")).isEqualTo("cached");
    }

    public interface CachingTestInterface {
        String getUncacheable(String key);

        @Cacheable
        String getCacheable(String key);

        String getMaybeCacheable(String key);

        @Cacheable
        String getWithExtraArguments(String nonkey, @CacheKey String key);

        @Cacheable
        String getComplexKey(String key1, String key2);
    }

    public class CachingTestTarget implements CachingTestInterface {
        public String getUncacheable(String key) {
            addInvocation("getUncacheable", key);
            return "uncacheable";
        }

        public String getCacheable(String key) {
            addInvocation("getCacheable", key);
            return "cacheable";
        }

        @Cacheable
        public String getMaybeCacheable(String key) {
            addInvocation("getMaybeCacheable", key);
            return "maybeCacheable";
        }

        public String getWithExtraArguments(String nonkey, String key) {
            addInvocation("getWithExtraArguments", key);
            return "withExtraArguments";
        }

        public String getComplexKey(String key1, String key2) {
            addInvocation("getComplexKey", key1 + "," + key2);
            return "complexKey";
        }
    }
}