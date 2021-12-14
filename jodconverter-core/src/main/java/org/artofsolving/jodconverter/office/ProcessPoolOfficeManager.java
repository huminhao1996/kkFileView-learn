//
// JODConverter - Java OpenDocument Converter
// Copyright 2004-2012 Mirko Nasato and contributors
//
// JODConverter is Open Source software, you can redistribute it and/or
// modify it under either (at your option) of the following licenses
//
// 1. The GNU Lesser General Public License v3 (or later)
//    -> http://www.gnu.org/licenses/lgpl-3.0.txt
// 2. The Apache License, Version 2.0
//    -> http://www.apache.org/licenses/LICENSE-2.0.txt
//
package org.artofsolving.jodconverter.office;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.artofsolving.jodconverter.process.ProcessManager;

/**
 * 池化的 Office管理器,包含多个Office管理器
 */
class ProcessPoolOfficeManager implements OfficeManager {

    private final BlockingQueue<PooledOfficeManager> pool; //存放Office管理器的阻塞队列
    private final PooledOfficeManager[] pooledManagers;
    private final long taskQueueTimeout;

    private volatile boolean running = false;

    private final Logger logger = Logger.getLogger(ProcessPoolOfficeManager.class.getName());

    /**
     * @param officeHome
     * @param unoUrls
     * @param runAsArgs
     * @param templateProfileDir
     * @param workDir
     * @param retryTimeout
     * @param taskQueueTimeout
     * @param taskExecutionTimeout
     * @param maxTasksPerProcess
     * @param processManager
     */
    public ProcessPoolOfficeManager(File officeHome,
                                    UnoUrl[] unoUrls,
                                    String[] runAsArgs,
                                    File templateProfileDir,
                                    File workDir,
                                    long retryTimeout,
                                    long taskQueueTimeout,
                                    long taskExecutionTimeout,
                                    int maxTasksPerProcess,
                                    ProcessManager processManager) {
        this.taskQueueTimeout = taskQueueTimeout;
        pool = new ArrayBlockingQueue<PooledOfficeManager>(unoUrls.length);
        pooledManagers = new PooledOfficeManager[unoUrls.length];
        for (int i = 0; i < unoUrls.length; i++) {
            PooledOfficeManagerSettings settings = new PooledOfficeManagerSettings(unoUrls[i]);
            settings.setRunAsArgs(runAsArgs);
            settings.setTemplateProfileDir(templateProfileDir);
            settings.setWorkDir(workDir);
            settings.setOfficeHome(officeHome);
            settings.setRetryTimeout(retryTimeout);
            settings.setTaskExecutionTimeout(taskExecutionTimeout);
            settings.setMaxTasksPerProcess(maxTasksPerProcess);
            settings.setProcessManager(processManager);
            pooledManagers[i] = new PooledOfficeManager(settings);
        }
        logger.info("ProcessManager implementation is " + processManager.getClass().getSimpleName());
    }

    /**
     * 开启 OpenOffice
     *
     * @throws OfficeException
     */
    public synchronized void start() throws OfficeException {
        for (int i = 0; i < pooledManagers.length; i++) {
            pooledManagers[i].start();
            releaseManager(pooledManagers[i]);
        }
        running = true;
    }

    public void execute(OfficeTask task) throws IllegalStateException, OfficeException {
        if (!running) {
            throw new IllegalStateException("this OfficeManager is currently stopped");
        }
        PooledOfficeManager manager = null;
        try {
            // 从队列中获取OfficeManager
            manager = acquireManager();
            if (manager == null) {
                throw new OfficeException("no office manager available");
            }
            manager.execute(task);
        } finally {
            // 用完后放回队列中
            if (manager != null) {
                releaseManager(manager);
            }
        }
    }

    public synchronized void stop() throws OfficeException {
        running = false;
        logger.info("stopping");
        pool.clear();
        for (int i = 0; i < pooledManagers.length; i++) {
            pooledManagers[i].stop();
        }
        logger.info("stopped");
    }

    /**
     * 从阻塞队列中获取 OfficeManager
     * @return
     */
    private PooledOfficeManager acquireManager() {
        try {
            return pool.poll(taskQueueTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interruptedException) {
            throw new OfficeException("interrupted", interruptedException);
        }
    }

    /**
     * 将OfficeManager放入队列
     * @param manager
     */
    private void releaseManager(PooledOfficeManager manager) {
        try {
            pool.put(manager);
        } catch (InterruptedException interruptedException) {
            throw new OfficeException("interrupted", interruptedException);
        }
    }

    public boolean isRunning() {
        return running;
    }

}
