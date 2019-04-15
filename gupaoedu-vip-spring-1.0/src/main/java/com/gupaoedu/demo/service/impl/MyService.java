package com.gupaoedu.demo.service.impl;

import com.gupaoedu.mvcframework.annotation.GPService;

@GPService
public class MyService {

    public void test(){
        System.out.println("测试实现类注入的情况，应该用类名首字母小写从ioc容器中取实例");
    }
}
