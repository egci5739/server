package com.face.server.service;

import com.face.server.entity.EquipmentEntity;
import com.face.server.entity.StaffEntity;
import com.face.server.hik.HCNetSDK;
import com.face.server.singleton.HikSingleton;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@EnableAsync
public class FaceService {
    int m_lSetFaceCfgHandle = -1; //下发人脸长连接句柄

    int m_lSetMultiFaceCfgHandle = -1; //批量下发人脸长连接句柄
    int dwFaceState = -1; //下发人脸数据状态

    int m_lGetFaceCfgHandle = -1;//获取所有人脸

    int iCharEncodeType = 0;//设备字符集
    int setFacesProgress = 0;//下发人脸进度
    int dwState = -1;

    public int getSetFacesTotal() {
        return setFacesTotal;
    }

    int setFacesTotal = 0;//下发人脸总数

    public int getSetFacesProgress() {
        return setFacesProgress;
    }

    /*
     * 获取人员图片
     * 无照片人员
     * 目前只能获取单人
     * */
    public void GetAllFace(EquipmentEntity equipmentEntity) {
        HCNetSDK.NET_DVR_FACE_COND struFaceCond = new HCNetSDK.NET_DVR_FACE_COND();
        struFaceCond.read();
        struFaceCond.dwSize = struFaceCond.size();
        struFaceCond.dwFaceNum = 1; //查询一个人脸参数
        struFaceCond.dwEnableReaderNo = 1;//读卡器编号
        for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++) {
            struFaceCond.byCardNo[j] = 0;
        }
        String strCardNo = "234912";
        System.arraycopy(strCardNo.getBytes(), 0, struFaceCond.byCardNo, 0, strCardNo.getBytes().length);
        struFaceCond.write();
        int m_lHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_GET_FACE, struFaceCond.getPointer(), struFaceCond.size(), null, null);
        if (m_lHandle == -1) {
            log.info("建立查询人脸参数长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立查询人脸参数长连接成功！");
        }
        //查询结果
        HCNetSDK.NET_DVR_FACE_RECORD struFaceRecord = new HCNetSDK.NET_DVR_FACE_RECORD();
        struFaceRecord.read();
        while (true) {
            dwState = HikSingleton.hik.NET_DVR_GetNextRemoteConfig(m_lHandle, struFaceRecord.getPointer(), struFaceRecord.size());
            struFaceRecord.read();
            if (dwState == -1) {
                log.info("NET_DVR_GetNextRemoteConfig查询人脸调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT) {
                log.info("查询中，请等待...");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("获取人脸参数失败, 卡号: " + strCardNo);
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if (struFaceRecord.dwFaceLen > 0) {
                    log.info("有图片");
                }
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                log.info("获取人脸参数完成");
                break;
            }
        }

        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }

    public void SetOneFace(EquipmentEntity equipmentEntity, StaffEntity staffEntity) throws InterruptedException {
        HCNetSDK.NET_DVR_FACE_COND struFaceCond = new HCNetSDK.NET_DVR_FACE_COND();
        struFaceCond.read();
        struFaceCond.dwSize = struFaceCond.size();
        struFaceCond.byCardNo = staffEntity.getStaffCardNumber().getBytes();
        struFaceCond.dwFaceNum = 1;  //下发一张
        struFaceCond.dwEnableReaderNo = 1;//人脸读卡器编号
        struFaceCond.write();
        Pointer ptrStruFaceCond = struFaceCond.getPointer();
        m_lSetFaceCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_SET_FACE, ptrStruFaceCond, struFaceCond.size(), null, null);
        if (m_lSetFaceCfgHandle == -1) {
            log.info("建立下发人脸长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立下发人脸长连接成功！");
        }
        HCNetSDK.NET_DVR_FACE_RECORD struFaceRecord = new HCNetSDK.NET_DVR_FACE_RECORD();
        struFaceRecord.read();
        struFaceRecord.dwSize = struFaceRecord.size();
        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            struFaceRecord.byCardNo[i] = 0;
        }
        for (int i = 0; i < staffEntity.getStaffCardNumber().length(); i++) {
            struFaceRecord.byCardNo[i] = staffEntity.getStaffCardNumber().getBytes()[i];
        }
        int picdataLength = 0;
        picdataLength = staffEntity.getStaffImage().length;
        if (picdataLength < 0) {
            log.info("input file dataSize < 0");
            return;
        }
        HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
        ptrpicByte.byValue = staffEntity.getStaffImage();
        ptrpicByte.write();
        struFaceRecord.dwFaceLen = picdataLength;
        struFaceRecord.pFaceBuffer = ptrpicByte.getPointer();
        struFaceRecord.write();
        HCNetSDK.NET_DVR_FACE_STATUS struFaceStatus = new HCNetSDK.NET_DVR_FACE_STATUS();
        struFaceStatus.read();
        struFaceStatus.dwSize = struFaceStatus.size();
        struFaceStatus.write();
        IntByReference pInt = new IntByReference(0);
        while (true) {
            dwFaceState = HikSingleton.hik.NET_DVR_SendWithRecvRemoteConfig(m_lSetFaceCfgHandle, struFaceRecord.getPointer(), struFaceRecord.size(), struFaceStatus.getPointer(), struFaceStatus.size(), pInt);
            struFaceStatus.read();
            if (dwFaceState == -1) {
                log.info("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                break;
            } else if (dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT) {
//                log.info("配置等待");
                Thread.sleep(10);
                continue;
            } else if (dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("下发人脸失败, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                break;
            } else if (dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                log.info("下发人脸异常, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                break;
            } else if (dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if (struFaceStatus.byRecvStatus != 1) {
                    log.info("下发人脸失败，人脸读卡器状态" + struFaceStatus.byRecvStatus + ", 卡号：" + new String(struFaceStatus.byCardNo).trim());
                    break;
                } else {
                    log.info("下发人脸成功, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 状态：" + struFaceStatus.byRecvStatus);
                }
                continue;
            } else if (dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                log.info("下发人脸完成");
                break;
            }
        }
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lSetFaceCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }

    public void SetMultiFace(EquipmentEntity equipmentEntity, List<StaffEntity> staffEntityList) {
        setFacesTotal = staffEntityList.size();
        HCNetSDK.NET_DVR_FACE_COND struFaceCond = new HCNetSDK.NET_DVR_FACE_COND();
        struFaceCond.read();
        struFaceCond.dwSize = struFaceCond.size();
        struFaceCond.byCardNo = new byte[32]; //批量下发，该卡号不需要赋值
        struFaceCond.dwFaceNum = staffEntityList.size();  //下发个数
        struFaceCond.dwEnableReaderNo = 1;//人脸读卡器编号
        struFaceCond.write();
        Pointer ptrStruFaceCond = struFaceCond.getPointer();
        m_lSetMultiFaceCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_SET_FACE, ptrStruFaceCond, struFaceCond.size(), null, null);
        if (m_lSetMultiFaceCfgHandle == -1) {
            log.info("建立下发人脸长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立下发人脸长连接成功！");
        }
        HCNetSDK.NET_DVR_FACE_STATUS struFaceStatus = new HCNetSDK.NET_DVR_FACE_STATUS();
        struFaceStatus.read();
        struFaceStatus.dwSize = struFaceStatus.size();
        struFaceStatus.write();
        IntByReference pInt = new IntByReference(0);
        setFacesProgress = 0;
        for (int i = 0; i < staffEntityList.size(); i++) {
            HCNetSDK.NET_DVR_FACE_RECORD struFaceRecord = new HCNetSDK.NET_DVR_FACE_RECORD();
            struFaceRecord.read();
            struFaceRecord.dwSize = struFaceRecord.size();
            for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++) {
                struFaceRecord.byCardNo[j] = 0;
            }
            for (int j = 0; j < staffEntityList.get(i).getStaffCardNumber().length(); j++) {
                struFaceRecord.byCardNo[j] = staffEntityList.get(i).getStaffCardNumber().getBytes()[j];
            }
            int picdataLength = 0;
            try {
                picdataLength = staffEntityList.get(i).getStaffImage().length;
            } catch (NullPointerException ignored) {
            }
            if (picdataLength < 0) {
                log.info("input file dataSize < 0");
                return;
            }
            HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
            ptrpicByte.byValue = staffEntityList.get(i).getStaffImage();
            ptrpicByte.write();
            struFaceRecord.dwFaceLen = picdataLength;
            struFaceRecord.pFaceBuffer = ptrpicByte.getPointer();
            struFaceRecord.write();
            dwFaceState = HikSingleton.hik.NET_DVR_SendWithRecvRemoteConfig(m_lSetMultiFaceCfgHandle, struFaceRecord.getPointer(), struFaceRecord.size(), struFaceStatus.getPointer(), struFaceStatus.size(), pInt);
            struFaceStatus.read();
            if (dwFaceState == -1) {
                log.info("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
            } else if (dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("下发人脸失败, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                //可以继续下发下一个
            } else if (dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                log.info("下发人脸异常, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                //异常是长连接异常，不能继续下发后面的数据，需要重新建立长连接
                break;
            } else if (dwFaceState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if (struFaceStatus.byRecvStatus != 1) {
                    log.info("下发人脸失败，人脸读卡器状态" + struFaceStatus.byRecvStatus + ", 卡号：" + new String(struFaceStatus.byCardNo).trim());
                } else {
                    log.info("下发人脸成功, 卡号: " + new String(struFaceStatus.byCardNo).trim() + ", 状态：" + struFaceStatus.byRecvStatus);
                }
                //可以继续下发下一个
            } else {
                log.info("其他状态：" + dwFaceState);
            }
            setFacesProgress++;
            Thread.currentThread().setName("人脸下发：" + setFacesProgress + "/" + setFacesTotal);
        }
        setFacesProgress = 0;
        setFacesTotal = 0;
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lSetMultiFaceCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }

    public void DelOneFace(EquipmentEntity equipmentEntity, StaffEntity staffEntity) {
        HCNetSDK.NET_DVR_FACE_PARAM_CTRL struFaceDelCond = new HCNetSDK.NET_DVR_FACE_PARAM_CTRL();
        struFaceDelCond.dwSize = struFaceDelCond.size();
        struFaceDelCond.byMode = 0; //删除方式：0- 按卡号方式删除，1- 按读卡器删除
        struFaceDelCond.struProcessMode.setType(HCNetSDK.NET_DVR_FACE_PARAM_BYCARD.class);
        //需要删除人脸关联的卡号
        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            struFaceDelCond.struProcessMode.struByCard.byCardNo[i] = 0;
        }
        System.arraycopy(staffEntity.getStaffCardNumber().getBytes(), 0, struFaceDelCond.struProcessMode.struByCard.byCardNo, 0, staffEntity.getStaffCardNumber().length());
        struFaceDelCond.struProcessMode.struByCard.byEnableCardReader[0] = 1; //读卡器
        struFaceDelCond.struProcessMode.struByCard.byFaceID[0] = 1; //人脸ID
        struFaceDelCond.write();
        Pointer ptrFaceDelCond = struFaceDelCond.getPointer();
        boolean bRet = HikSingleton.hik.NET_DVR_RemoteControl(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_DEL_FACE_PARAM_CFG, ptrFaceDelCond, struFaceDelCond.size());
        if (!bRet) {
            log.info("删除人脸失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("删除人脸成功！");
        }
    }
}
