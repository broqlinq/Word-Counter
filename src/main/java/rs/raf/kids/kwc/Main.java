package rs.raf.kids.kwc;

import jdk.jshell.execution.Util;
import rs.raf.kids.kwc.cli.ConsoleUI;
import rs.raf.kids.kwc.cli.Logger;
import rs.raf.kids.kwc.config.AppConfig;
import rs.raf.kids.kwc.crawler.DirectoryCrawler;
import rs.raf.kids.kwc.job.JobDispatcher;
import rs.raf.kids.kwc.job.ScanType;
import rs.raf.kids.kwc.job.ScanningJobQueue;
import rs.raf.kids.kwc.pool.FileScannerPool;
import rs.raf.kids.kwc.pool.WebScannerPool;
import rs.raf.kids.kwc.result.DefaultResultRetriever;
import rs.raf.kids.kwc.result.ResultRetriever;
import rs.raf.kids.kwc.util.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Main {

    public static final ScanningJobQueue scanningJobQueue;
    public static final DirectoryCrawler directoryCrawler;
    public static final ResultRetriever resultRetriever;
    public static final JobDispatcher jobDispatcher;
    public static final FileScannerPool fileScannerPool;
    public static final WebScannerPool webScannerPool;
    public static final ConsoleUI console;

    static {
        scanningJobQueue = new ScanningJobQueue();
        directoryCrawler = new DirectoryCrawler(scanningJobQueue, AppConfig.corpusPrefix, AppConfig.crawlerSleepTime);
        resultRetriever = new DefaultResultRetriever();
        jobDispatcher = new JobDispatcher(scanningJobQueue);
        fileScannerPool = new FileScannerPool(scanningJobQueue, resultRetriever);
        webScannerPool = new WebScannerPool(scanningJobQueue, resultRetriever);
        console = new ConsoleUI();

        // register file and web scanner pools with corresponding scan types
        jobDispatcher.registerScannerPool(ScanType.FILE, fileScannerPool);
        jobDispatcher.registerScannerPool(ScanType.WEB, webScannerPool);
    }

    public static void main(String[] args) throws InterruptedException {

        // when debugging, we want to set Logger flag to true
//        Logger.setDebug(true);

        Logger.warn("Initializing...");
        // Initialize all the thread processes
        Thread crawlerThread = new Thread(directoryCrawler);
        Thread jobDispatcherThread = new Thread(jobDispatcher);
        Thread consoleThead = new Thread(console);

        crawlerThread.setName("DirectoryCrawlerThread");
        // Job dispatcher thread is identified by name JobDispatcherThread
        jobDispatcherThread.setName("JobDispatcherThread");
        consoleThead.setName("CLI Thread");

        // and start them
        crawlerThread.start();
        jobDispatcherThread.start();
        consoleThead.start();

        // main should be running as long as
        // the ConsoleUI thread is running
        consoleThead.join();

        // after ConsoleUI thread has finished
        // we can safely signal other processes
        // to stop execution
        directoryCrawler.stop();
        resultRetriever.stop();
//        jobDispatcher.stop();

        scanningJobQueue.terminate();

        // now main thread waits for all the
        // processes to finish (wakes crawler
        // thread in case it was sleeping)
        crawlerThread.join();
        jobDispatcherThread.join();

        fileScannerPool.terminate();
        webScannerPool.terminate();

        Logger.warn("Stopping main...");
    }

    public static void printThreads() {
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        Logger.info("\n-- Thread Info --");
        for (Thread t : threadSet) {
            Logger.info("%-20s \t %-16s \t %4d \t %-10s".formatted(t.getName(), t.getState(), t.getPriority(), t.isDaemon() ? "Daemon" : "Normal"));
        }
        Logger.info("----------------");
    }
}
