package com.msb.mall.cart.interceptor;

import com.msb.common.constant.AuthConstant;
import com.msb.common.vo.MemberVO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 自定义拦截器：帮助我们获取当前登录的用户信息 通过session共享获取的
 * 方便后续方法调用直接从threadLocal中取用户信息: MemberVO
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
        }
        return true;
    }
}
