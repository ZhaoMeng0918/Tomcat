package com.tomcat.imitate.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import com.tomcat.imitate.classloader.WebappClassLoader;
import com.tomcat.imitate.exception.WebConfigDuplicatedException;
import com.tomcat.imitate.http.ApplicationContext;
import com.tomcat.imitate.http.StandardFilterConfig;
import com.tomcat.imitate.utils.ContextXMLUtil;
import com.tomcat.imitate.http.StandardServletConfig;
import com.tomcat.imitate.watcher.ContextFileChangeWatcher;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName Context
 * @Description 应用上下文对象, 一个Context对应一个应用
 * @Author GerryZhao
 * @Date 2022-04-01 16:44
 * @Version 1.0.0
 */
public class Context {
    // 应用path
    private String path;

    // 应用在磁盘上的位置
    private String docBase;

    // 热部署需要用到
    private Host host;

    // 是否可重复加载
    private boolean reloadable;

    // 配置文件: web.xml
    private File contextWebXmlFile;

    // 每个web应用独立的类加载器
    private WebappClassLoader webappClassLoader;

    // 热部署监听器
    private ContextFileChangeWatcher contextFileChangeWatcher;

    // 存储上下文对象, 即applicationContext, 里面存储Attribute和context对象
    private ServletContext servletContext;

    // Servlet相关的类 ------------------------------------------------
    // 存储Servlet单例
    private Map<Class<?>, HttpServlet> servletPool;

    // 地址对应Servlet的类名
    private Map<String, String> url2servletClassName;

    // 地址对应Servlet的名称
    private Map<String, String> url2servletName;

    // Servlet的名称对应类名
    private Map<String, String> servletName2className;

    // Servlet类名对应的名称
    private Map<String, String> className2servletName;

    // Servlet的类名对应的初始化参数
    private Map<String, Map<String, String>> servletClassNameInitParams;
    // --------------------------------------------------------------

    // Filter相关的类 ------------------------------------------------
    // 存储Filter单例
    private Map<String, Filter> filterPool;

    // Servlet自启动使用
    private List<String> loadOnStartupServletClassNames;

    // URL对应的Filter的类名
    private Map<String, List<String>> url2filterClassName;

    // URL对应的Filter的名称
    private Map<String, List<String>> url2filterNames;

    // Filter的名称对应类名
    private Map<String, String> filterName2className;

    // Filter的类名对应名称
    private Map<String, String> className2filterName;

    // Filter的类名对应的初始化参数
    private Map<String, Map<String, String>> filterClassNameInitParams;
    // --------------------------------------------------------------

    // Listener集合
    private List<ServletContextListener> listeners;

    public Context(String path, String docBase, Host host, boolean reloadable) {
        this.path = path;
        this.docBase = docBase;
        this.host = host;
        this.reloadable = reloadable;
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);
        this.loadOnStartupServletClassNames = new ArrayList<>();
        this.servletContext = new ApplicationContext(this);

        this.servletPool = new ConcurrentHashMap<>();
        this.url2servletClassName = new HashMap<>();
        this.url2servletName = new HashMap<>();
        this.servletName2className = new HashMap<>();
        this.className2servletName = new HashMap<>();
        this.servletClassNameInitParams = new HashMap<>();

        this.filterPool = new ConcurrentHashMap<>();
        this.url2filterClassName = new HashMap<>();
        this.url2filterNames = new HashMap<>();
        this.filterName2className = new HashMap<>();
        this.className2filterName = new HashMap<>();
        this.filterClassNameInitParams = new HashMap<>();

        this.listeners = new ArrayList<>();

