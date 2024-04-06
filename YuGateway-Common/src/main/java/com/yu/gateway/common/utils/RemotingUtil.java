package com.yu.gateway.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * @author yu
 * @description 远程工具类
 * @date 2024-04-06
 */
@Slf4j
public class RemotingUtil {
    public static final String OS_NAME = System.getProperty("os.name");
    private static boolean isLinuxPlatform = false;
    private static boolean isWindowsPlatform = false;

    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }

    public static boolean isIsLinuxPlatform() {
        return isLinuxPlatform;
    }

    public static boolean isIsWindowsPlatform() {
        return isWindowsPlatform;
    }

    /**
     * 开启linux平台selector优化io
     */
    public static Selector openSelector() {
        Selector result = null;
        if (isLinuxPlatform) {
            try {
                final Class<?> clazz = Class.forName("sun.nio.ch.EPollSelectorProvider");
                if (clazz != null) {
                    Method method = clazz.getMethod("provider");
                    SelectorProvider provider = (SelectorProvider) method.invoke(null);
                    if (provider != null) {
                        result = provider.openSelector();
                    }
                }
            } catch (Exception e) {
                log.warn("LinuxSystem Open Selector exception", e);
            }
        }
        if (result == null) {
            try {
                result = Selector.open();
            } catch (IOException e) {
                log.warn("Selector open failed", e);
            }
        }
        return result;
    }

    /**
     * address To string
     * @param address
     * @return
     */
    public static String socketAddress2String(final SocketAddress address) {
        StringBuilder builder = new StringBuilder();
        InetSocketAddress inetSocketAddress = (InetSocketAddress) address;
        builder.append(inetSocketAddress.getAddress().getHostAddress());
        builder.append(":");
        builder.append(inetSocketAddress.getPort());
        return builder.toString();
    }

    /**
     * string To Address
     * @param str
     * @return
     */
    public static SocketAddress string2SocketAddress(String str) {
        String[] strs = str.split(":");
        InetSocketAddress address = new InetSocketAddress(strs[0], Integer.parseInt(strs[1]));
        return address;
    }

    /**
     *  解析获取本地IP地址
     * @return
     */
    public static String getLocalAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            ArrayList<String> ipv4Results = new ArrayList<>();
            ArrayList<String> ipv6Results = new ArrayList<>();
            while(networkInterfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while(inetAddresses.hasMoreElements()) {
                    final InetAddress inetAddress = inetAddresses.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) {
                            ipv4Results.add(normalizeAddress(inetAddress));
                        } else {
                            ipv6Results.add(normalizeAddress(inetAddress));
                        }
                    }
                }
            }

            if (!ipv4Results.isEmpty()) {
                for (String ip : ipv4Results) {
                    if (ip.startsWith("127.0") || ip.startsWith("192.168")) {
                        continue;
                    }
                    return ip;
                }
                return ipv4Results.get(ipv4Results.size() - 1);
            } else if (!ipv6Results.isEmpty()) {
                return ipv6Results.get(0);
            }
            final InetAddress localhost = InetAddress.getLocalHost();
            return normalizeAddress(localhost);
        } catch (Exception e) {
            log.error("Failed to obtain localAddress", e);
        }
        return null;
    }

    public static String normalizeAddress(final InetAddress host) {
        if (host instanceof Inet6Address) {
            return "[" + host.getHostAddress() + "]";
        } else {
            return host.getHostAddress();
        }
    }
}
