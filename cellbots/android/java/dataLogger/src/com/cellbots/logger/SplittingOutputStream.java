/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.cellbots.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link OutputStream} that splits the files while writing.
 *
 * @author birmiwal@google.com (Shishir Birmiwal)
 */
public class SplittingOutputStream extends OutputStream {

    private static final String OUTPUT_FILENAME_FORMAT = "%s.part-%04d";

    int fileCounter;

    private FileOutputStream fileOutputStream;

    private final String filenamePrefix;

    private final int maxFileSize;

    private int numBytesWrittenToPresentFile;

    public static OutputStream getOutputStream(String filename, int maxSize)
            throws FileNotFoundException {
        if (maxSize <= 0) {
            return new FileOutputStream(new File(filename));
        }
        return new SplittingOutputStream(filename, maxSize);
    }

    @Override
    public void write(int oneByte) throws IOException {
        swapUnderlyingFileIfRequiredToWrite(1);
        fileOutputStream.write(oneByte);
        numBytesWrittenToPresentFile += 1;
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        swapUnderlyingFileIfRequiredToWrite(buffer.length);
        fileOutputStream.write(buffer);
        numBytesWrittenToPresentFile += buffer.length;
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        swapUnderlyingFileIfRequiredToWrite(count);
        fileOutputStream.write(buffer, offset, count);
        numBytesWrittenToPresentFile += count;
    }

    @Override
    public void close() throws IOException {
        fileOutputStream.close();
    }

    @Override
    public void flush() throws IOException {
        fileOutputStream.flush();
    }

    private SplittingOutputStream(String filenamePrefix, int maxFileSize)
            throws FileNotFoundException {
        this.filenamePrefix = filenamePrefix;
        this.maxFileSize = maxFileSize;
        openNextFileForOutput();
    }

    private void openNextFileForOutput() throws FileNotFoundException {
        fileOutputStream = new FileOutputStream(getNextFilename());
        numBytesWrittenToPresentFile = 0;
    }

    private String getNextFilename() {
        return String.format(OUTPUT_FILENAME_FORMAT, filenamePrefix, fileCounter++);
    }

    private void swapUnderlyingFileIfRequiredToWrite(int numBytes) throws IOException {
        // TODO(birmiwal): To get exact maxFileSize bytes, write
        // (maxFileSize-(numBytesWrittenToPresentFile + numBytes)) bytes.
        // then write remaining bytes to the next file.
        if (numBytesWrittenToPresentFile + numBytes <= maxFileSize) {
            return;
        }

        // swap file
        fileOutputStream.flush();
        fileOutputStream.close();
        openNextFileForOutput();
    }
}