        TimeInterval timeInterval = DateUtil.timer();
        LogFactory.get().info("Deploying web application directory {} {}", this.docBase);
        deploy();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms\r\n",
                this.docBase, timeInterval.intervalMs());
    }

    /**
     * 发布加载应用
     */
    private void deploy() {
        // 加载监听器
        loadListeners();
        // 初始化
        init();
        // 如果允许重复加载的话, 就开启文件监听, 实现热部署
        if (reloadable) {
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }

        /**
         * 这里进行了JspRuntimeContext的初始化,
         * 是为了在jsp所转换的java文件里的javax.sevlet.isp.JspFactory.getDefaulFactory()这行能够有返回值
         */
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);
    }

    /**
     * 初始化应用
     */
    private void init() {
        if (contextWebXmlFile.exists()) {
            try {
                checkDuplicated();
            } catch (WebConfigDuplicatedException e) {
                e.printStackTrace();
                return;
            }

            String xml = FileUtil.readUtf8String(this.contextWebXmlFile);
            Document document = Jsoup.parse(xml);

            // 解析Servlet映射
            parseServletMapping(document);

            // 解析Servlet初始化参数
            // 解析之后并不是立即使用, 而是等到实际初始化Servlet实例的时候才取出来使用
            parseServletInitParams(document);

            // 解析Filter映射
            parseFilterMapping(document);

            // 解析Filter初始化参数
            parseFilterInitParams(document);

            // 初始化Filter
            initFilter();

            // 解析需要在启动时就实例化的Servlet类
            parseLoadOnStartup(document);

            //
            handleLoadOnStartup();

            // 监听事件
            fireEvent("init");
        }
    }

    /**
     * 停止应用
     */
    private void stop() {
        // 先关掉类加载器
        webappClassLoader.stop();
        // 关掉监听器
        contextFileChangeWatcher.stop();
        // 销毁Servlet, 调用已经加载的Servlet的destroy()方法
        destroyServlets();
        // 处理destroy事件, 通知监听器做处理
        fireEvent("destroy");
    }

    /**
     * 重新加载，热部署使用
     */
    public void reload() {
        this.host.reload(this);
    }

    /**
     * 检查Servlet配置是否符合规范
     */
    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document document = Jsoup.parse(xml);
        checkDuplicated(document, "servlet-mapping url-pattern", "Servlet URLS duplicate, please keep them unique :{} ");
        checkDuplicated(document, "servlet servlet-name", "Servlet names duplicate, keep them unique :{} ");
        checkDuplicated(document, "servlet servlet-class", "Servlet class names duplicate, keep them unique :{} ");
    }

    private void checkDuplicated(Document document, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = document.select(mapping);
        List<String> contents = new ArrayList<>();
        for (Element element : elements) {
            contents.add(element.text());
        }
        Collections.sort(contents);
        for (int i = 0; i < contents.size(); i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }
    }

    /**
     * 解析Servlet映射
     * 初始化4个Map:
     * url2servletClassName
     * url2servletName
     * servletName2className
     * className2servletName
     *
     * @param document
     */
    private void parseServletMapping(Document document) {
        Elements mappingurlElements = document.select("servlet-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String servletName = mappingurlElement.parent().select("servlet-name").first().text();
            url2servletName.put(urlPattern, servletName);
        }

        Elements servletNameElements = document.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").first().text();
            servletName2className.put(servletName, servletClass);
            className2servletName.put(servletClass, servletName);
        }

        Set<String> urls = url2servletName.keySet();
        for (String url : urls) {
            String servletName = url2servletName.get(url);
            String servletClassName = servletName2className.get(servletName);
            url2servletClassName.put(url, servletClassName);
        }
    }

    public String getServletClassName(String uri) {
        return url2servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public String getDocBase() {
        return docBase;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    /**
     * 获取Servlet
     * 线程安全的单例模式【优化】
     *
     * @param clazz
     * @return
     */
    public HttpServlet getServlet(Class<?> clazz) throws IllegalAccessException, InstantiationException, ServletException {
        HttpServlet servlet = servletPool.get(clazz);
        if (servlet == null) {
            synchronized (this) {
                servlet = servletPool.get(clazz);
                if (servlet == null) {
                    servlet = (HttpServlet) clazz.newInstance();
                    ServletContext servletContext = getServletContext();
                    String className = clazz.getName();
                    String servletName = className2servletName.get(className);
                    Map<String, String> initParameters = servletClassNameInitParams.get(className);
                    ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);
                    servlet.init();
                    servletPool.put(clazz, servlet);
                }
            }
        }
        return servlet;
    }

    /**
     * 解析Servlet初始化参数
     *
     * @param document
     */
    private void parseServletInitParams(Document document) {
        Elements servletClassNameElements = document.select("servlet-class");
        for (Element servletClassNameElement : servletClassNameElements) {
            String servletClassName = servletClassNameElement.text();
            Elements initElements = servletClassNameElement.parent().select("init-param");
            if (initElements.isEmpty()) {
                continue;
            }
            Map<String, String> initParams = new HashMap<>();
            for (Element element : initElements) {
                String name = (element.select("param-name").get(0)).text();
                String value = (element.select("param-value").get(0)).text();
                initParams.put(name, value);
            }
            this.servletClassNameInitParams.put(servletClassName, initParams);
        }
    }

    private void destroyServlets() {
        Collection<HttpServlet> servlets = this.servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    public void parseLoadOnStartup(Document document) {
        Elements elements = document.select("load-on-startup");
        for (Element element : elements) {
            String loadOnStartupServletClassName = element.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }

    public void handleLoadOnStartup() {
        for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                this.getServlet(clazz);
            } catch (InstantiationException | IllegalAccessException | ServletException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public WebappClassLoader getWebClassLoader() {
        return this.webappClassLoader;
    }

    /**
     * 解析Filter映射
     *
     * @param document
     */
    public void parseFilterMapping(Document document) {
        Elements mappingUrlElements = document.select("filter-mapping url-pattern");
        for (Element mappingUrlElement : mappingUrlElements) {
            String urlPattern = mappingUrlElement.text();
            String filterName = mappingUrlElement.parent().select("filter-name").first().text();
            List<String> filterNames = url2filterNames.get(urlPattern);
            if (filterNames == null) {
                filterNames = new ArrayList<>();
                this.url2filterNames.put(urlPattern, filterNames);
            }
            filterNames.add(filterName);
        }

        Elements filterNameElements = document.select("filter filter-name");
        for (Element filterNameElement : filterNameElements) {
            String filterName = filterNameElement.text();
            String fiterClass = filterNameElement.parent().select("filter-class").first().text();
            filterName2className.put(filterName, fiterClass);
            className2filterName.put(fiterClass, filterName);
        }

        Set<String> urls = url2filterNames.keySet();
        for (String url : urls) {
            List<String> filterNames = url2filterNames.get(url);
            if (null == filterNames) {
                filterNames = new ArrayList<>();
                url2filterNames.put(url, filterNames);
            }
            for (String filterName : filterNames) {
                String filterClassName = filterName2className.get(filterName);
                List<String> filterClassNames = url2filterClassName.get(url);
                if (null == filterClassNames) {
                    filterClassNames = new ArrayList<>();
                    url2filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }

    /**
     * 解析Filter初始化参数, 存入filterClassNameInitParams
     *
     * @param document
     */
    private void parseFilterInitParams(Document document) {
        Elements filterClassNameElements = document.select("filter-class");
        for (Element filterClassNameElement : filterClassNameElements) {
            String filterClassName = filterClassNameElement.text();
            Elements initElements = filterClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;
            Map<String, String> initParams = new HashMap<>();
            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }
            filterClassNameInitParams.put(filterClassName, initParams);
        }
    }

    /**
     * 初始化Filter
     */
    private void initFilter() {
        Set<String> classNames = this.className2filterName.keySet();
        for (String className : classNames) {
            try {
                Class<?> clazz = getWebClassLoader().loadClass(className);
                System.out.println("clazz " + clazz);
                System.out.println(className);
                Map<String, String> initParameters = filterClassNameInitParams.get(className);
                String filterName = className2filterName.get(className);
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, filterName, initParameters);
                Filter filter = filterPool.get(className);
                if (null == filter) {
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(className, filter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 根据uri获取匹配到的Filter
     *
     * @param uri
     * @return
     */
    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> filters = new ArrayList<>();
        Set<String> patterns = url2filterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();
        for (String pattern : patterns) {
            if (this.match(pattern, uri)) {
                matchedPatterns.add(pattern);
            }
        }
        Set<String> matchedFilterClassNames = new HashSet<>();
        for (String pattern : matchedPatterns) {
            List<String> filterClassName = url2filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
        for (String filterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }
        return filters;
    }

    /**
     * 判断uri和模式是否匹配
     *
     * @param pattern
     * @param uri
     * @return
     */
    private boolean match(String pattern, String uri) {
        // 完全匹配
        if (StrUtil.equals(pattern, uri)) {
            return true;
        }
        // /*模式
        else if (StrUtil.equals(pattern, "/*")) {
            return true;
        }
        // 后缀名模式
        else if (StrUtil.startWith(pattern, "/*.")) {
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri, '.', false);
            return StrUtil.equals(patternExtName, uriExtName);
        }
        // 其他匹配模式暂未考虑
        return false;
    }

    public void addListener(ServletContextListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ServletContextListener listener) {
        this.listeners.remove(listener);
    }

    private void loadListeners() {
        try {
            if (contextWebXmlFile.exists()) {
                String xml = FileUtil.readUtf8String(contextWebXmlFile);
                Document d = Jsoup.parse(xml);
                Elements es = d.select("listener listener-class");
                for (Element e : es) {
                    String listenerClassName = e.text();
                    Class<?> clazz = this.getWebClassLoader().loadClass(listenerClassName);
                    ServletContextListener listener = (ServletContextListener) clazz.newInstance();
                    addListener(listener);
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IORuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void fireEvent(String type) {
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener servletContextListener : listeners) {
            if ("init".equals(type)) {
                servletContextListener.contextInitialized(event);
            }
            if ("destroy".equals(type)) {
                servletContextListener.contextDestroyed(event);
            }
        }
    }
}
