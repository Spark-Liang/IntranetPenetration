package org.nanospark.intranetpenetration.server;

import org.apache.commons.io.IOUtils;
import org.nanospark.intranetpenetration.exception.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

import static org.nanospark.intranetpenetration.server.PenetrationServer.PENETRATION_PREFIX;

public class ServerTask implements Runnable {
    private static Logger Logger = LoggerFactory.getLogger(ServerTask.class);

    private static final long WAIT_MICROSECONDS = 1000;

    private PenetrationServer processingServer;

    // 和本线程相关的Socket
    private Socket socket = null;


    public ServerTask(PenetrationServer server, Socket socket) {
        this.processingServer = server;
        this.socket = socket;
    }

    // 线程执行的操作，响应客户端的请求
    public void run() {
        InetAddress address = socket.getInetAddress();
        System.out.println("新连接，客户端的IP：" + address.getHostAddress() + " ,端口：" + socket.getPort());
        BufferedReader br = null;
        PrintWriter pw = null;
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            pw = new PrintWriter(socket.getOutputStream());
            String firstRow = br.readLine();
            if (firstRow.startsWith(PENETRATION_PREFIX)) {
                try {
                    processingServer.setListeningSocket(socket);
                } catch (ServerException e) {
                    Logger.info("socket fail to listen,reason is " + e, e);
                    pw.println("server has already listen by socket :" + processingServer.getListeningSocket());
                }
            } else {
                transferMessage(processingServer.getListeningSocket(), socket);
            }

        } catch (IOException e) {
            Logger.info("failed to open the stream from the socket", e);
            try {
                socket.close();
            } catch (IOException e1) {
                ServerException exception = new ServerException(e1);
                Logger.info("failed to close the socket", exception);
            }
        } finally {
            br = null;
            pw = null;
        }

    }

    public void transferMessage(Socket listeningSocket, Socket socket) {
        InputStream listeningSocket_IN = null;
        OutputStream listeningSocket_OUT = null;
        synchronized (listeningSocket) {
            try {
                listeningSocket_IN = socket.getInputStream();
                listeningSocket_OUT = socket.getOutputStream();
                try (InputStream socket_IN = socket.getInputStream();
                     OutputStream socket_OUT = socket.getOutputStream()) {
                    IOUtils.copy(socket_IN, listeningSocket_OUT);
                    while (listeningSocket_IN.available() == 0) {
                        try {
                            Thread.sleep(WAIT_MICROSECONDS);
                        } catch (InterruptedException e) {
                            Logger.info("Interrupted Waiting");
                        }
                    }
                    IOUtils.copy(listeningSocket_IN, socket_OUT);
                } catch (IOException e) {
                    Logger.info("failed to open the stream from the socket", e);
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        ServerException exception = new ServerException(e1);
                        Logger.info("failed to close the socket", exception);
                    }
                }
            } catch (IOException e) {
                Logger.info("failed to open the stream from the listening socket", e);
                try {
                    listeningSocket.close();
                } catch (IOException e1) {
                    ServerException exception = new ServerException(e1);
                    Logger.info("failed to close the listening socket", exception);
                }
            } finally {
                listeningSocket_IN = null;
                listeningSocket_OUT = null;
            }
        }
    }


    @Override
    public String toString() {
        return "ServerThread [socket=" + socket + "]";
    }
}
