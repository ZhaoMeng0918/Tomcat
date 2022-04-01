package com.tomcat.imitate.exception;

/**
 * @ClassName WebConfigDuplicatedException
 * @Description 应用配置重复异常
 * 用户配置的Servlet的url、name和class如果重复，抛出此异常
 * @Author Administrator
 * @Date 2022-04-01 19:20
 * @Version 1.0.0
 */
public class WebConfigDuplicatedException extends Exception {
    public WebConfigDuplicatedException(String msg) {
        super(msg);
    }
}
