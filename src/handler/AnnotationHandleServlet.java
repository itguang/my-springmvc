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
