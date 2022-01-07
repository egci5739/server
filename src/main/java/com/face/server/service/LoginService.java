package com.face.server.service;

import com.face.server.entity.EquipmentEntity;
import com.face.server.hik.HCNetSDK;
import com.face.server.singleton.HikSingleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoginService {
    public int Login(EquipmentEntity equipmentEntity) {
        //注册设备
        HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();//设备登录信息
        String m_sDeviceIP = equipmentEntity.getEquipmentIp();//设备ip地址
        m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
        System.arraycopy(m_sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, m_sDeviceIP.length());
        String m_sUsername = "admin";//设备用户名
        m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
        System.arraycopy(m_sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, m_sUsername.length());
        String m_sPassword = "hik12345";//设备密码
        m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
        System.arraycopy(m_sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, m_sPassword.length());
        m_strLoginInfo.wPort = 8000;
        m_strLoginInfo.bUseAsynLogin = false; //是否异步登录：0- 否，1- 是
        m_strLoginInfo.write();
        HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();//设备信息
        int lUserID = HikSingleton.hik.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);
        if (lUserID == -1) {
            log.info("登录失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return -1;
        } else {
            log.info("登录成功，lUserID为：" + lUserID);
            return lUserID;
        }
    }
}
