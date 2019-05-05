package com.gupaoedu.mvcframework.v3.servlet;

import com.gupaoedu.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;

public class GPDispatcherServlet extends HttpServlet {

    //读取配置文件
    private Properties contextConfig = new Properties();

    //保存类名
    private List<String> classNameList = new ArrayList<String>();

    //IOC容器
    private Map<String,Object> ioc = new HashMap<String, Object>();

    private List<HandlerMapping> handlerMappingList = new ArrayList<HandlerMapping>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {
            //6.调用、运行阶段
            doDispatcher(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception,Detail:" + Arrays.toString(e.getStackTrace()));
        }

    }

    /**
     * 调用、运行阶段
     * @param req
     * @param resp
     */
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception{

        //这是用户请求的绝对路径，需要处理成相对路径
        String url = req.getRequestURI();

        String contextPath = req.getContextPath();
        //把前缀去掉，处理成相对路径
        url = url.replaceAll(contextPath,"").replaceAll("/+" ,"/");

        HandlerMapping handlerMapping = getHandlerMapping(req);

        if(handlerMapping == null){
            //handlerMapping如果不存在这个url，则返回404
            resp.getWriter().write("404 not found!");
            return;
        }

        Map<String, Integer> paramIndexMapping = handlerMapping.getParamIndexMapping();
        Class<?>[] paramTypes = handlerMapping.getParamTypes();

        Object[] paramValues = new Object[paramTypes.length];

        //请求url对应的参数列表
        Map<String,String[]> parameterMap = req.getParameterMap();
        for(Map.Entry<String,String[]> parameter : parameterMap.entrySet()){

            if(!paramIndexMapping.containsKey(parameter.getKey())){
                continue;
            }

            String value = Arrays.toString(parameter.getValue()).replaceAll("\\[|\\]","")
                    .replaceAll("\\s",",").replaceAll(",+",",");

            int index = paramIndexMapping.get(parameter.getKey());

            paramValues[index] = convert(paramTypes[index],value);
        }

        if(paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
            Integer reqIndex = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }

        if (paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
            Integer respIndex = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
        }

        Method method = handlerMapping.getMethod();
        Object controller = handlerMapping.getController();
        Object returnValue = method.invoke(controller,paramValues);
        if(returnValue == null || returnValue instanceof Void){
            return;
        }

        resp.getWriter().write(returnValue.toString());

    }

    /**
     * 根据参数类型，转换参数值
     * @param paramType
     * @param value
     * @return
     */
    private Object convert(Class<?> paramType, String value) {
        if(Integer.class.equals(paramType)){
            return Integer.valueOf(value);
        }else if (Double.class.equals(paramType)) {
            return Double.valueOf(value);
        }
        return value;
    }

    /**
     * 获取handlerMapping
     * @param req
     * @return
     */
    private HandlerMapping getHandlerMapping(HttpServletRequest req) {

        if(handlerMappingList.isEmpty()){
            return null;
        }

        //这是用户请求的绝对路径，需要处理成相对路径
        String url = req.getRequestURI();

        String contextPath = req.getContextPath();
        //把前缀去掉，处理成相对路径
        url = url.replaceAll(contextPath,"").replaceAll("/+" ,"/");

        for(HandlerMapping handlerMapping : handlerMappingList){
            if(handlerMapping.getUrl().equals(url)){
                return handlerMapping;
            }
        }
        return null;
    }

    /**
     * 获取参数值
     * @param paramMap
     * @param parameter
     * @return
     */
    private String getParamValue(Map<String, String[]> paramMap, Parameter parameter) {
        String value = "";
        GPRequestParam gpRequestParam = parameter.getAnnotation(GPRequestParam.class);
        String paramName = gpRequestParam.value();
        if(paramMap.containsKey(paramName)){
            for (Map.Entry<String, String[]> stringEntry : paramMap.entrySet()) {
                if(stringEntry.getKey().equals(paramName)){
                    value = Arrays.toString(stringEntry.getValue()).replaceAll("\\[|\\]","").replaceAll("\\s",",").replaceAll(",+",",");
                    break;
                }
            }
        }
        return value;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3.初始化扫描到的类，并放入IOC容器中
        doInstance();

        //4.完成依赖注入
        doAutoWired();

        //5.初始化HandlerMapping
        initHandlerMapping();

        System.out.println("GP mini Spring framework is init...");

    }

