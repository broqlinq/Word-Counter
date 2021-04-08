package rs.raf.kids.kwc.job;

import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.config.AppConfig;
import rs.raf.kids.kwc.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FileScanningJob implements ScanningJob {

    private final File corpusDirectory;

    private final long sizeLimit;

    public FileScanningJob(File corpusDirectory) {
        this.corpusDirectory = corpusDirectory;
        sizeLimit = AppConfig.fileSizeLimit;
    }

    @Override
    public ScanType getScanType() {
        return ScanType.FILE;
    }

    @Override
    public String getQuery() {
        return "file|" + corpusDirectory.getName();
    }

    @Override
    public Future<Map<String, Integer>> initiate(ExecutorService executorService) {
        return executorService.submit(() -> countKeywords(executorService));
    }

    private Map<String, Integer> countKeywords(ExecutorService executorService) {
        Logger.info("Started job for corpus: " + getQuery());
        File[] files = Utils.nullableToEmpty(corpusDirectory.listFiles());
        List<Future<Map<String, Integer>>> subtasks = splitJobIntoTasks(files, executorService);
        return mergePartialResults(subtasks);
    }

    private List<Future<Map<String, Integer>>> splitJobIntoTasks(File[] files, ExecutorService executorService) {
        List<Future<Map<String, Integer>>> subtasks = new ArrayList<>();
        List<File> taskFiles = new ArrayList<>();
        long size = 0;
        for (int i = 0, filesLength = files.length; i < filesLength; i++) {
            File file = files[i];
            size += file.length();
            taskFiles.add(file);
            if (size >= sizeLimit || i == filesLength - 1) {
                Future<Map<String, Integer>> task = executorService.submit(new FileScanningTask(taskFiles));
                subtasks.add(task);
                taskFiles = new ArrayList<>();
                size = 0;
            }
        }
        return subtasks;
    }

    private Map<String, Integer> mergePartialResults(List<Future<Map<String, Integer>>> tasks) {
        List<Map<String, Integer>> partialResults = new ArrayList<>();
        for (Future<Map<String, Integer>> subtask : tasks) {
            try {
                partialResults.add(subtask.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        Logger.debugInfo("Finished job for corpus: " + corpusDirectory);
        return Utils.combineKeywordMaps(partialResults);
    }

}
