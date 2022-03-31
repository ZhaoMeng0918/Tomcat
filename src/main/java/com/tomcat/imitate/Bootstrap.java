package com.tomcat.imitate;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @ClassName Bootstrap
 * @Description Tomcat-Imitate启动类
 * @Author GerryZhao
 * @Date 2022-03-31 18:26
 * @Version 1.0.0
 */
public class Bootstrap {
    private static Logger logger = Logger.getLogger(Bootstrap.class);

    private static final Object daemonLock = new Object();

    static {
        String userDir = System.getProperty("user.dir");
    }

    private void init() throws IOException {
        SocketServerListenHandler listenHandler = new SocketServerListenHandler();
        listenHandler.listenClientConnect();
    }

    public static void main(String[] args) throws IOException {
        logger.info("Tomcat-Imitate启动");
        synchronized (daemonLock) {
            Bootstrap bootstrap = new Bootstrap();
            try {
                bootstrap.init();
            } catch (Throwable t) {
                logger.error("Tomcat-Imitate出现异常：", t);
            }
        }
    }
}
