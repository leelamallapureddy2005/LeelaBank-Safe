import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.*;

public class BankServer {
    static Map<String, String> accounts = new HashMap<>(); // accNo -> "name|pin|balance"
    static String FILE = "accounts.txt";

    public static void main(String[] args) throws Exception {
        loadData();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/create", BankServer::handleCreate);
        server.createContext("/api/login", BankServer::handleLogin);
        server.createContext("/api/deposit", BankServer::handleDeposit);
        server.createContext("/api/withdraw", BankServer::handleWithdraw);
        server.createContext("/api/balance", BankServer::handleBalance);

        server.setExecutor(null);
        server.start();
        System.out.println("LeelaBank-Safe Backend Running on http://localhost:8080");
    }

    static void handleCreate(HttpExchange ex) throws IOException {
        enableCORS(ex);
        if(ex.getRequestMethod().equals("OPTIONS")){ ex.sendResponseHeaders(200,-1); return; }
        String body = new String(ex.getRequestBody().readAllBytes());
        // body: accNo,name,pin
        String[] p = body.split(",");
        accounts.put(p[0].trim(), p[1].trim()+"|"+p[2].trim()+"|0");
        saveData();
        send(ex, "Account Created: "+p[0]);
    }
    static void handleLogin(HttpExchange ex) throws IOException {
        enableCORS(ex);
        if(ex.getRequestMethod().equals("OPTIONS")){ ex.sendResponseHeaders(200,-1); return; }
        String body = new String(ex.getRequestBody().readAllBytes());
        String[] p = body.split(",");
        String data = accounts.get(p[0].trim());
        if(data!=null && data.split("\\|")[1].equals(p[1].trim())) send(ex, "SUCCESS|"+data);
        else send(ex, "FAIL");
    }
    static void handleDeposit(HttpExchange ex) throws IOException {
        enableCORS(ex);
        if(ex.getRequestMethod().equals("OPTIONS")){ ex.sendResponseHeaders(200,-1); return; }
        String body = new String(ex.getRequestBody().readAllBytes());
        String[] p = body.split(",");
        String acc = p[0].trim(); double amt = Double.parseDouble(p[1].trim());
        String d = accounts.get(acc);
        String[] parts = d.split("\\|");
        double bal = Double.parseDouble(parts[2]) + amt;
        accounts.put(acc, parts[0]+"|"+parts[1]+"|"+bal);
        saveData();
        send(ex, String.valueOf(bal));
    }
    static void handleWithdraw(HttpExchange ex) throws IOException {
        enableCORS(ex);
        if(ex.getRequestMethod().equals("OPTIONS")){ ex.sendResponseHeaders(200,-1); return; }
        String body = new String(ex.getRequestBody().readAllBytes());
        String[] p = body.split(",");
        String acc = p[0].trim(); double amt = Double.parseDouble(p[1].trim());
        String d = accounts.get(acc);
        String[] parts = d.split("\\|");
        double bal = Double.parseDouble(parts[2]);
        if(bal < amt) { send(ex, "INSUFFICIENT"); return; }
        bal -= amt;
        accounts.put(acc, parts[0]+"|"+parts[1]+"|"+bal);
        saveData();
        send(ex, String.valueOf(bal));
    }
    static void handleBalance(HttpExchange ex) throws IOException {
        enableCORS(ex);
        String acc = ex.getRequestURI().getQuery().split("=")[1];
        String d = accounts.get(acc);
        send(ex, d.split("\\|")[2]);
    }

    static void send(HttpExchange ex, String res) throws IOException {
        ex.getResponseHeaders().add("Content-Type","text/plain");
        ex.sendResponseHeaders(200, res.getBytes().length);
        ex.getResponseBody().write(res.getBytes());
        ex.getResponseBody().close();
    }
    static void enableCORS(HttpExchange ex){
        ex.getResponseHeaders().add("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods","GET,POST,OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers","*");
    }
    static void loadData() throws IOException {
        if(!Files.exists(Paths.get(FILE))) return;
        for(String line: Files.readAllLines(Paths.get(FILE))){
            String[] kv = line.split("=",2);
            if(kv.length==2) accounts.put(kv[0], kv[1]);
        }
    }
    static void saveData() throws IOException {
        StringBuilder sb = new StringBuilder();
        accounts.forEach((k,v)-> sb.append(k).append("=").append(v).append("\n"));
        Files.writeString(Paths.get(FILE), sb.toString());
    }
}