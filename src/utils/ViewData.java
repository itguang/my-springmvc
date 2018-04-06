package utils;

import javax.servlet.http.HttpServletRequest;

/**
 * @author itguang
 * @create 2018-04-05 22:16
 **/
public class ViewData {

    private HttpServletRequest request;

    public ViewData() {
        initRequest();
    }

    private void initRequest(){
        //从requestHodler中获取request对象
        this.request = WebContext.requestHodler.get();
    }

    public void put(String name,Object value){
        this.request.setAttribute(name, value);
    }
}
