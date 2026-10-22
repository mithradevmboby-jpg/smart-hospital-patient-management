import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class Main {
    private static final PatientManager manager = new PatientManager();
    private static final Path WEB_ROOT = Paths.get(".").toAbsolutePath().normalize();

    public static void main(String[] args) throws Exception {
        // Sample patients make the dashboard easier to demonstrate.
        manager.addPatient(new Patient("P001", "Aarav Nair", 34, "9876543210", "Fever and headache"));
        manager.addPatient(new Patient("P002", "Ananya Thomas", 27, "9876501234", "Cough and cold"));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/patients", Main::handlePatients);
        server.createContext("/api/next", Main::handleNext);
        server.createContext("/api/search", Main::handleSearch);
        server.createContext("/api/complete", Main::handleComplete);
        server.createContext("/", Main::handleStatic);

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("Smart Hospital Patient Management System");
        System.out.println("Open: http://localhost:8080");
    }

    private static void handlePatients(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, String> data = parseQuery(readBody(exchange));

            String id = clean(data.get("id"));
            String name = clean(data.get("name"));
            String ageText = clean(data.get("age"));
            String phone = clean(data.get("phone"));
            String symptoms = clean(data.get("symptoms"));

            if (id.isEmpty() || name.isEmpty() || ageText.isEmpty()) {
                send(exchange, 400, "{\"error\":\"ID, name and age are required.\"}");
                return;
            }

            try {
                int age = Integer.parseInt(ageText);
                Patient patient = new Patient(id, name, age, phone, symptoms);

                if (!manager.addPatient(patient)) {
                    send(exchange, 409, "{\"error\":\"Patient ID already exists.\"}");
                    return;
                }

                send(exchange, 201, patientToJson(patient));
            } catch (NumberFormatException e) {
                send(exchange, 400, "{\"error\":\"Age must be a valid number.\"}");
            }
            return;
        }

        StringBuilder json = new StringBuilder("[");
        List<Patient> list = manager.getWaitingPatients();

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) json.append(",");
            json.append(patientToJson(list.get(i)));
        }

        json.append("]");
        send(exchange, 200, json.toString());
    }

    private static void handleNext(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        Patient patient = manager.callNextPatient();

        if (patient == null) {
            send(exchange, 404, "{\"error\":\"No patients are waiting.\"}");
            return;
        }

        send(exchange, 200, patientToJson(patient));
    }

    private static void handleSearch(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        Map<String, String> data = parseQuery(query == null ? "" : query);

        Patient patient = manager.searchPatient(data.get("id"));

        if (patient == null) {
            send(exchange, 404, "{\"error\":\"Patient not found.\"}");
            return;
        }

        send(exchange, 200, patientToJson(patient));
    }

    private static void handleComplete(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        Map<String, String> data = parseQuery(readBody(exchange));
        Patient patient = manager.completePatient(data.get("id"));

        if (patient == null) {
            send(exchange, 404, "{\"error\":\"Patient not found.\"}");
            return;
        }

        send(exchange, 200, patientToJson(patient));
    }

    private static void handleStatic(HttpExchange exchange) throws IOException {
        String requested = exchange.getRequestURI().getPath();

        if (requested.equals("/")) {
            requested = "/index.html";
        }

        // Basic path-traversal protection.
        Path file = WEB_ROOT.resolve(requested.substring(1)).normalize();

        if (!file.startsWith(WEB_ROOT) || !Files.exists(file) || Files.isDirectory(file)) {
            send(exchange, 404, "404 - File not found");
            return;
        }

        String contentType = contentType(file.toString());
        byte[] content = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream input = exchange.getRequestBody()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();

        if (query == null || query.isEmpty()) return map;

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            map.put(key, value);
        }

        return map;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String patientToJson(Patient p) {
        return "{"
                + "\"id\":\"" + jsonEscape(p.getId()) + "\","
                + "\"name\":\"" + jsonEscape(p.getName()) + "\","
                + "\"age\":" + p.getAge() + ","
                + "\"phone\":\"" + jsonEscape(p.getPhone()) + "\","
                + "\"symptoms\":\"" + jsonEscape(p.getSymptoms()) + "\""
                + "}";
    }

    private static String jsonEscape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String contentType(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);

        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";

        return "application/octet-stream";
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
