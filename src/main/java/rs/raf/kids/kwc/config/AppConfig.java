package rs.raf.kids.kwc.config;

import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class AppConfig {
    private static final String KEY_KEYWORDS = "keywords";
    private static final String KEY_CORPUS_PREFIX = "file_corpus_prefix";
    private static final String KEY_FILE_SIZE_LIMIT = "file_scanning_size_limit";
    private static final String KEY_CRAWLER_SLEEP_TIME = "directory_crawler_sleep_time";
    private static final String KEY_URL_DEPTH_LIMIT = "web_scanning_depth_limit";
    private static final String KEY_URL_REFRESH_TIME = "url_refresh_time";

    private static Properties properties;
    public static Set<String> keywords;
    public static String corpusPrefix;
    public static long fileSizeLimit;
    public static long crawlerSleepTime;
    public static int urlDepthLimit;
    public static long urlRefreshTime;

    static {
        loadProperties();
        validateProperties();
        initProperties();
    }

    private static void loadProperties() {
        properties = new Properties();
        try (InputStream stream = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            properties.load(stream);
        } catch (IOException e) {
            Logger.debugError("Error | Failed to load properties from app.properties: " + e.getMessage());
        }
    }

    private static void validateProperties() {

    }

    private static void initProperties() {
        String keywordValues = properties.getProperty("keywords", "");
        keywords = Arrays.stream(keywordValues.split(","))
                .map(String::trim)
                .collect(Collectors.toUnmodifiableSet());

        corpusPrefix = properties.getProperty("file_corpus_prefix", "corpus_");
        Logger.debugError("corpusPrefix=" + corpusPrefix);

        String fileSizeLimitValue = properties.getProperty("file_scanning_size_limit", "1048576");
        fileSizeLimit = Utils.parseIntOrDefault(fileSizeLimitValue, 1048576,
                () -> {
//            Logger.def.warn("WARN", "Invalid value", "file_scanning_size_limit is being set to default value of 1048576");
            properties.setProperty("file_scanning_size_limit", "1048576");
        });
        Logger.debugError("fileSizeLimit=" + fileSizeLimit);

        String crawlerSleepTimeValue = properties.getProperty("directory_crawler_sleep_time", "1000");
        crawlerSleepTime = Utils.parseLongOrDefault(crawlerSleepTimeValue, 1000L, () -> {}
//                () -> Logger.def.warn("WARN", "Invalid value", "directory_crawler_sleep_time is being set to default value of 1000")
        );
        Logger.debugError("crawlerSleepTimeValue=" + crawlerSleepTimeValue);

        String urlDepthLimitValue = properties.getProperty("web_scanning_depth_limit", "1");
        urlDepthLimit = Utils.parseIntOrDefault(urlDepthLimitValue, 1, () -> {}
//                () -> Logger.def.warn("WARN", "Invalid value", "web_scanning_depth_limit is being set to default value of 1")
        );
        Logger.debugError("urlDepthLimitValue=" + urlDepthLimitValue);

        String urlRefreshTimeValue = properties.getProperty("url_refresh_time", "86400000");
        urlRefreshTime = Utils.parseLongOrDefault(urlRefreshTimeValue, 86400000L, () -> {}
//                () -> Logger.def.warn("WARN", "Invalid value", "url_refresh_time is being set to default value of 86400000")
        );
        Logger.debugError("urlRefreshTimeValue=" + urlRefreshTimeValue);
    }

    public static Properties properties() {
        return (Properties) properties.clone();
    }

    private AppConfig() {}

}
