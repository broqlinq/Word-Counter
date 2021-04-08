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

    private final List<File> filesToScan;

    private final String corpusName;

    private final Set<String> keywords;

    public FileScanningTask(List<File> filesToScan) {
        this.filesToScan = filesToScan;
        corpusName = filesToScan.get(0).getParentFile().getName();
        keywords = AppConfig.keywords;
    }

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

    private List<String> filesToWords(List<File> files) {
        return files.stream()
                .flatMap(this::fileToLines)
                .flatMap(this::lineToWords)
                .map(Utils::removePunctuation)
                .filter(keywords::contains)
                .toList();
    }

    private Stream<String> fileToLines(File file) {
        try {
            return Files.lines(file.toPath());
        } catch (IOException e) {
            return Stream.empty();
        }
    }

    private Stream<String> lineToWords(String line) {
        return Arrays.stream(line.split(" "));
    }

}
