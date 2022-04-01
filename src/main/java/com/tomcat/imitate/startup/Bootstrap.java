package com.tomcat.imitate.startup;

import com.tomcat.imitate.connector.SocketServerListenHandler;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * @ClassName Bootstrap
 * @Description Tomcat-Imitate启动类
 * @Author GerryZhao
 * @Date 2022-03-31 18:26
 * @Version 1.0.0
 */
public class Bootstrap {
    private static Logger logger = Logger.getLogger(Bootstrap.class);

    public static void main(String[] args) throws IOException {
        logger.info("Tomcat-Imitate启动");
        SocketServerListenHandler listenHandler = new SocketServerListenHandler();
        listenHandler.listenClientConnect();
    }
}
