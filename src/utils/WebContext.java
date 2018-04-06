package utils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 *  WebContext主要是用来存储当前线程中的HttpServletRequest和HttpServletResponse
 *  当别的地方需要使用HttpServletRequest和HttpServletResponse，就可以通过requestHodler和responseHodler获取
 *
 * @author itguang
 * @create 2018-04-05 22:13
 **/
public class WebContext {

    //使用ThreadLocal在多线程中隔离变量
    public static ThreadLocal<HttpServletRequest> requestHodler = new ThreadLocal<HttpServletRequest>();
    public static ThreadLocal<HttpServletResponse> responseHodler = new ThreadLocal<HttpServletResponse>();

    public HttpServletRequest getRequest(){
        return requestHodler.get();
    }

    public HttpSession getSession(){
        return requestHodler.get().getSession();
    }

    public ServletContext getServletContext(){
        return requestHodler.get().getSession().getServletContext();
    }

    public HttpServletResponse getResponse(){
        return responseHodler.get();
    }
}
