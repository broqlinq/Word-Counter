package rs.raf.kids.kwc.cli;

public class Logger {

    private Logger() {}

    public enum FontColor {
        black("30"),
        red("31"),
        green("32"),
        yellow("33"),
        blue("34"),
        magenta("35"),
        cyan("36"),
        white("37"),
        blackBright("90"),
        redBright("91"),
        greenBright("92"),
        yellowBright("93"),
        blueBright("94"),
        magentaBright("95"),
        cyanBright("96"),
        whiteBright("97");

        private final String value;

        FontColor(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum FontType {
        normal("0"),
        bold("1"),
        underline("4");

        private final String value;

        FontType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    private static final String reset = "\033[0m";

    private static final String base = "\033[%s;%sm";

    private static boolean debug = false;

    private static String applyFormat(FontColor color, FontType type) {
        return String.format(base, type, color);
    }

    public static void log(String message, FontColor color, FontType type) {
        String effect = applyFormat(color, type);
        System.out.printf("%s%s%s%n", effect, message, reset);
    }

    public static void info(String message) {
        log(message, FontColor.cyanBright, FontType.bold);
    }

    public static void success(String message) {
        log(message, FontColor.greenBright, FontType.bold);
    }

    public static void error(String message) {
        log(message, FontColor.redBright, FontType.normal);
    }

    public static void warn(String message) {
        log(message, FontColor.yellowBright, FontType.bold);
    }

    public static void debugLog(String message, FontColor color, FontType type) {
        if (debug) {
            log("[DEBUG] " + message, color, type);
        }
    }

    public static void debugInfo(String message) {
        debugLog(message, FontColor.cyanBright, FontType.normal);
    }

    public static void debugWarn(String message) {
        debugLog(message, FontColor.yellowBright, FontType.normal);
    }

    public static void debugError(String message) {
        debugLog(message, FontColor.redBright, FontType.normal);
    }

    public static void debugSuccess(String message) {
        debugLog(message, FontColor.yellowBright, FontType.bold);
    }

    public static void setDebug(boolean debugActive) {
        debug = debugActive;
    }
}
