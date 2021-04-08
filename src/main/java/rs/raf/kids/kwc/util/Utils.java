package rs.raf.kids.kwc.util;

import org.jsoup.helper.HttpConnection;
import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.config.AppConfig;
import rs.raf.kids.kwc.job.ScanType;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Utils {

    public static int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static int parseIntOrDefault(String value, int defaultValue, Runnable onFail) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            onFail.run();
            return defaultValue;
        }
    }

    public static long parseLongOrDefault(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long parseLongOrDefault(String value, long defaultValue, Runnable onFail) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            onFail.run();
            return defaultValue;
        }
    }

    public static void prettyPrintSummaryResult(ScanType scanType, Map<String, Map<String, Integer>> summary) {
        Logger.success("\n-- " + scanType + " Scan Summary --");
        summary.forEach(Utils::prettyPrintResult);
        Logger.success("----------");
    }

    public static void prettyPrintResult(Object title, Object result) {
        Logger.success("%-25s: %s".formatted(title, result));
    }

    public static Map<String, Integer> initKeywordsMap() {
        return initMap(AppConfig.keywords, () -> 0);
    }

    public static <K, V> Map<K, V> initMap(Collection<K> keys, Supplier<V> defaultValueFactory) {
        Map<K, V> map = new HashMap<>();
        for (K key : keys) {
            map.put(key, defaultValueFactory.get());
        }
        return map;
    }

    public static boolean timeHasExpired(long domainExpirationTime) {
        return domainExpirationTime - System.currentTimeMillis() < 0;
    }

    public static String removePunctuation(String word) {
        return word.replaceAll("\\p{Punct}", "");
    }

    public static Map<String, Integer> combineKeywordMaps(List<Map<String, Integer>> maps) {
        Map<String, Integer> result = initKeywordsMap();
        for (Map<String, Integer> map : maps) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                result.compute(entry.getKey(), (k,v) -> v + entry.getValue());
            }
        }
        return result;
    }

    public static boolean isJobDispatcherThread() {
        String id = Thread.currentThread().getName();
        return id.equals("JobDispatcherThread");
    }

    public static boolean shareSameDomain(String url1, String url2) {
        try {
            URL u1 = new URL(url1);
            URL u2 = new URL(url2);
            return u1.getHost().equals(u2.getHost());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static boolean isOfDomain(String url, String domain) {
        try {
            URL u = new URL(url);
            return u.getHost().equals(domain);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static String extractDomain(String url) {
        try {
            URL u = new URL(url);
            return u.getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static Set<String> extractDomains(Collection<String> urls) {
        return urls.stream()
                .map(Utils::extractDomain)
                .collect(Collectors.toSet());
    }

    public static boolean isValidUrl(String url) {
        try {
            URL u = new URL(url);
            u.toURI();
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            int code = connection.getResponseCode();
            return code == HttpURLConnection.HTTP_OK;
        } catch (URISyntaxException | IOException e) {
            return false;
        }
    }

    public static void waitForThreadToStop(Thread thread) {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException ignored) {}
    }

    /**
     * Wrapper method for <code>Thread.sleep</code>, to avoid unnecessary
     * <code>try/catch</code> blocks.
     * @param millis time for thread to sleep given in milliseconds
     */
    public static void sleepThread(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
    }

    /**
     * Helper method to convert possible <code>null</code> array
     * to an empty one of the same type.
     * @param array An array object which is possibly null.
     * @param <T> Type of elements stored in original array.
     * @return The same array that has been passed if it was
     * not <code>null</code>. Otherwise, an empty array will
     * be returned.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] nullableToEmpty(T[] array) {
        if (array == null) {
            List<T> t = List.of();
            array = (T[]) t.toArray();
        }
        return array;
    }
}
