package com.workshop.logproxy.proxy;

/**
 * Common contract shared by the real microservices and the proxies that wrap them.
 *
 * @param <T> type of the value returned by the operation
 */
public interface MicroserviceProxy<T> {

    T execute(String operation, Object... params);
}
