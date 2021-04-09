package rs.raf.kids.kwc.crawler;

import rs.raf.kids.kwc.Stoppable;
import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.job.FileScanningJob;
import rs.raf.kids.kwc.job.ScanningJob;
import rs.raf.kids.kwc.job.ScanningJobQueue;
import rs.raf.kids.kwc.util.Utils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectoryCrawler implements Runnable, Stoppable {

    /**
     * A list of directories that crawler will
     * scan for corpus directories in each run.
     */
    private final List<File> directories;

    /**
     * A map which holds information about files
     * that are being scanned.
     */
    private final Map<String, FileInfo> fileInfoMap;

    /**
     * A flag to indicate whether the crawler
     * should be running.
     */
    private volatile boolean running;

    /**
     * Time to sleep between individual scan
     * runs, given in milliseconds.
     */
    private final long sleepTime;

    /**
     * Prefix which determines if directory
     * is a candidate for corpus.
     */
    private final String corpusPrefix;

    private long sleepUntil;

    private final ScanningJobQueue scanningJobQueue;

    /**
     * Creates an instance of <code>DirectoryCrawler</code>
     * with an empty directory list and wait time between
     * individual scan runs determined by <code>sleepTime</code>.
     * Corpus directories are recognized by <code>corpusPrefix</code>
     * parameter.
     * @param scanningJobQueue queue to put scanning jobs into
     * @param corpusPrefix a prefix which indicates that directory
     *                     is a corpus directory
     * @param sleepTime time to wait between individual scans.
     */
    public DirectoryCrawler(ScanningJobQueue scanningJobQueue, String corpusPrefix, long sleepTime) {
        this.scanningJobQueue = scanningJobQueue;
        this.corpusPrefix = corpusPrefix;
        this.sleepTime = sleepTime;
        directories = new CopyOnWriteArrayList<>();
        fileInfoMap = new ConcurrentHashMap<>();
    }

    /**
     * Adds a directory to the list, if the given
     * path to directory is valid.
     * @param path path to directory
     * @throws IllegalArgumentException if the specified
     * directory does not exist
     */
    public void addDirectory(String path) {
        File dir = new File(path);
        // if there's no directory at specified path
        // an exception is thrown
        if (!dir.isDirectory())
            throw new IllegalArgumentException(path + " is not a directory");

        // then we check if directory is already in a list
        boolean contained = directories
                .stream()
                .anyMatch(d -> dir.getPath().equals(d.getPath()));

        if (!contained) {
            // if its not, directory is added to the list
            Logger.info("Adding directory '" + path + '\'');
            directories.add(dir);
        } else {
            // otherwise, we notify user that directory is already added
            Logger.warn("Directory '" + path + "' is already added");
        }
    }

    @Override
    public void run() {
        signalRun();
        while (isRunning()) {

            // iterate over list of directories
            // for each corpus directory create new FileScanningJob
            // check if any files in directory were modified and
            // if they have, add newly created job to ScanningJobQueue
            directories.forEach(this::scanForCorpusDirectories);
            pause();
        }
    }

    /**
     * Searches and scans all the possible corpus directories
     * in the given directory, recursively. Directory is considered as
     * corpus directory if its name starts with <code>corpusPrefix</code>.
     * @param directory Directory which is scanned for corpus directories.
     */
    private void scanForCorpusDirectories(File directory) {

        // get the list of files in directory
        // and convert it into empty array if
        // listFiles returned null
        File[] files = Utils.nullableToEmpty(directory.listFiles());

        // now iterate over all the files
        for (File dir : files) {

            // in case the file is directory
            if (dir.isDirectory()) {

                // and its name starts with corpusPrefix
                // check if scanning job is required
                if (dir.getName().startsWith(corpusPrefix)) {
                    checkDirectoryForScanningJob(dir);
                }
                // otherwise, search for possible corpus
                // directories recursively inside directory
                else {
                    scanForCorpusDirectories(dir);
                }
            }
        }
    }

    /**
     * Iterates through all the files in the given directory and updates
     * the <code>lastModified</code> property of a given file in
     * <code>fileInfoMap</code> if its needed. In case any of the files
     * were modified since the last run, a new <code>FileScanningJob</code>
     * is created and submitted to <code>scanningJobQueue</code>.
     * @param directory corpus directory to scan for modifications
     */
    private void checkDirectoryForScanningJob(File directory) {

        // first get all the files inside directory
        File[] files = Utils.nullableToEmpty(directory.listFiles());

        // then iterate over all the files to check
        // if any of them were modified
        boolean shouldStartJob = Arrays.stream(files)
                .filter(File::isFile)
                .map(this::updateFileInfo)
                .reduce(false, Boolean::logicalOr, Boolean::logicalOr);

        // if shouldStartJob is true, we create new ScanningJob
        // and submit it to ScanningJobQueue
        if (shouldStartJob) {

            // Job creation code goes here
//            Logger.def.info("INFO", Thread.currentThread().getName(), "Creating job for corpus: " + directory);
            ScanningJob job = new FileScanningJob(directory);
            scanningJobQueue.submit(job);
        }
    }

    /**
     * Updates <code>FileInfo</code> of corresponding file in <code>fileInfoMap</code>.
     * If its <code>FileInfo</code> was not found, a new instance is created and put
     * into <code>fileInfoMap</code>.
     * @param file file whose info will be updated
     * @return <code>true</code> is file wasn't registered before or if it has expired
     */
    private boolean updateFileInfo(File file) {

        // filePath will be used as unique key for fileInfoMap
        String filepath = file.getPath();
        // we get lastModified value to compare with existing fileInfo
        long lastModified = file.lastModified();

        // if file was checked earlier, it will be registered in fileMap
        if (fileInfoMap.containsKey(filepath)) {

            // we retrieve fileInfo for the current file
            FileInfo fileInfo = fileInfoMap.get(filepath);

            // if modification occurred in, lastModified
            // values will no be the same
            if (fileInfo.lastModified != lastModified) {

                // in that case we update lastModified value
                // and return true as a signal that scanning
                // job should be created
                fileInfo.lastModified = lastModified;
                return true;
            }

            // otherwise, we know that the file has not been
            // updated since the last check, so false is returned
            return false;
        } else {

            // in case that no fileInfo was found in fileInfoMap
            // first we create new fileInfo object for current file,
            // update its lastModified property, put it in the fileInfoMap
            // and return true to signal that a new job should be created
            FileInfo fileInfo = new FileInfo(file.getName());
            fileInfo.lastModified = lastModified;
            fileInfoMap.put(filepath, fileInfo);
            return true;
        }
    }

    private void pause() {
        sleepUntil = System.currentTimeMillis() + sleepTime;
        synchronized (this) {
            try {
                while (System.currentTimeMillis() < sleepUntil && isRunning()) {
                    wait(sleepTime);
                }
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void stop() {
        signalStop();
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Checks if the crawler is still running,
     * based on the <code>running</code> flag.
     * @return Whether the crawler is running or not.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Sets the <code>running</code> flag to true.
     */
    private void signalRun() {
        running = true;
        Logger.warn("DirectoryCrawler is now running");
    }

    /**
     * Sets the <code>running</code> flag to false.
     */
    private void signalStop() {
        running = false;
        Logger.warn("Stopping DirectoryCrawler...");
    }

    /**
     * <p>
     *     A private class that encapsulates basic
     *     information about file. Instances of this
     *     class are used as values in <code>fileInfoMap</code>.
     * </p>
     * <p>
     *     Objects of this class contain a name of the file
     *     they are associated with (immutable), and when was
     *     the corresponding file last modified (mutable).
     * </p>
     */
    private static class FileInfo {
        final String filename;
        long lastModified;

        private FileInfo(String filename) {
            this.filename = filename;
            lastModified = Long.MIN_VALUE;
        }

        @Override
        public String toString() {
            return "FileInfo{" +
                    "filename='" + filename + '\'' +
                    ", lastModified=" + lastModified +
                    '}';
        }
    }
}
