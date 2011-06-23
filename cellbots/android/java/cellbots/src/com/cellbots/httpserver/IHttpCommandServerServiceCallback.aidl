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

/**
 * A oneway interface for making callbacks to the client of the service when
 * HTTP requests are received.
 *
 * @author chaitanyag@google.com (Chaitanya Gharpure)
 */
oneway interface IHttpCommandServerServiceCallback {
    // This method is called when a PUT/POST request is made to the HTTP server.
    // @param request The name of the resource requested
    // @param keys An array of keys of all URL parameters
    // @param values An array of values of all URL parameters
    // @param data the data sent as an entity with the PUT/POST request
    void OnRequest(String request, out String[] keys, out String[] values, out byte[] data);
}