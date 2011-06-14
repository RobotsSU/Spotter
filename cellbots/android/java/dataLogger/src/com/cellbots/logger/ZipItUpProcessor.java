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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Zips up files in a {@link ZipItUpRequest}.
 *
 * @author birmiwal@google.com (Shishir Birmiwal)
 */
public class ZipItUpProcessor {
    private static final int BUFFER_SIZE = 2048;

    private final ZipItUpRequest request;

    private byte[] buffer = new byte[BUFFER_SIZE];

    public static interface LoggingCallback {
        void logStatus(String msg, int percentageDone);
    }

    public ZipItUpProcessor(ZipItUpRequest request) {
        this.request = request;
    }

    public void zipIt(Handler handler) throws IOException {
        Log.e("zipIt", "processing file zip request - writing to " + request.getOutputFile());
        ZipOutputStream outStream = new ZipOutputStream(SplittingOutputStream.getOutputStream(
                request.getOutputFile(), request.getMaxOutputFileSize()));

        int numFilesProcessed = 0;
        for (String inputFile : request.getInputFiles()) {
            Log.e("zipIt", "reading file " + inputFile);
            updateStatus(handler, numFilesProcessed, inputFile, false);
            BufferedInputStream in = new BufferedInputStream(
                    new FileInputStream(new File(inputFile)));
            ZipEntry entry = new ZipEntry(inputFile.substring(inputFile.lastIndexOf('/') + 1));
            outStream.putNextEntry(entry);
            int numBytesRead = 0;
            while ((numBytesRead = in.read(buffer)) >= 0) {
                outStream.write(buffer, 0, numBytesRead);
            }
            in.close();
            outStream.closeEntry();
            numFilesProcessed++;
            Log.e("zipIt", "done " + inputFile);
        }
        outStream.close();
        Log.e("zipIt", "closing");

        if (request.isDeleteInputfiles()) {
            numFilesProcessed = 0;
            for (String inputFile : request.getInputFiles()) {
                updateStatus(handler, numFilesProcessed, inputFile, true);
                File file = new File(inputFile);
                file.delete();
                numFilesProcessed++;
            }
        }
        sendUpdate(handler, 100, "all done");
    }

    private void updateStatus(
            Handler handler, int numFilesProcessed, String inputFile, boolean deleteStage) {
        int percentageDone = (100 * numFilesProcessed) / request.getInputFiles().size();
        String statusMsg = (deleteStage ? "deleting " : "zipping ") + inputFile;
        sendUpdate(handler, percentageDone, statusMsg);
    }

    private void sendUpdate(Handler handler, int percentageDone, String statusMsg) {
        if (handler == null) {
            return;
        }
        Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("percentageDone", percentageDone);
        b.putString("status", statusMsg);
        msg.setData(b);
        handler.sendMessage(msg);
    }
}
