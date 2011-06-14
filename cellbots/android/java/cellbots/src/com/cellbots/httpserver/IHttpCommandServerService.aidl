/**
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
 **/
package com.cellbots.httpserver;

import com.cellbots.httpserver.IHttpCommandServerServiceCallback;

/**
 * Interface for the HTTP command server service.
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
interface IHttpCommandServerService {
    // Sets the response data for a named resource as a byte array.
    // @param name the name of the resource whose value to set
    // @param data the data value of the resource
    // @param contentType The content type of the resource which is sent back
    //     in the HTTP response  
    void setResponseDataByName(in String name, in byte[] data, String contentType);

    // Sets the path of the named resource which could be the path of a file
    // on the SD card.
    // @param name the name of the resource whose value to set
    // @param resource the path to the resource file
    // @param contentType The content type of the resource which is sent back
    //     in the HTTP response  
    void setResponsePathByName(in String name, in String resource, String contentType);

    // Returns the value of the named resource as a byte array.
    // @param name the name of the resource to retrieve
    // @return the data value of the resource as a byte array
    byte[] getResponseByName(in String name);

    // Sets the root directory where to look up resource files.
    void setRoot(String root);

    // Stops the HTTP server.
    void stopServer();
}
