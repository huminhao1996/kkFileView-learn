//
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * 单个 Office管理器
 */
class PooledOfficeManager implements OfficeManager {

    private final PooledOfficeManagerSettings settings; //设置
    private final ManagedOfficeProcess managedOfficeProcess; //office进程操作类, 对OfficeProcess的方法进行封装
    private final SuspendableThreadPoolExecutor taskExecutor; // 可暂停的线程池

    private volatile boolean stopping = false;
    private int taskCount;
    private Future<?> currentTask;

    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Office连接监听
     */
    private OfficeConnectionEventListener connectionEventListener = new OfficeConnectionEventListener() {
        public void connected(OfficeConnectionEvent event) {
            taskCount = 0;
            taskExecutor.setAvailable(true);
        }
        public void disconnected(OfficeConnectionEvent event) {
            taskExecutor.setAvailable(false);
            if (stopping) {
                // expected
                stopping = false;
            } else {
                logger.warning("connection lost unexpectedly; attempting restart");
                if (currentTask != null) {
                    currentTask.cancel(true);
                }
                managedOfficeProcess.restartDueToLostConnection();
            }
        }
    };

    public PooledOfficeManager(UnoUrl unoUrl) {
        this(new PooledOfficeManagerSettings(unoUrl));
    }

    public PooledOfficeManager(PooledOfficeManagerSettings settings) {
        this.settings = settings;
        managedOfficeProcess = new ManagedOfficeProcess(settings);
        managedOfficeProcess.getConnection().addConnectionEventListener(connectionEventListener);
        taskExecutor = new SuspendableThreadPoolExecutor(new NamedThreadFactory("OfficeTaskThread"));
    }

    /**
     * 核心方法: 执行任务
     * @param task
     * @throws OfficeException
     */
    public void execute(final OfficeTask task) throws OfficeException {
        Future<?> futureTask = taskExecutor.submit(new Runnable() {
            public void run() {
                if (settings.getMaxTasksPerProcess() > 0 && ++taskCount == settings.getMaxTasksPerProcess() + 1) {
                    logger.info(String.format("reached limit of %d maxTasksPerProcess: restarting", settings.getMaxTasksPerProcess()));
                    taskExecutor.setAvailable(false);
                    stopping = true;
                    managedOfficeProcess.restartAndWait();
                    //FIXME taskCount will be 0 rather than 1 at this point
                }
                task.execute(managedOfficeProcess.getConnection()); // 执行任务 传入office连接
            }
         });
         currentTask = futureTask;
         try {
             futureTask.get(settings.getTaskExecutionTimeout(), TimeUnit.MILLISECONDS);
         } catch (TimeoutException timeoutException) {
             // 任务超时异常
             managedOfficeProcess.restartDueToTaskTimeout();
             throw new OfficeException("task did not complete within timeout", timeoutException);
         } catch (ExecutionException executionException) {
             if (executionException.getCause() instanceof OfficeException) {
                 throw (OfficeException) executionException.getCause();
             } else {
                 throw new OfficeException("task failed", executionException.getCause());
             }
         } catch (Exception exception) {
             throw new OfficeException("task failed", exception);
         }
    }

    public void start() throws OfficeException {
        managedOfficeProcess.startAndWait();
    }

    public void stop() throws OfficeException {
        taskExecutor.setAvailable(false);
        stopping = true;
        taskExecutor.shutdownNow();
        managedOfficeProcess.stopAndWait();
    }

	public boolean isRunning() {
		return managedOfficeProcess.isConnected();
	}

}
