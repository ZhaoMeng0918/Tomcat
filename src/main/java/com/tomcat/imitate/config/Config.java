package com.tomcat.imitate.config;

import lombok.Data;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * @ClassName Config
 * @Description 全局配置
 * @Author GerryZhao
 * @Date 2022-04-01 14:49
 * @Version 1.0.0
 */
public class Config {
    private static Logger logger = Logger.getLogger(Config.class);
    private String host;
    private int port;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    private Config() {
        String basePath = System.getProperty("user.dir") + "/src/main/resources/config.properties";
        BufferedInputStream bufferedInputStream;
        Properties properties = new Properties();
        try {
            bufferedInputStream = new BufferedInputStream(new FileInputStream(new File(basePath)));
            properties.load(bufferedInputStream);
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        host = properties.getProperty("host");
        port = Integer.parseInt(properties.getProperty("port"));
    }

    private static volatile Config INSTANCE = null;

    public static synchronized Config getInstance() {
        if (INSTANCE == null) {
            synchronized (Config.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Config();
                }
            }
        }
        return INSTANCE;
    }
}
