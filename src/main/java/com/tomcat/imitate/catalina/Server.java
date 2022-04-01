package com.tomcat.imitate.catalina;

/**
 * @ClassName Server
 * @Description 一个Tomcat-Imitate对应一个Server
 * @Author GerryZhao
 * @Date 2022-04-01 16:50
 * @Version 1.0.0
 */
public class Server {

    private Service service;

    public Server() {
        this.service = new Service();
    }
}
