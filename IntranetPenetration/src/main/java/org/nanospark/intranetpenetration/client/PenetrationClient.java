package org.nanospark.intranetpenetration.client;

import org.nanospark.intranetpenetration.server.PenetrationServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class PenetrationClient implements AutoCloseable {
    private static Logger Logger = LoggerFactory.getLogger(PenetrationClient.class);

    private String hostname;
    private int port;

    private Socket socket;

    public PenetrationClient(String hostname, int port) throws IOException {
        this.hostname = hostname;
        this.port = port;
        socket = new Socket();
        socket.setReuseAddress(true);
        socket.connect(new InetSocketAddress(hostname, port));
    }

    public void listen() throws IOException {
        PrintWriter writer = new PrintWriter(socket.getOutputStream());
        writer.println(PenetrationServer.PENETRATION_PREFIX);
        writer.flush();
    }

    @Override
    public void close() throws Exception {
        socket.close();
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public static void main(String[] args) {
        String hostname = "47.106.230.145";
        int port = Integer.valueOf(args[0]);
        Logger.info("start listening");
        try (PenetrationClient client = new PenetrationClient(hostname, port)) {
            client.listen();
            Socket socket = client.getSocket();
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                String line = br.readLine();
                while (line == null) {
                    try {
                        Thread.sleep(1000);
                        line = br.readLine();
                    } catch (InterruptedException e) {
                        Logger.info("client is interrupted");
                    }
                }
                Logger.info("receive line : " + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
