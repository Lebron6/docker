package com.aros.apron.tools;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class IPAddressUtil {  
  
    /**  
     * 获取本地IPv4地址  
     *  
     * @return IPv4地址，如果找不到则返回null  
     */  
    public static String getLocalIPv4Address() {  
        try {  
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {  
                NetworkInterface intf = en.nextElement();  
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {  
                    InetAddress inetAddress = enumIpAddr.nextElement();  
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();  
                    }  
                }  
            }  
        } catch (SocketException ex) {  
            // 处理异常  
            ex.printStackTrace();  
        }  
        return null;  
    } }