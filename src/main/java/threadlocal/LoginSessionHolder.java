package threadlocal;

import java.util.Map;

public class LoginSessionHolder {
    private static final ThreadLocal<Map<String, String>> loginThreadLocal = new ThreadLocal<>();

    public static void init(Map<String, String> map) {
        loginThreadLocal.set(map);
    }

    public static String get(String key) {
        Map<String, String> map = loginThreadLocal.get();
        return map.get(key);
    }

    public static void set(String key, String value) {
        Map<String, String> map = loginThreadLocal.get();
        map.put(key, value);
    }

    public static void clear() {
        loginThreadLocal.remove();
        ;
    }
}
