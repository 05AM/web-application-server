package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.DataBase;
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
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(), connection.getPort());

        try (InputStream in = connection.getInputStream();
             OutputStream out = connection.getOutputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            // 요청 역직렬화
            HttpRequest request = new HttpRequest(in);

            log.info("[현재 요청 URL] {}", request.getRequestPath());

            // 요청 정보 출력
            log.info(request.getRawRequest());

            DataOutputStream dos = new DataOutputStream(out);
            process(request, dos);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void process(HttpRequest request, DataOutputStream dos) throws IOException {
        byte[] body = null;

        String requestPath = request.getRequestPath();
        String httpMethod = request.getHttpMethod();

        switch (requestPath) {
            // 정적 파일
            case "/index.html",
                 "/user/form.html",
                 "/user/login.html",
                 "/user/login_failed.html",
                 "/css/bootstrap.min.css",
                 "/css/styles.css" -> {
                body = IOUtils.readFileAsBytes(requestPath);
                response200Header(dos, getContentType(requestPath), body.length);
                endOfHeader(dos);
                responseBody(dos, body);
            }

            // 그 외 요청
            case "/user/create" -> {
                if (httpMethod.equals("GET") || httpMethod.equals("POST")) {
                    Map<String, String> params = request.getParameters();

                    String userId = params.get("userId");
                    String password = params.get("password");
                    String name = params.get("name");
                    String email = params.get("email");

                    User user = new User(userId, password, name, email);
                    DataBase.addUser(user);
                }

                response302Header(dos, "/index.html");
                endOfHeader(dos);
            }

            case "/user/login" -> {
                Map<String, String> params = request.getParameters();

                String userId = params.get("userId");
                String password = params.get("password");

                User user = DataBase.findUserById(userId);

                if (user == null || !user.getPassword().equals(password)) {
                    response302Header(dos, "/user/login_failed.html");
                    endOfHeader(dos);
                } else {
                    response302Header(dos, "/index.html");
                    setCookie(dos, "logined=true");
                    endOfHeader(dos);
                }
            }

            case "/user/list" -> {
                Map<String, String> cookies = request.getCookies();
                String logined = cookies.get("logined");

                if (logined == null || !logined.equals("true")) {
                    response302Header(dos, "/user/login.html");
                } else {
                    Collection<User> users = DataBase.findAll();

                    StringBuilder userList = new StringBuilder();
                    for (User user : users) {
                        userList.append(user.getUserId()).append("\n");
                    }

                    body = userList.toString().getBytes();
                    response200Header(dos, "text/plain", body.length);
                    endOfHeader(dos);
                    responseBody(dos, body);
                }
            }

            default -> {
                body = "Hello World".getBytes();
                response200Header(dos, "text/html", body.length);
                endOfHeader(dos);
                responseBody(dos, body);
            }
        }

        dos.flush();
    }

    private void response200Header(DataOutputStream dos, String contentType, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: " + contentType + "\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String redirectUrl) {
        try {
            dos.writeBytes("HTTP/1.1 302 FOUND \r\n");
            dos.writeBytes("Location: " + redirectUrl + "\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private String getContentType(String filePath) {
        if (filePath.endsWith(".html")) {
            return "text/html;charset=utf-8";
        } else if (filePath.endsWith(".css")) {
            return "text/css";
        } else {
            return "application/octet-stream";
        }
    }

    private void setCookie(DataOutputStream dos, String content) {
        try {
            dos.writeBytes("Set-Cookie: " + content + "\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void endOfHeader(DataOutputStream dos) {
        try {
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        if (body == null) {
            return;
        }

        try {
            dos.write(body, 0, body.length);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}