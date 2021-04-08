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

    /**
     * A directory of files that need to be scanned.
     */
    private final File corpusDirectory;

    /**
     * A limit for total size of files in a single subtask.
     */
    private final long sizeLimit;

    /**
     * Creates a new <code>FileScanningJob</code> for
     * given directory with size limit specified in
     * <code>AppConfig</code>.
     * @param corpusDirectory a directory to be scanned
     *                        for keywords
     */
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

    /**
     * Initiates a file scanning task and returns corresponding <code>Future</code>
     * object. Corpus is searched for text files and the task is divided based on
     * specified size limit. Subtasks are computed asynchronously and merged upon
     * completion.
     * @param executorService a service which will execute the word counting task
     * @return <code>Future</code> object that holds the result of computation
     */
    @Override
    public Future<Map<String, Integer>> initiate(ExecutorService executorService) {
        return executorService.submit(() -> countKeywords(executorService));
    }

    /**
     * A method that represents base task of this object. Directory is searched
     * for files and the task is split based on specified size limit. On completion,
     * partial results are merged into one <code>Map</code> object.
     * @param executorService a service which executes subtasks
     * @return a <code>Map</code> which holds results of word counting, where
     * keys are keywords specified in <code>AppConfig</code>, and values are
     * occurrences of those keywords in all files in corpus directory
     */
    private Map<String, Integer> countKeywords(ExecutorService executorService) {
        Logger.info("Started job for corpus: " + getQuery());
        File[] files = Utils.nullableToEmpty(corpusDirectory.listFiles());
        List<Future<Map<String, Integer>>> subtasks = splitJobIntoTasks(files, executorService);
        return mergePartialResults(subtasks);
    }

    /**
     * Splits main task into multiple subtasks, based on specified size limit. Files are
     * grouped greedy, that is, a list for subtask will take files as long as the total
     * size is less than size limit. Upon reaching limit or the end of the file list,
     * a new <code>FileScanningTask</code> is created and submitted for execution to
     * specified <code>ExecutorService</code> and is put into result list which holds
     * all <code>Future</code> objects from all submitted subtasks.
     * @param files a list of files in corpus directory
     * @param executorService a service which executes subtasks
     * @return list of <code>Future</code> objects of all submitted subtasks
     */
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

    /**
     * Takes a list of <code>Future</code> objects, iterates over them and puts all
     * results of computation into a list. After all the tasks are finished, partial
     * results are merged into a single <code>Map</code> object which holds a cumulative
     * result.
     * @param tasks a list of subtasks represented by <code>Future</code> object
     * @return a <code>Map</code> where keys are keywords specified in <code>AppConfig</code>,
     * and corresponding values are number of occurrences of each keyword in corpus
     */
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
