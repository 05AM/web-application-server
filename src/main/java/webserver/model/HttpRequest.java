package webserver.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import util.HttpRequestUtils;

public class HttpRequest {

    private String requestPath;
    private String httpMethod;
    private Map<String, String> parameters;
    private String body = null;
    private String rawRequest;


    public HttpRequest(InputStream in) throws IOException {
        parseRequest(new BufferedReader(new InputStreamReader(in)));
    }

    private void parseRequest(BufferedReader reader) throws IOException {
        List<String> requestLines = new ArrayList<>();

        // 요청 라인 파싱
        String requestLine = reader.readLine();
        requestLines.add(requestLine);
        parseRequestLine(requestLine);

        // 헤더 파싱
        parseHeaders(reader, requestLines);

        // 바디 파싱
        if ("POST".equals(httpMethod)) {
            parseBody(reader, requestLines);
        }

        // 원본 요청 저장
        this.rawRequest = buildRawRequest(requestLines);
    }

    private void parseRequestLine(String requestLine) {
        String[] tokens = requestLine.split(" ");
        this.httpMethod = tokens[0];

        String[] paths = tokens[1].split("\\?");
        this.requestPath = paths[0];
        this.parameters = (paths.length > 1) ? HttpRequestUtils.parseQueryString(paths[1]) : Collections.emptyMap();
    }

    private void parseHeaders(BufferedReader reader, List<String> requestLines) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            requestLines.add(line);
        }
    }

    private void parseBody(BufferedReader reader, List<String> requestLines) throws IOException {
        StringBuilder body = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            body.append(line).append("\n");
            requestLines.add(line);
        }

        this.body = body.toString();
    }


    private String buildRawRequest(List<String> requestLines) {
        return String.join("\n", requestLines);
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getRawRequest() {
        return rawRequest;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getBody() {
        return body;
    }
}
