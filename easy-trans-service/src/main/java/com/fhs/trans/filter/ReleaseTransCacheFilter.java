package com.fhs.trans.filter;

import com.fhs.core.trans.vo.VO;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 用来释放资源
 */
@Order(0)
@WebFilter(filterName = "releaseTransCacheFilter", urlPatterns = "/*",asyncSupported=true)
public class ReleaseTransCacheFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            Cache<String, Map<String, String>> cache = CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).build();
            VO.TRANS_MAP_CACHE.set(cache.asMap());
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
