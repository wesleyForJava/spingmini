package com.wesley.service.impl;

import com.wesley.annotation.WesService;
import com.wesley.service.HelloService;

@WesService
public class HelloServiceImpl  implements HelloService {
    @Override
    public String get(String name) {
        return "My name is " + name;
    }


}
