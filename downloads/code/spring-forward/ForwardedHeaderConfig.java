// Copyright Simon Kitching 2017. Available under the Apache License 2.0
package net.vonos.spring.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Spring context configuration class which adds a servlet-filter to the servlet-engine filter chain in order to add http-header X-Forwarded-Port
 * when needed.
 * <p>
 * When an incoming HTTPS connection is intercepted by an external loadbalancer (or similar), the https connection terminated there, and the
 * request forwarded to this app as HTTP, then the intercepting proxy should set headers X-Forwarded-Proto=https and X-Forwarded-Port=443. Various
 * code may depend on these headers - and in particular, checks done by the spring CorsFilter class does (indirectly via UriComponentBuilder).
 * Sadly some proxies (including Google IAP and Traefik) set X-Forwarded-Proto only, thus causing problems with CorsFilter and potentially other code.
 * This filter "patches" the incorrect request headers to add the missing header that the proxy _should_ have set.
 * </p>
 * <p>
 * See https://jira.spring.io/browse/SPR-16262 for more information on the spring-cors problem caused by the missing header.
 * </p>
 */
@Configuration
public class ForwardedHeaderConfig {
    /**
     * A servlet-engine has an ordered list of Filter instances to apply to a request. However spring has invented type FilterChainProxy which looks
     * to the servlet-engine like a single filter but internally is a list of filters to apply. Spring-security creates a single bean of name
     * springSecurityFilterChain (type FilterChainProxy) which contains all the security-module-relevant filter objects. That chain object
     * has an associated "order" value of -100 (where lower values are registered first in the "real" servlet-engine filter list). In order for
     * the AddForwardPortFilter filter to be executed _before_ the CorsFilter (which is part of the securityFilterChain), it must be registered
     * with an order less than -100.
     */
    private static final int ORDER_BEFORE_SECURITY = -200;

    /**
     * Register a filter that adds in an X-Forwarded-Port http-header if desired.
     * <p>
     * It is not possible to dynamically decide whether to return a bean here or not - the return value must always be non-null. However
     * it is possible to return a bean with property enabled=false (ie call setEnabled on the returned object), in which case the referenced
     * filter object will not be registered.
     * </p>
     */
    @Bean
    FilterRegistrationBean addForwardedPortHeader() {
        Filter filter = new AddForwardPortFilter();
        FilterRegistrationBean frb = new FilterRegistrationBean(filter);
        frb.setOrder(ORDER_BEFORE_SECURITY);
        // optionally call frb.setEnabled(false);
        return frb;
    }

    // =================================================================================================================================

    private static class AddForwardPortFilter implements Filter {
        private static final String HEADER_FORWARDED_PROTO = "X-Forwarded-Proto";
        private static final String HEADER_FORWARDED_PORT = "X-Forwarded-Port";

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

            // If request has X-Forwarded-Proto of "https" but not "X-Forwarded-Port" then set the forwarded port to 443.
            // The proxy which did the forwarding really should have set the -Port header - but we can do it for them here.
            HttpServletRequest req = (HttpServletRequest) request;
            String forwardedProto = req.getHeader(HEADER_FORWARDED_PROTO);
            String forwardedPort = req.getHeader(HEADER_FORWARDED_PORT);
            if ("https".equals(forwardedProto) && (forwardedPort == null)) {
                request = new AddHeaderRequest(req, HEADER_FORWARDED_PORT, "443");
            }

            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }
    }

    /**
     * Trivial class to "set an http header" on an incoming request.
     * <p>
     * Type HttpServletRequest does not provide a "set header" method - headers are immutable. It is therefore necessary to wrap the
     * original request and pass all calls through unaltered except for "getHeader"...
     * </p>
     */
    private static class AddHeaderRequest extends HttpServletRequestWrapper {
        private final String headerName;
        private final String headerValue;

        AddHeaderRequest(HttpServletRequest req, String headerName, String headerValue) {
            super(req);
            this.headerName = headerName;
            this.headerValue = headerValue;
        }

        @Override
        public String getHeader(String name) {
            if (headerName.equals(name)) {
                return headerValue;
            }

            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (headerName.equals(name)) {
                return Collections.enumeration(Collections.singletonList(headerValue));
            }

            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            return new ChainedEnumeration<String>(
                    super.getHeaderNames(),
                    Collections.enumeration(Collections.singletonList(headerName)));
        }
    }

    /**
     * Trivial class to make a single enumeration return data from multiple collections.
     */
    private static class ChainedEnumeration<T> implements Enumeration<T>  {
        Enumeration<T> first;
        Enumeration<T> second;
        ChainedEnumeration(Enumeration<T> first, Enumeration<T> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean hasMoreElements() {
            return first.hasMoreElements() || second.hasMoreElements();
        }

        @Override
        public T nextElement() {
            if (first.hasMoreElements()) {
                return first.nextElement();
            } else {
                return second.nextElement();
            }
        }
    }
}
