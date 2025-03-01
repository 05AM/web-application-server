package webserver.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import util.HttpRequestUtils;
import util.IOUtils;

public class HttpRequest {
    private String requestPath;
    private String httpMethod;
    private Map<String, String> parameters = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> cookies = new HashMap<>();
    private String body;
    private StringBuilder rawRequestBuilder = new StringBuilder();

    public HttpRequest(InputStream in) throws IOException {
        parseRequest(new BufferedReader(new InputStreamReader(in)));
    }

    private void parseRequest(BufferedReader reader) throws IOException {
        // Request Line
        String requestLine = reader.readLine();
        rawRequestBuilder.append(requestLine).append("\n");
        parseRequestLine(requestLine);

        // Header
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            rawRequestBuilder.append(line).append("\n");
            parseHeader(line);
        }

        // Cookie
        parseCookie(headers.get("Cookie"));

        // Body
        if ("POST".equals(httpMethod)) {
            parseBody(reader);
        }
    }

    private void parseRequestLine(String requestLine) {
        String[] tokens = requestLine.split(" ");
        this.httpMethod = tokens[0];

        String[] pathParts = tokens[1].split("\\?");
        this.requestPath = pathParts[0];

        if (pathParts.length > 1) {
            this.parameters = HttpRequestUtils.parseQueryString(pathParts[1]);
        }
    }

    private void parseHeader(String headerLine) {
        HttpRequestUtils.Pair header = HttpRequestUtils.parseHeader(headerLine);
        if (header != null) {
            headers.put(header.getKey(), header.getValue());
        }
    }

    private void parseCookie(String cookieHeader) {
        if (cookieHeader != null && !cookieHeader.isEmpty()) {
            for (String cookie : cookieHeader.split("; ")) {
                String[] keyValue = cookie.split("=", 2);
                this.cookies.put(keyValue[0], keyValue[1]);
            }
        }
    }

    private void parseBody(BufferedReader reader) throws IOException {
        int contentLength = Integer.parseInt(headers.get("Content-Length"));

        if (contentLength > 0) {
            this.body = IOUtils.readData(reader, contentLength);
            rawRequestBuilder.append("\n").append(body);

            if ("application/x-www-form-urlencoded".equals(headers.get("Content-Type"))) {
                parameters.putAll(HttpRequestUtils.parseQueryString(body));
            }
        }
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getCookies() {
        return cookies;
    }

    public String getBody() {
        return body;
    }

    public String getRawRequest() {
        return rawRequestBuilder.toString();
    }
}