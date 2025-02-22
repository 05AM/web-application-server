package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.IOUtils;
import webserver.model.HttpRequest;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream();
             OutputStream out = connection.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            // 요청 역직렬화
            HttpRequest request = new HttpRequest(in);

            // 요청 정보 출력
            log.info(request.getRawRequest());

            DataOutputStream dos = new DataOutputStream(out);
            byte[] body = process(request);

            response200Header(dos, body.length);
            responseBody(dos, body);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private byte[] process(HttpRequest request) throws IOException {
        byte[] body = null;

        String requestPath = request.getRequestPath();
        String httpMethod = request.getHttpMethod();

        switch (requestPath) {
            case "/index.html":
            case "/user/form.html":
                body = IOUtils.readFileAsBytes(requestPath);
                break;
            case "/user/create":
                if (httpMethod.equals("GET")) {
                    Map<String, String> params = request.getParameters();

                    String userId = params.get("userId");
                    String password = params.get("password");
                    String name = params.get("name");
                    String email = params.get("email");

                    User user = new User(userId, password, name, email);

                } else if (httpMethod.equals("POST")) {
                    System.out.println("[body]");
                    System.out.println(request.getBody());
                }

                body = "User Create Success".getBytes();

                break;
            default:
                body = "Hello World".getBytes();
                break;
        }

        return body;
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}