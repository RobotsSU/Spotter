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

import java.io.Serializable;
import java.util.List;

/**
 * A request to zip up some files. Use this with {@link ZipItUpProcessor}.
 *
 * @author birmiwal@google.com (Shishir Birmiwal)
 */
@SuppressWarnings("serial")
public class ZipItUpRequest implements Serializable {
    // if 0, then write out 1 zip file,
    // else, output is split across multiple files and each output file is <=
    // maxOutputFileSize
    // the file-name is outputFile.part-XXXX
    private int maxOutputFileSize;

    private List<String> inputFiles;

    private String outputFile;

    private boolean deleteInputfiles;

    public List<String> getInputFiles() {
        return inputFiles;
    }

    public void setInputFiles(List<String> inputFiles) {
        this.inputFiles = inputFiles;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public int getMaxOutputFileSize() {
        return maxOutputFileSize;
    }

    public void setMaxOutputFileSize(int maxOutputFileSize) {
        this.maxOutputFileSize = maxOutputFileSize;
    }

    public void setDeleteInputfiles(boolean deleteInputfiles) {
        this.deleteInputfiles = deleteInputfiles;
    }

    public boolean isDeleteInputfiles() {
        return deleteInputfiles;
    }
}