    /**
     * 初始化HandlerMapping
     */
    private void initHandlerMapping() {
        if (ioc.isEmpty()){ return; }

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Object instance = entry.getValue();
            Class<?> instanceClass = instance.getClass();
            if(!instanceClass.isAnnotationPresent(GPController.class)){
                //如果类没有注解GPController，则表示不是action，可忽略
                continue;
            }
            String baseUrl = "/";
            if (instanceClass.isAnnotationPresent(GPRequestMapping.class)){
                //如果类上有GPRequestMapping注解
                GPRequestMapping instanceRequestMapping = instanceClass.getAnnotation(GPRequestMapping.class);
                baseUrl = instanceRequestMapping.value();
            }

            //获取所有的公共方法
            Method[] methods = instanceClass.getMethods();
            for(Method method : methods){
                if (!method.isAnnotationPresent(GPRequestMapping.class)){
                    continue;
                }
                GPRequestMapping methodRequestMapping = method.getAnnotation(GPRequestMapping.class);
                //如果有多个/，则替换为一个/
                String url = (baseUrl + methodRequestMapping.value()).replaceAll("/+","/");

                HandlerMapping handlerMapping = new HandlerMapping(url,method,instance);
                handlerMappingList.add(handlerMapping);
                System.out.println("handlerMapping为:" + url + "," + method);
            }
        }
    }

    /**
     * 完成依赖注入
     */
    private void doAutoWired() {
        if (ioc.isEmpty()){ return; }

        try {
            for (Map.Entry<String,Object> entry : ioc.entrySet()){
                Object instance = entry.getValue();//bean
                //获取当前类所有的属性（包含private、protected、public等修饰的属性）
                Field[] fields = instance.getClass().getDeclaredFields();
                for (Field field : fields){
                    if(!field.isAnnotationPresent(GPAutowired.class)){
                        continue;
                    }

                    GPAutowired autowired = field.getAnnotation(GPAutowired.class);
                    /*
                     *  如果用户没有自定义的beanName,则根据属性类型注入。
                     */
                    String beanName = autowired.value().trim();
                    if("".equals(beanName)){
                        //获取属性类型名称com.gupaoedu.demo.service.IDemoService
                        beanName = field.getType().getName();
                    }
                    //如果这里能取到bean
                    Object fieldBean = ioc.get(beanName);
                    if(fieldBean == null){
                        //如果用自定义名称和类型名称（接口）没有取到bean,则需要用类名首字母小写作为beanName再取一遍
                        Class<?> fieldClass = Class.forName(beanName);
                        beanName = toLowerFirstCase(fieldClass.getSimpleName());
                        fieldBean = ioc.get(beanName);
                    }
                    field.setAccessible(true);
                    //利用反射自动给属性赋值
                    field.set(instance,fieldBean);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 初始化扫描到的类，并放入IOC容器中
     */
    private void doInstance() {
        //初始化，为DI做准备
         if(classNameList.isEmpty()){
             return;
         }

        try {
            for (String className : classNameList){
                //className 是全类名（包名+类名）
                Class<?> clazz = Class.forName(className);
                /*
                 *  哪些类需要初始化？加了注解的才要初始化
                 */
                if(clazz.isAnnotationPresent(GPController.class)){
                    Object instance = clazz.newInstance();
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    //key为首字母小写的类名
                    ioc.put(beanName,instance);
                }else if (clazz.isAnnotationPresent(GPService.class)){
                    GPService service = clazz.getAnnotation(GPService.class);
                    //1、自定义beanName
                    String beanName = service.value().trim();

                    //2、自定义的beanName为空时，取默认值
                    if("".equals(beanName)){
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                    //3、根据类型自动赋值,投机取巧方式
                    for(Class<?> i : clazz.getInterfaces()){

                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The " + i.getName() + " is exists;");
                        }
                        //key为接口名，value为对应实例
                        ioc.put(i.getName(),instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 类名首字母小写
     * @param simpleName
     * @return
     */
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 扫描相关的类
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        //scanPackage=com.gupaoedu.demo 存储的是包路径，需要转换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File classpath = new File(url.getFile());
        for(File file : classpath.listFiles()){

            if(file.isDirectory()){
                //如果是文件夹，则递归调用
                doScanner(scanPackage + "." + file.getName());
            }else {
                if(!file.getName().endsWith(".class")){
                    //不是class文件，忽略
                    continue;
                }
                String className = scanPackage + "." + file.getName().replaceAll(".class","");
                //这里存放的类名未必都是需要初始化的，只有加了注解的类需要初始化
                classNameList.add(className);

            }
        }


    }

    /**
     * 加载配置文件
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        //从类路径下找到Spring主配置文件所在的路径，并将其读取到properties对象中
        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
