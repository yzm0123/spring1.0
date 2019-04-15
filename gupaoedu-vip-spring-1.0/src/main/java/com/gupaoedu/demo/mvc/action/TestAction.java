package com.gupaoedu.demo.mvc.action;


import com.gupaoedu.mvcframework.annotation.GPAutowired;
import com.gupaoedu.mvcframework.annotation.GPController;
import com.gupaoedu.mvcframework.annotation.GPService;

import java.lang.reflect.Field;

public class TestAction {

    public static void main(String[] args) {
        test1();
    }

    public static void test1(){
        try {
            Class<?> clazz = Class.forName("com.gupaoedu.demo.service.impl.DemoService");
            String simpleName= clazz.getSimpleName();
            System.out.println("simpleName:" + simpleName);
            String name = clazz.getName();
            System.out.println("name:" + name);
            GPService gpService = clazz.getAnnotation(GPService.class);
            System.out.println("gpService：" + gpService.value());

            Class<?> c1 = Class.forName("com.gupaoedu.demo.mvc.action.DemoAction");
            String demoActionSimpleName = c1.getSimpleName();
            System.out.println("demoActionSimpleName:" + demoActionSimpleName);
            GPController gpController = c1.getAnnotation(GPController.class);
            System.out.println("gpController:" + gpController.value());

            Field[] fields = c1.getDeclaredFields();
            for (Field field : fields){
                System.out.println("field: " + field);
                System.out.println("fieldType: " + field.getType());
                System.out.println("fieldTypeName: " + field.getType().getName());
                System.out.println("fieldName: " + field.getName());

                String typeName = field.getType().getName();
                Class<?> fieldClass = Class.forName(typeName);
                String beanName = fieldClass.getSimpleName();

                System.out.println("beanName:" + beanName);

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
