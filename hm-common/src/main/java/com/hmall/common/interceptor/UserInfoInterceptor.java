package com.hmall.common.interceptor;

import com.hmall.common.utils.UserContext;
import org.apache.catalina.User;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取登录信息
        String userInfo = request.getHeader("user-info");
        //2.判断是否获取到了用户
        if (userInfo != null && !userInfo.isEmpty()) {
            UserContext.setUser(Long.valueOf(userInfo));
        }
        //3.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        // 4.清理用户
        UserContext.removeUser();
    }
}
