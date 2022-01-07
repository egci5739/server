package com.face.server.service;

import com.face.server.entity.EquipmentEntity;
import com.face.server.hik.HCNetSDK;
import com.face.server.singleton.HikSingleton;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HikService {
    /*
     * 获取设备工作状态
     * 卡数量
     * */
    public int getEquipmentCardNums(EquipmentEntity equipmentEntity) {
        IntByReference ibrBytesReturned = new IntByReference(0);
        boolean bRet;
        HCNetSDK.NET_DVR_ACS_WORK_STATUS_V50 net_dvr_acs_work_status_v50 = new HCNetSDK.NET_DVR_ACS_WORK_STATUS_V50();
        net_dvr_acs_work_status_v50.write();
        bRet = HikSingleton.hik.NET_DVR_GetDVRConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_GET_ACS_WORK_STATUS_V50, -1, net_dvr_acs_work_status_v50.getPointer(), net_dvr_acs_work_status_v50.size(), ibrBytesReturned);
        net_dvr_acs_work_status_v50.read();
        if (bRet) {
            return net_dvr_acs_work_status_v50.dwCardNum;
        } else {
            return 0;
        }
    }
}
