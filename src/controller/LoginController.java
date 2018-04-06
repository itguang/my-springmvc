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
