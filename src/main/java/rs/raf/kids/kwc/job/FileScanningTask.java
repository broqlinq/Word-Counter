package rs.raf.kids.kwc.job;

import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.config.AppConfig;
import rs.raf.kids.kwc.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public class FileScanningTask implements Callable<Map<String, Integer>> {

    /**
     * A list of files that are being scanned for keywords.
     */
    private final List<File> filesToScan;

    private final String corpusName;

    /**
     * Keywords that are being counted in each file.
     */
    private final Set<String> keywords;

    public FileScanningTask(List<File> filesToScan) {
        this.filesToScan = filesToScan;
        corpusName = filesToScan.get(0).getParentFile().getName();
        keywords = AppConfig.keywords;
    }

    /**
     * Initiates an empty <code>Map</code> object with keywords as keys.
     * Words in file are then extracted, filtered and counted.
     * @return occurrences of each keyword
     */
    @Override
    public Map<String, Integer> call() {
//        Logger.info("Started file scan for: file|" + corpusName);
        Map<String, Integer> result = Utils.initKeywordsMap();
        List<String> keywordsInFiles = filesToWords(filesToScan);
        for (String word : keywordsInFiles) {
            result.compute(word, (k,v) -> v + 1);
        }
        Utils.sleepThread(ThreadLocalRandom.current().nextLong(6000, 16000));
//        Logger.debugInfo("Finished file scan for: file|" + corpusName);
        return result;
    }

    /**
     * Transforms files to list of words. Files are first split into lines,
     * lines are then transformed into words, and finally, words are then
     * cleaned and filtered, and put into a list.
     * @param files a list of files to split into keywords
     * @return
     */
    private List<String> filesToWords(List<File> files) {
        return files.stream()
                .flatMap(this::fileToLines)
                .flatMap(this::lineToWords)
                .map(Utils::removePunctuation)
                .filter(keywords::contains)
                .toList();
    }

    /**
     * Takes a single file and splits it into lines. In case of any exception,
     * an empty <code>Stream</code> is returned.
     * @param file file to be split into lines
     * @return a <code>Stream</code> of <code>String</code> objects, which
     * represent lines in file
     */
    private Stream<String> fileToLines(File file) {
        try {
            return Files.lines(file.toPath());
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    /**
     * Splits a string into words by removing whitespace.
     * @param line a string to be split into words
     * @return words of given string
     */
    private Stream<String> lineToWords(String line) {
        return Arrays.stream(line.split(" "));
    }

}
