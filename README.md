# 自己动手实现一个SpringMvc

springmvc 作为javaweb 主流框架,相比大家都用过,也已经都很熟悉了.但是对springmvc如何实现有了解多少呢?

本文将带你了解并亲手实现一个springmvc框架,带你一探究竟.


在Spring MVC中，将一个普通的java类标注上Controller注解之后，再将类中的方法使用RequestMapping注解标注，那么这个普通的java类就够处理Web请求，示例代码如下：

```java
/**
 2  * 使用Controller注解标注LoginUI类
 3  */
/**
 * 使用Controller注解标注LoginUI类
 */
@Controller
public class LoginUI {
    
    //使用RequestMapping注解指明forward1方法的访问路径  
    @RequestMapping("LoginUI/Login2")
    public View forward1(){
        //执行完forward1方法之后返回的视图
        return new View("/login2.jsp");  
    }
    
    //使用RequestMapping注解指明forward2方法的访问路径  
    @RequestMapping("LoginUI/Login3")
    public View forward2(){
        //执行完forward2方法之后返回的视图
        return new View("/login3.jsp");  
    } 
}
```

然后我们只需要在web.xml文件中配置一个 Dispatcher Servlet (SpringMvc控制器) 就可以了.

![](http://ww4.sinaimg.cn/mw690/6941baebtw1epg9al8bv6j20f90aqjrx.jpg)

> 图片来源: http://www.importnew.com/15141.html

spring通过java annotation就可以注释一个类为action ，在方法上添加上一个java annotation 就可以配置请求的路径了，
那么这种机制是如何实现的呢，今天我们使用"自定义注解+Servlet"来简单模拟一下Spring MVC中的这种注解配置方式。


**首先来讲一下大体思路: 类似SpringMvc,我们也有一个叫做Dispatcher Servlet的东西,用来拦截所有请求,拿到客户端请求路径,
利用java反射技术,实例化所有带有Controller注解的类,去执行请求路径对应的Controller的方法.**


现在我们就开始动手实践吧.

## 一:编写注解

### Controller注解


开发Controller注解，这个注解只有一个value属性，默认值为空字符串，代码如下:

```java
package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义Controller注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Controller {

    public String value() default "";
}

```

### RequestMapping注解

开发RequestMapping注解，用于定义请求路径，这个注解只有一个value属性，默认值为空字符串，代码如下：

```java
package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自动以RequestMapping注解
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapping {

    public String value()default "";

}

```


**以上就是我们自定义的两个注解，注解的开发工作就算是完成了，有了注解之后，那么就必须针对注解来编写处理器，
否则我们开发的注解配置到类或者方法上面是不起作用的，这里我们使用Servlet来作为注解的处理器。**


## 二、编写核心的注解处理器

### 开发AnnotationHandleServlet

这里使用一个Servlet来作为注解处理器，编写一个AnnotationHandleServlet，代码如下：

```java
package handler;

import annotation.Controller;
import annotation.RequestMapping;
import utils.BeanUtils;
import utils.DispatchActionConstant;
import utils.RequestMapingMap;
import utils.ScanClassUtil;
import utils.View;
import utils.WebContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * 自定义注解的核心处理器,负责调用目标业务方法处理用户请求,类似于SpringMvc的DespatcherServlet
 *
 * @author itguang
 * @create 2018-04-05 21:54
 **/
public class AnnotationHandleServlet extends HttpServlet {


    /**
     * 从HttpRequest中解析出 请求路径,即 RequestMapping() 的value值.
     *
     * @param request
     * @return
     */
    private String pareRequestURI(HttpServletRequest request) {

        String path = request.getContextPath() + "/";
        String requestUri = request.getRequestURI();
        String midUrl = requestUri.replace(path, "");
        String lastUrl = midUrl.substring(0, midUrl.lastIndexOf("."));


        return lastUrl;
    }


    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException

    {
        System.out.println("AnnotationHandlerServlet-->doGet....");
        this.excute(request, response);
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        System.out.println("AnnotationHandlerServlet-->doPost....");
        this.excute(request, response);
    }

    private void excute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //1.将当前 HttpRequest 放到ThreadLocal中,方便在Controller中使用
        WebContext.requestHodler.set(request);
        //将 HttpReponse 放到ThreadLocal中,方便在Controller中使用
        WebContext.responseHodler.set(response);

        //2.解析请求的url
        String requestUrl = pareRequestURI(request);

        //3.根据 请求的url获取要使用的类
        Class<?> clazz = RequestMapingMap.getClassName(requestUrl);
        //4.创建类的实例
        Object classInstance = BeanUtils.instanceClass(clazz);

        //5.获取类中定义的方法
        Method[] methods = BeanUtils.findDeclaredMethods(clazz);

        //遍历所有方法,找出url与RequestMapping注解的value值相匹配的方法
        Method method = null;
        for (Method m : methods) {

            if (m.isAnnotationPresent(RequestMapping.class)) {
                String value = m.getAnnotation(RequestMapping.class).value();
                if (value != null && !"".equals(value.trim()) && requestUrl.equals(value.trim())) {
                    //找到要执行的目标方法
                    method = m;
                    break;
                }

            }

        }

        //6.执行url对应的方法,处理用户请求

        if (method != null) {
            Object retObject = null;
            try {
                //利用反射执行这个方法
                retObject = method.invoke(classInstance);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            //如果有返回值,就代表用户需要返回视图
            if (retObject != null) {
                View view = (View) retObject;
                //判断要使用的跳转方式
                if (view.getDispathAction().equals(DispatchActionConstant.FORWARD)) {
                    //使用服务器端跳转方式
                    request.getRequestDispatcher(view.getUrl()).forward(request, response);
                } else if (view.getDispathAction().equals(DispatchActionConstant.REDIRECT)) {
                    //使用客户端跳转方式
                    response.sendRedirect(request.getContextPath() + view.getUrl());
                } else {
                    request.getRequestDispatcher(view.getUrl()).forward(request, response);
                }
            }


        }


    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        /**
         * 重写了Servlet的init方法后一定要记得调用父类的init方法，
         * 否则在service/doGet/doPost方法中使用getServletContext()方法获取ServletContext对象时
         * 就会出现java.lang.NullPointerException异常
         */
        super.init(config);
        System.out.println("---初始化开始---");
        //获取web.xml中配置的要扫描的包
        String basePackage = config.getInitParameter("basePackage");
        //如果配置了多个包，例如：<param-value>me.gacl.web.controller,me.gacl.web.UI</param-value>
        if (basePackage.indexOf(",")>0) {
            //按逗号进行分隔
            String[] packageNameArr = basePackage.split(",");
            for (String packageName : packageNameArr) {
                initRequestMapingMap(packageName);
            }
        }else {
            initRequestMapingMap(basePackage);
        }
        System.out.println("----初始化结束---");
    }

    /**
     * @Method: initRequestMapingMap
     * @Description:添加使用了Controller注解的Class到RequestMapingMap中
     */
    private void initRequestMapingMap(String packageName){
        //得到扫描包下的class
        Set<Class<?>> setClasses =  ScanClassUtil.getClasses(packageName);
        for (Class<?> clazz :setClasses) {

            if (clazz.isAnnotationPresent(Controller.class)) {
                Method [] methods = BeanUtils.findDeclaredMethods(clazz);
                for(Method m:methods){//循环方法，找匹配的方法进行执行
                    if(m.isAnnotationPresent(RequestMapping.class)){
                        String anoPath = m.getAnnotation(RequestMapping.class).value();
                        if(anoPath!=null && !"".equals(anoPath.trim())){
                            if (RequestMapingMap.getRequesetMap().containsKey(anoPath)) {
                                throw new RuntimeException("RequestMapping映射的地址不允许重复！");
                            }
                            //把所有的映射地址存储起来  映射路径--类
                            RequestMapingMap.put(anoPath, clazz);
                        }
                    }
                }
            }
        }
    }


}

```

这里说一下AnnotationHandleServlet的实现思路:

* 1. AnnotationHandleServlet初始化(init)时扫描指定的包下面使用了Controller注解的类，如下图所示：

![1]()

* 2. 遍历类中的方法，找到类中使用了RequestMapping注解标注的那些方法，
获取RequestMapping注解的value属性值，value属性值指明了该方法的访问路径，以RequestMapping注解的value属性值作为key，
Class类作为value将存储到一个静态Map集合中。如下图所示：

![2]()

* 3. 当用户请求时(无论是get还是post请求)，会调用封装好的execute方法 ，
execute会先获取请求的url，然后解析该URL，根据解析好的URL从Map集合中取出要调用的目标类 ，
再遍历目标类中定义的所有方法，找到类中使用了RequestMapping注解的那些方法，
判断方法上面的RequestMapping注解的value属性值是否和解析出来的URL路径一致,如果一致，说明了这个就是要调用的目标方法，
此时就可以利用java反射机制先实例化目标类对象，然后再通过实例化对象调用要执行的方法处理用户请求。服务器将以下图的方式与客户端进行交互

![3]()

另外，方法处理完成之后需要给客户端发送响应信息，比如告诉客户端要跳转到哪一个页面，采用的是服务器端跳转还是客户端方式跳转，或者发送一些数据到客户端显示，那么该如何发送响应信息给客户端呢，在此，我们可以设计一个View(视图)类，对这些操作属性进行封装，其中包括跳转的路径 、展现到页面的数据、跳转方式。
这就是AnnotationHandleServlet的实现思路。


### 在Web.xml文件中注册AnnotationHandleServlet

就像使用SpringMvc一样我们也需要在web.xml中进行配置:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <servlet>
        <servlet-name>AnnotationHandleServlet</servlet-name>
        <servlet-class>handler.AnnotationHandleServlet</servlet-class>
        <init-param>
            <description>配置要扫描包及其子包, 如果有多个包,以逗号分隔</description>
            <param-name>basePackage</param-name>
            <param-value>controller</param-value>
            
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>

    <servlet-mapping>
        <servlet-name>AnnotationHandleServlet</servlet-name>
        <!-- 拦截所有以.do后缀结尾的请求 -->
        <url-pattern>*.do</url-pattern>
    </servlet-mapping>
</web-app>
```
**相关代码与工具类请参考文末给出的github地址.**

* BeanUtils.java: BeanUtils工具类主要是用来处理一些反射的操作

* RequestMapingMap.java: 该类是用于存储方法的访问路径，AnnotationHandleServlet初始化时会将类(使用Controller注解标注的那些类)中使用了RequestMapping注解标注的那些方法的访问路径存储到RequestMapingMap中。


* ScanClassUtil.java: 扫描某个包下面的类的工具类

* WebContext:

WebContext主要是用来存储当前线程中的HttpServletRequest和HttpServletResponse，
当别的地方需要使用HttpServletRequest和HttpServletResponse，就可以通过requestHodler和responseHodler获取，
通过WebContext.java这个类 ，我们可以在作为Controller的普通java类中获取当前请求的request、response或者session相关请求类的实例变量，
并且线程间互不干扰的，因为用到了ThreadLocal这个类。


* View.java: 一个视图类，对一些客户端响应操作进行封装，其中包括跳转的路径 、展现到页面的数据、跳转方式.

* ViewData.java: request范围的数据存储类，当需要发送数据到客户端显示时，就可以将要显示的数据存储到ViewData类中。使用ViewData.put(String name,Object value)方法往request对象中存数据。

* DispatchActionConstant.java: 一个跳转方式的常量类
                               

## 三, Controller注解和RequestMapping注解测试


新建一个controller包,在该包下面新建 LoginController.java,如:

```java
package controller;

import annotation.Controller;
import annotation.RequestMapping;
import utils.View;
import utils.ViewData;
import utils.WebContext;

import javax.servlet.http.HttpServletRequest;

/**
 * @author itguang
 * @create 2018-04-06 09:26
 **/
@Controller
public class LoginController {


    //使用RequestMapping注解指明forward1方法的访问路径
    @RequestMapping("login2")
    public View forward1() {


        System.out.println("login2...");

        HttpServletRequest request = WebContext.requestHodler.get();

        String username = request.getParameter("username");
        String password = request.getParameter("password");


        //执行完forward1方法之后返回的视图
        return new View("/Login2.jsp");
    }

    /**
     * 处理登录请求,接受参数
     * @return
     */
    @RequestMapping("login")
    public View login(){

        System.out.println("login...");

        //首先获取当前线程的request对象
        HttpServletRequest request = WebContext.requestHodler.get();

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        //将数据存储到ViewData中
        ViewData viewData = new ViewData();
        viewData.put("msg","欢迎你"+username);
        // 相当于
        // request.setAttribute("msg","欢迎你"+username);


        return new View("/index.jsp");
    }






}

```


index.jsp:

```jsp
<%--
  Created by IntelliJ IDEA.
  User: itguang
  Date: 2018/4/5
  Time: 15:24
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
    <title>$Title$</title>
  </head>
  <body>
  <label style="color: red;">${msg}</label>
  </body>
</html>

```

Login2.jsp

```jsp
<%@ page language="java" pageEncoding="UTF-8"%>
<!DOCTYPE HTML>
<html>
<head>
    <title>login2.jsp登录页面</title>
</head>

<body>
<fieldset>
    <legend>用户登录</legend>
    <form action="${pageContext.request.contextPath}/login.do" method="post">
        用户名：<input type="text" value="${param.usename}" name="username">
        <br/>
        密码：<input type="text" value="${param.pwd}" name="password">
        <br/>
        <input type="submit" value="登录"/>
    </form>
</fieldset>
<hr/>
<label style="color: red;">${msg}</label>
</body>
</html>
```


然后我们访问: http://localhost:8080/login2.do,注意需要以 .do 结尾,我们就会来到登录页面,
随便输入用户名密码,就会提交表单到 http://localhost:8080/login.do?username=小光光&password=123445 这个地址.

最后会转到 index.jsp页面.显示欢迎信息.

以上就是对springmvc的简单模拟.

大家可以参考下面的 gthub源码地址,或者自己导入到ide中进行断点调试,进一步理解.


---

> 本文源码地址: https://github.com/itguang/springmvc

