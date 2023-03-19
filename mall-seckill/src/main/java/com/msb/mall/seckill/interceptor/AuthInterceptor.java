package com.msb.mall.seckill.interceptor;

import com.msb.common.constant.AuthConstant;
import com.msb.common.vo.MemberVO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 秒杀活动的拦截器 确认用户在登录的状态下操作的
 */
public class AuthInterceptor implements HandlerInterceptor {
    // 本地线程对象 Map<thread, Object>
    public static ThreadLocal<MemberVO> threadLocal = new ThreadLocal();
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 通过HttpSession获取当前登录的用户信息
        HttpSession session = request.getSession();
        Object attribute = session.getAttribute(AuthConstant.AUTH_SESSION_REDIS);
        if(attribute != null){
            MemberVO memberVO = (MemberVO) attribute;
            threadLocal.set(memberVO);
            return true;
        }
        // 如果attribute = null 说明用户没有登录，需要重定向到登录页面
        session.setAttribute(AuthConstant.AUTH_SESSION_MSG, "请先登录");
        response.sendRedirect("http://auth.msb.com/login.html");
        return false;
    }
}
