package com.gupaoedu.mvcframework.v3.servlet;

import com.gupaoedu.mvcframework.annotation.GPRequestParam;
import com.sun.xml.internal.ws.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class HandlerMapping {

    private String url;
    private Method method;
    private Object controller;
    //形参类型列表
    private Class<?>[] paramTypes ;

    //形参列表，key:参数名称，value：参数位置（顺序）
    private Map<String,Integer> paramIndexMapping ;

    public HandlerMapping(String url, Method method, Object controller) {
        this.url = url;
        this.method = method;
        this.controller = controller;
        paramTypes = method.getParameterTypes();
        paramIndexMapping = new HashMap<String,Integer>();

        putParamIndexMapping(method);
    }

    /**
     *
     * @param method
     */
    private void putParamIndexMapping(Method method) {

        /**
         * 提取方法中加了注解的参数，把方法上的注解拿到，得到的是一个二维数组。
         * 因为一个参数可以有多个注解，二一个方法又有多个参数。
         */
        Annotation[][] pa = method.getParameterAnnotations();
        //外层遍历所有的参数
        for(int i=0; i < pa.length;i++){

            //里层的循环遍历一个参数上所有的注解
            for(Annotation annotation : pa[i]){
                //这里只解析的是含有GPRequestParam的参数
                if(annotation instanceof GPRequestParam){
                    //拿到参数名称，去和url  http://localhost:8080/demo/query?name=tom 匹配
                    String paramName = ((GPRequestParam) annotation).value();
                    if(!"".equals(paramName.trim())){
                        paramIndexMapping.put(paramName,i);
                    }
                }
            }

        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        for(int i = 0;i < parameterTypes.length;i++){
            Class<?> parameterType = parameterTypes[i];
            if(parameterType == HttpServletRequest.class
                    || parameterType == HttpServletResponse.class){
                paramIndexMapping.put(parameterType.getName(),i);
            }

        }
    }

    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public Object getController() {
        return controller;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public Class<?>[] getParamTypes() {
        return paramTypes;
    }
}
