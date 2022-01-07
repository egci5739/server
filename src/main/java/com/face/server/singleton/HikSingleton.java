package com.face.server.singleton;

import com.face.server.hik.HCNetSDK;

public class HikSingleton {
    public static HCNetSDK hik = HCNetSDK.INSTANCE;

    public static boolean init() {
        return hik.NET_DVR_Init();
    }

    public static boolean clean() {
        return hik.NET_DVR_Cleanup();
    }

}
