package org.nanospark.intranetpenetration.server;

import org.nanospark.intranetpenetration.exception.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class PenetrationServer implements AutoCloseable {
    private static Logger Logger = LoggerFactory.getLogger(PenetrationServer.class);

    public static String PENETRATION_PREFIX = "listen";

    private Executor executor;
    private BlockingDeque<Runnable> tasks = new LinkedBlockingDeque<>();

    private int coreSize = 8;
    private int maxSize = 16;
    private long keepAliveTime = 60;
    private TimeUnit timeUnit = TimeUnit.SECONDS;

    private volatile Socket listeningSocket;

    public PenetrationServer() {
        init();
    }

    public PenetrationServer(int coreSize, int maxSize, long keepAliveTime, TimeUnit timeUnit) {
        this.coreSize = coreSize;
        this.maxSize = maxSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        init();
    }

    private void init() {
        executor = new ThreadPoolExecutor(coreSize, maxSize, keepAliveTime, timeUnit, tasks);
    }

    public void proceed(ServerTask task) {
        executor.execute(task);
    }

    public void start() {
        try {
            // 1.创建一个服务器端Socket，即ServerSocket，指定绑定的端口，并监听此端口
            ServerSocket serverSocket = new ServerSocket(8888);
            Socket socket = null;
            // 记录客户端的数量
            int count = 0;
            Logger.info("*** Penetration Server start，waiting for connection ***");
            // 循环监听等待客户端的连接
            while (true) {
                // 调用accept()方法开始监听，等待客户端的连接
                socket = serverSocket.accept();
                ServerTask serverTask = new ServerTask(this, socket);
                this.proceed(serverTask);

                count++;// 统计客户端的数量
                Logger.info("currently the amount of the client is ：" + count);
            }
        } catch (IOException e) {
            ServerException exception = new ServerException(e);
            Logger.info("failed to start the server :" + exception, exception);
        }
    }

    public void close() throws Exception {
        listeningSocket.close();
    }

    public Socket getListeningSocket() {
        return listeningSocket;
    }

    public void setListeningSocket(Socket listeningSocket) throws ServerException {
        if (getListeningSocket() == null) {
            synchronized (this) {
                if (getListeningSocket() == null) {
                    this.listeningSocket = listeningSocket;
                    return;
                }
            }
        }
        throw new ServerException("Server has already listen by socket :" + listeningSocket);
    }

    public static void main(String[] args) {
        try (PenetrationServer server = new PenetrationServer()) {
            server.start();
        } catch (Exception e) {
            Logger.info("failed to close the server :" + e, e);
        }
    }
}
