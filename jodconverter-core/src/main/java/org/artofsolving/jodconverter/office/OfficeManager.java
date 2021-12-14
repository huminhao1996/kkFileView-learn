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

/**
 * office管理器,
 * An OfficeManager knows how to execute {@link OfficeTask}s.
 * <p>
 * An OfficeManager implementation will typically manage one or more
 * {@link OfficeConnection}s.
 */
public interface OfficeManager {

    /**
     * 执行
     * @param task
     * @throws OfficeException
     */
    void execute(OfficeTask task) throws OfficeException;

    /**
     * 开启
     * @throws OfficeException
     */
    void start() throws OfficeException;

    /**
     * 停止
     * @throws OfficeException
     */
    void stop() throws OfficeException;

    boolean isRunning();
}
