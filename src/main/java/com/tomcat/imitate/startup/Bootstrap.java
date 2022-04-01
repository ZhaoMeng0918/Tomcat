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
        logger.info("\n" +
                "___________                           __    .___        .__  __          __          \n" +
                "\\__    ___/___   _____   ____ _____ _/  |_  |   | _____ |__|/  |______ _/  |_  ____  \n" +
                "  |    | /  _ \\ /     \\_/ ___\\\\__  \\\\   __\\ |   |/     \\|  \\   __\\__  \\\\   __\\/ __ \\ \n" +
                "  |    |(  <_> )  Y Y  \\  \\___ / __ \\|  |   |   |  Y Y  \\  ||  |  / __ \\|  | \\  ___/ \n" +
                "  |____| \\____/|__|_|  /\\___  >____  /__|   |___|__|_|  /__||__| (____  /__|  \\___  >\n" +
                "                     \\/     \\/     \\/                 \\/              \\/          \\/ \n");
        logger.info("Tomcat-Imitate启动");
        SocketServerListenHandler listenHandler = new SocketServerListenHandler();
        listenHandler.listenClientConnect();
    }
}
