/*
 * Copyright (C) 2010 Google Inc.
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

package com.cellbots.personas;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

/**
 * Content Provider for serving personas. Persona files must be stored under:
 * /sdcard/cellbot/personas/PERSONA_NAME/ Access persona files by using this
 * pattern: content://com.cellbot.localpersonas/PERSONA_NAME/FILE For example:
 * /sdcard/cellbot/personas/default/happy.png can be accessed in the WebView by
 * using: content://com.cellbot.localpersonas/default/happy.png IMPORTANT NOTE:
 * For security reasons, you are not allowed to access anything with ".." or
 * "//" in the path. This is to prevent a personas script from accessing the
 * rest of the SD card.
 *
 * @author clchen@google.com (Charles L. Chen)
 */
public class LocalPersonasServer extends ContentProvider {
    private static final String URI_PREFIX = "content://com.cellbot.localpersonas";

    public static String constructUri(String url) {
        Uri uri = Uri.parse(url);
        return uri.isAbsolute() ? url : URI_PREFIX + url;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String filename = uri.toString();
        if (filename.length() > URI_PREFIX.length()) {
            filename = filename.substring(URI_PREFIX.length() + 1);
            if ((filename.indexOf("//") != -1) || (filename.indexOf("..") != -1)) {
                return null;
            }
            filename = Environment.getExternalStorageDirectory() + "/cellbot/personas/" + filename;

            File file = new File(filename);
            ParcelFileDescriptor parcel = ParcelFileDescriptor.open(
                    file, ParcelFileDescriptor.MODE_READ_ONLY);
            return parcel;
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

}
