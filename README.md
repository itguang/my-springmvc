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

　　spring通过java annotation就可以注释一个类为action ，在方法上添加上一个java annotation 就可以配置请求的路径了，那么这种机制是如何实现的呢，今天我们使用"自定义注解+Servlet"来简单模拟一下Spring MVC中的这种注解配置方式。

