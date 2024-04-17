/*
 * Util
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 27 Feb 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.core;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import com.connectsdk.service.capability.listeners.ErrorListener;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

public final class Util {
    static public String T = "Connect SDK";

    static private Handler handler;

    static private final int NUM_OF_THREADS = 20;

    static private Executor executor;

    static {
        createExecutor();
    }

    static void createExecutor() {
        Util.executor = Executors.newFixedThreadPool(NUM_OF_THREADS, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread th = new Thread(r);
                th.setName("2nd Screen BG");
                return th;
            }
        });
    }

    public static void runOnUI(Runnable runnable) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }

        handler.post(runnable);
    }

    public static void runInBackground(Runnable runnable, boolean forceNewThread) {
        if (forceNewThread || isMain()) {
            executor.execute(runnable);
        } else {
            runnable.run();
        }

    }

    public static void runInBackground(Runnable runnable) {
        runInBackground(runnable, false);
    }

    public static Executor getExecutor() {
        return executor;
    }

    public static boolean isMain() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static <T> void postSuccess(final ResponseListener<T> listener, final T object) {
        if (listener == null)
            return;

        Util.runOnUI(new Runnable() {

            @Override
            public void run() {
                listener.onSuccess(object);
            }
        });
    }

    public static void postError(final ErrorListener listener, final ServiceCommandError error) {
        if (listener == null)
            return;

        Util.runOnUI(new Runnable() {

            @Override
            public void run() {
                listener.onError(error);
            }
        });
    }

    public static byte[] convertIpAddress(int ip) {
        return new byte[] {
                (byte) (ip & 0xFF), 
                (byte) ((ip >> 8) & 0xFF), 
                (byte) ((ip >> 16) & 0xFF), 
                (byte) ((ip >> 24) & 0xFF)};
    }

    public static long getTime() {
        return TimeUnit.MILLISECONDS.toSeconds(new Date().getTime());
    }

    public static boolean isIPv4Address(String ipAddress) {
        return InetAddressUtils.isIPv4Address(ipAddress);
    }

    public static boolean isIPv6Address(String ipAddress) {
        return InetAddressUtils.isIPv6Address(ipAddress);
    }

    public static InetAddress getIpAddress(Context context) throws UnknownHostException {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();

        if (ip == 0) {
            /*return null;*/
            try {
                // 모든 네트워크 인터페이스를 가져옵니다.
                for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    // 네트워크 인터페이스 이름이 "eth"로 시작하는지 확인합니다.
                    if (ni.getName().startsWith("eth")) {
                        // 해당 인터페이스에 할당된 IP 주소 목록을 가져옵니다.
                        for (InetAddress address : Collections.list(ni.getInetAddresses())) {
                            // 외부로 통신 가능한 IPv4 주소인지 확인합니다.
                            if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                                return address;
                            }
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
            // 유선랜 인터페이스를 찾지 못하면 null을 반환합니다.
            return null;
        }
        else {
            byte[] ipAddress = convertIpAddress(ip);
            return InetAddress.getByAddress(ipAddress);
        }
    }
}