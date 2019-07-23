package com.wesley.controller;

import com.wesley.annotation.WesAutowired;
import com.wesley.annotation.WesController;
import com.wesley.annotation.WesRequestMapping;
import com.wesley.annotation.WesRequestParam;
import com.wesley.service.HelloService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WesController
@WesRequestMapping("/hello")
public class HelloController {

    @WesAutowired
    HelloService helloService;


    @WesRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @WesRequestParam("name")String name){
        String result = helloService.get(name);
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @WesRequestMapping("/add")
    public void add(HttpServletRequest request, HttpServletResponse response,
                    @WesRequestParam("first")Integer first,@WesRequestParam("second")Integer second){
        try {
            response.getWriter().write(first+"+"+second+"="+(first+second));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @WesRequestMapping("/sub")
    public void sub(HttpServletRequest request, HttpServletResponse response,
                    @WesRequestParam("first")Integer first,@WesRequestParam("second")Integer second){
        try {
            response.getWriter().write(first+"-"+second+"="+(first-second));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
