package com.fhs.trans.filter;

import com.fhs.core.trans.vo.VO;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
import java.util.HashMap;

/**
 * 用来释放资源
 */
@Order(0)
@WebFilter(filterName = "releaseTransCacheFilter", urlPatterns = "/*")
public class ReleaseTransCacheFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            VO.TRANS_MAP_CACHE.set(new HashMap<>());
            chain.doFilter(request, response);
        } catch (Exception e) {
            throw e;
        } finally {
            VO.TRANS_MAP_CACHE.set(null);
        }
    }

    @Override
    public void destroy() {

    }
}
