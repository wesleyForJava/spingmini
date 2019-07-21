package com.servlet;

import com.wesley.annotation.WesAutowired;
import com.wesley.annotation.WesController;
import com.wesley.annotation.WesRequestMapping;
import com.wesley.annotation.WesService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class WesDispatcherServlet extends HttpServlet {

   private  Properties contextCofig=new Properties();
   //保存所有扫描到的类名
    private List<String> classNames=new ArrayList<>();
    //IOC容器
    private  Map<String,Object> ioc=new HashMap<String,Object>();

    //HandlerMapping容器 保存url和Mehtod的对应关系
    private Map<String,Method> handlerMappings=new HashMap<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6.调用运行阶段
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception..."+Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        //绝对路径
        String url = req.getRequestURI();
        //处理成相对路径
        String contextPath = req.getContextPath();

        url= url.replaceAll(contextPath,"").replaceAll("/+","/");

        if(!this.handlerMappings.containsKey(url)){
            try {
                resp.getWriter().write("404 找到路径...");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Method method = this.handlerMappings.get(url);
        //投机取巧的方式
        //通过放射拿到method所在的class，拿到class后还是拿到class的名称
        //用toLowerFristCase获得beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());

        Map<String, String[]> parameterMap = req.getParameterMap();
        try {
            method.invoke(ioc.get(beanName),req,resp,parameterMap.get("name")[0]);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }

    //初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {
        String contextConfigLocation = config.getInitParameter("contextConfigLocation");
        //1.加载配置文件
        doloadConfigration(contextConfigLocation);
        //2.扫描相关的类
        doscanner(contextCofig.getProperty("scanPackage"));
        //3.初始化所有相关的实例并且放入IOC容器中
        doInstance();
        //4.完成依赖注入
        doAutowired();
        // 5.初始化HandlerMapping
      initHandlerMapping();

        System.out.println("WES framework is init...");



    }

    private void doAutowired() {
        if(ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields) {
                if(!field.isAnnotationPresent(WesAutowired.class)) continue;
                WesAutowired wesAutowired = field.getAnnotation(WesAutowired.class);
                //如果用户没有自定义beanName，默认就根据类型注入
                //TODO 这个地方省去了对类名首字母小写的判断  有时间可以完成一下
                String beanName = wesAutowired.value().trim();
                 if("".equals(beanName)){
                     //获得的接口的类型作为key 待会拿这个key去ioc容器去取值
                     beanName = field.getType().getName();
                 }
                 field.setAccessible(true);

                try {
                    //用反射机制动态给字段赋值
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }


    }

    //初始化url和Method一对一的映射关系
    private void initHandlerMapping() {
        if(ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(WesController.class)){continue;}

            //拿到类上面请求路径
            String baseUrl="";
          if(clazz.isAnnotationPresent(WesRequestMapping.class)){
              WesRequestMapping wesRequestMapping = clazz.getAnnotation(WesRequestMapping.class);
              baseUrl = wesRequestMapping.value();
          }
          //默认获取所有的public方法
            for (Method method : clazz.getMethods()) {
               if(!method.isAnnotationPresent(WesRequestMapping.class)){ continue;}
                WesRequestMapping wesRequestMapping = method.getAnnotation(WesRequestMapping.class);
                String url =(baseUrl+"/"+wesRequestMapping.value())
                        .replaceAll("/+","/");
                handlerMappings.put(url,method);
                System.out.println("Mapped"+url+":"+method);
            }


        }


    }

    private void doInstance() {

        //初始化为DI做准备
        if(classNames.isEmpty()) return;
        try {
        for (String className:classNames) {
                Class<?> clazz = Class.forName(className);
             //什么样的类才需要初始化呢？
            //加了注解的才初始化
            //component暂时省略...
             if(clazz.isAnnotationPresent(WesController.class)){
                 try {
                     Object instance = clazz.newInstance();
                     //spring默认类名首字母小写
                     String beanName = toLowerFirstCase(clazz.getSimpleName());

                     ioc.put(beanName,instance);

                 } catch (InstantiationException e) {
                     e.printStackTrace();
                 } catch (IllegalAccessException e) {
                     e.printStackTrace();
                 }
             }else if (clazz.isAnnotationPresent(WesService.class)){
                 //1.默认类名首字母小写
                 //2.自定义的beanName
                 WesService wesService = clazz.getAnnotation(WesService.class);
                 String beanName =wesService.value();
                 if(beanName.equals("")){
                     beanName= toLowerFirstCase(clazz.getSimpleName());
                 }
                 //3.根据类型自动赋值
                 Object instance = clazz.newInstance();
                 ioc.put(beanName,instance);
                 for (Class impl :clazz.getInterfaces()) {
                     if(ioc.containsKey(impl.getName())){
                         throw new RuntimeException(impl.getName()+"is exists...");
                     }
                     ioc.put(impl.getName(),instance);
                 }

             }else{
                 continue;
             }

             }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }


    }

    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }

    /**
     * 扫描出相关的类
     * @param scanPackage
     */
    private void doscanner(String scanPackage) {
        //存储的是包路径把.替换为/文件路径
        //classpath
        URL url=this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
     //转换为文件
        File classpath=new File(url.getFile());
        for (File files : classpath.listFiles()) {
           if(files.isDirectory()){
               doscanner(scanPackage+"."+files.getName());
           }else{
               if(!files.getName().endsWith(".class")){continue;}
               String className=(scanPackage+"."+files.getName()).replaceAll(".class","");
               classNames.add(className);
           }
        }


    }

    /**
     * 加载配置文件
     * @param contextconfigLocation
     */
    private void doloadConfigration(String contextconfigLocation) {
        //直接从类路径下找到spring配置文件所在的路径
        //并且读取出来放到Properties对象中
        InputStream fis = this.getClass().getClassLoader().getResourceAsStream(contextconfigLocation);
        try {
            contextCofig.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != fis){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
