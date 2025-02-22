package webserver.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HttpRequest {

    private String path;
    private String rawRequest;

    public HttpRequest(InputStream in) throws IOException {
        parseRequest(new BufferedReader(new InputStreamReader(in)));
    }

    private void parseRequest(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        this.path = parsePath(line);

        StringBuilder full = new StringBuilder(line).append("\n");
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            full.append(line).append("\n");
        }

        this.rawRequest = full.toString();
    }

    private String parsePath(String line) {
        String[] tokens = line.split(" ");
        return tokens[1];
    }

    public String getPath() {
        return path;
    }

    public String getRawRequest() {
        return rawRequest;
    }
}
