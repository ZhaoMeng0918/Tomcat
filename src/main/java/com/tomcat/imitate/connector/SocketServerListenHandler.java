package com.tomcat.imitate.connector;

import com.tomcat.imitate.config.Config;
import com.tomcat.imitate.startup.Bootstrap;
import com.tomcat.imitate.utils.HttpMessageParser;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName SocketServerListenHandler
 * @Description 服务器监听类，监听客户端的请求
 * @Author Administrator
 * @Date 2022-03-31 19:36
 * @Version 1.0.0
 */
public class SocketServerListenHandler {
    private static Logger logger = Logger.getLogger(Bootstrap.class);

    private ServerSocket serverSocket;

    private final String host = Config.getInstance().getHost();
    private final int port = Config.getInstance().getPort();

    private SocketServerExecutorThreadPool threadPool;

    public SocketServerListenHandler() {
        try {
            serverSocket = new ServerSocket(port);
            threadPool = new SocketServerExecutorThreadPool(30, 1000,
                    TimeUnit.MILLISECONDS, 100, ((queue, task) -> {
                queue.offer(task);
            }));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public void listenClientConnect() throws IOException {
        logger.info("启动serverSocket " + host + ":" + serverSocket.getLocalPort());
        while (true) {
            try {
                Socket clientConnectSocket = serverSocket.accept();
                threadPool.execute(new Runnable() {
                    @SneakyThrows
                    @Override
                    public void run() {
                        HttpMessageParser.Request httpRequest = HttpMessageParser.parse2request(clientConnectSocket.getInputStream());

                    }
                });
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
