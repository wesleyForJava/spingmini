# spirngmini
### 仿写springMvc简化版
已实现功能为主，部分的文件有修改。

理解SpringIOC、DI、MVC的基本执行原理。

IOC
IOC是Inversion of Control的缩写,IOC中最基本的技术就是“反射(Reflection)”编程。利用依赖关系注入的方式，实现对象之间的解耦。同时IOC也是一个单例的Map

DI
IOC的别名：依赖注入（Dependency Injection）。依赖注入（DI）和控制反转（IOC）是从不同的角度的描述的同一件事情，指就是通过引入IOC容器，利用依赖关系注入的方式，实现对象之间的解耦
```javascript
MVC
1、配置web.xml
2、设定init-param
3、设定url-pattern
4、设置Annotation
5、调用init()方法
6、IOC容器初始化
7、扫描添加了注解的相关类
8、创建实例化并保存至容器
9、进行DI操作
10、初始化HandlerMapping
11、调用doPost()/doGet()
12、匹配HandlerMapping
13、反射调用method.invoker()
13、response.getWriter().write()
```
