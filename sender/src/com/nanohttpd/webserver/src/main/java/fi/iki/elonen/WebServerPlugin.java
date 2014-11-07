package com.nanohttpd.webserver.src.main.java.fi.iki.elonen;

import java.io.File;
import java.util.Map;

import com.nanohttpd.core.src.main.java.fi.iki.elonen.NanoHTTPD;

/**
* @author Paul S. Hawke (paul.hawke@gmail.com)
*         On: 9/14/13 at 8:09 AM
*/
public interface WebServerPlugin {

    void initialize(Map<String, String> commandLineOptions);

    boolean canServeUri(String uri, File rootDir);

    NanoHTTPD.Response serveFile(String uri, Map<String, String> headers, File file, String mimeType);
}
