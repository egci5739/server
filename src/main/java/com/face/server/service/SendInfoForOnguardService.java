package com.face.server.service;

import com.face.server.entity.StaffStatEntity;
import com.face.server.hik.HCNetSDK;
import com.face.server.singleton.HikSingleton;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Slf4j
@Service
public class SendInfoForOnguardService {
    int m_lSetCardCfgHandle = -1; //下发卡长连接句柄
    int m_lSetMultiCardCfgHandle = -1;//批量下发卡长连接句柄
    int m_lDelCardCfgHandle = -1;//删除卡长连接句柄
    int iCharEncodeType = 0;//设备字符集
    int dwState = -1; //下发卡数据状态
    int dwCardState = -1; //下发卡数据状态
    int m_lSetMultiFaceCfgHandle = -1; //批量下发人脸长连接句柄
    int dwFaceState = -1; //下发人脸数据状态
    int m_lSetFaceCfgHandle = -1; //下发人脸长连接句柄
    int m_lDelMultiCardCfgHandle = -1;//批量删除卡长连接句柄
    int dwDelCardState = -1; //下发人脸数据状态

    /*
     * 下发单张卡
     * */
    public void setCard(int lUserID, StaffStatEntity staffStatEntity) throws UnsupportedEncodingException, InterruptedException {
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = 1;  //下发一张
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lSetCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD, ptrStruCond, struCardCond.size(), null, null);
        if (m_lSetCardCfgHandle == -1) {
            log.info("建立下发卡长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立下发卡长连接成功！");
        }
        HCNetSDK.NET_DVR_CARD_RECORD struCardRecord = new HCNetSDK.NET_DVR_CARD_RECORD();
        struCardRecord.read();
        struCardRecord.dwSize = struCardRecord.size();
        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            struCardRecord.byCardNo[i] = 0;
        }
        for (int i = 0; i < staffStatEntity.getCard().length(); i++) {
            struCardRecord.byCardNo[i] = staffStatEntity.getCard().getBytes()[i];
        }
        struCardRecord.byCardType = 1; //普通卡
        struCardRecord.byLeaderCard = 0; //是否为首卡，0-否，1-是
        struCardRecord.byUserType = 0;
        struCardRecord.byDoorRight[0] = 1; //门1有权限
        struCardRecord.wCardRightPlan[0] = 1;//关联门计划模板，使用了前面配置的计划模板
        struCardRecord.struValid.byEnable = 1;    //卡有效期使能，下面是卡有效期从2000-1-1 11:11:11到2030-1-1 11:11:11
        struCardRecord.struValid.struBeginTime.wYear = 2000;
        struCardRecord.struValid.struBeginTime.byMonth = 1;
        struCardRecord.struValid.struBeginTime.byDay = 1;
        struCardRecord.struValid.struBeginTime.byHour = 11;
        struCardRecord.struValid.struBeginTime.byMinute = 11;
        struCardRecord.struValid.struBeginTime.bySecond = 11;
        struCardRecord.struValid.struEndTime.wYear = 2030;
        struCardRecord.struValid.struEndTime.byMonth = 1;
        struCardRecord.struValid.struEndTime.byDay = 1;
        struCardRecord.struValid.struEndTime.byHour = 11;
        struCardRecord.struValid.struEndTime.byMinute = 11;
        struCardRecord.struValid.struEndTime.bySecond = 11;
//        struCardRecord.dwEmployeeNo = 66611; //工号
        if ((iCharEncodeType == 0) || (iCharEncodeType == 1) || (iCharEncodeType == 2)) {
            byte[] strCardName = staffStatEntity.getName().getBytes("GBK");  //姓名
            for (int i = 0; i < HCNetSDK.NAME_LEN; i++) {
                struCardRecord.byName[i] = 0;
            }
            System.arraycopy(strCardName, 0, struCardRecord.byName, 0, strCardName.length);
        }
        if (iCharEncodeType == 6) {
            byte[] strCardName = staffStatEntity.getName().getBytes("UTF-8");  //姓名
            for (int i = 0; i < HCNetSDK.NAME_LEN; i++) {
                struCardRecord.byName[i] = 0;
            }
            System.arraycopy(strCardName, 0, struCardRecord.byName, 0, strCardName.length);
        }
        struCardRecord.write();
        HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        IntByReference pInt = new IntByReference(0);
        while (true) {
            dwState = HikSingleton.hik.NET_DVR_SendWithRecvRemoteConfig(m_lSetCardCfgHandle, struCardRecord.getPointer(), struCardRecord.size(), struCardStatus.getPointer(), struCardStatus.size(), pInt);
            struCardStatus.read();
            if (dwState == -1) {
                log.info("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT) {
//                log.info("配置等待");
                Thread.sleep(10);
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("下发卡失败, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                log.info("下发卡异常, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if (struCardStatus.dwErrorCode != 0) {
                    log.info("下发卡成功,但是错误码" + struCardStatus.dwErrorCode + ", 卡号：" + new String(struCardStatus.byCardNo).trim());
                } else {
                    log.info("下发卡成功, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 状态：" + struCardStatus.byStatus);
                }
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                log.info("下发卡完成");
                break;
            }
        }
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lSetCardCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }

    /*
     * 删除单张卡
     * */
    public void delCard(int lUserID, StaffStatEntity staffStatEntity) throws UnsupportedEncodingException, InterruptedException {
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = 1;  //下发一张
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lDelCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_DEL_CARD, ptrStruCond, struCardCond.size(), null, null);
        if (m_lDelCardCfgHandle == -1) {
            log.info("建立删除卡长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立删除卡长连接成功！");
        }
        HCNetSDK.NET_DVR_CARD_SEND_DATA struCardData = new HCNetSDK.NET_DVR_CARD_SEND_DATA();
        struCardData.read();
        struCardData.dwSize = struCardData.size();
        for (int i = 0; i < HCNetSDK.ACS_CARD_NO_LEN; i++) {
            struCardData.byCardNo[i] = 0;
        }
        for (int i = 0; i < staffStatEntity.getCard().length(); i++) {
            struCardData.byCardNo[i] = staffStatEntity.getCard().getBytes()[i];
        }
        struCardData.write();
        HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        IntByReference pInt = new IntByReference(0);
        while (true) {
            dwState = HikSingleton.hik.NET_DVR_SendWithRecvRemoteConfig(m_lDelCardCfgHandle, struCardData.getPointer(), struCardData.size(), struCardStatus.getPointer(), struCardStatus.size(), pInt);
            struCardStatus.read();
            if (dwState == -1) {
                log.info("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT) {
//                log.info("配置等待");
                Thread.sleep(10);
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("删除卡失败, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                log.info("删除卡异常, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if (struCardStatus.dwErrorCode != 0) {
                    log.info("删除卡成功,但是错误码" + struCardStatus.dwErrorCode + ", 卡号：" + new String(struCardStatus.byCardNo).trim());
                } else {
                    log.info("删除卡成功, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 状态：" + struCardStatus.byStatus);
                }
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                log.info("删除卡完成");
                break;
            }
        }
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lDelCardCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }

    /*
     * 下发单张人脸
     * */
    public void setFace(int lUserID, StaffStatEntity staffStatEntity) throws InterruptedException {
        HCNetSDK.NET_DVR_FACE_COND struFaceCond = new HCNetSDK.NET_DVR_FACE_COND();
        struFaceCond.read();
        struFaceCond.dwSize = struFaceCond.size();
        struFaceCond.byCardNo = staffStatEntity.getCard().getBytes();
        struFaceCond.dwFaceNum = 1;  //下发一张
        struFaceCond.dwEnableReaderNo = 1;//人脸读卡器编号
        struFaceCond.write();
        Pointer ptrStruFaceCond = struFaceCond.getPointer();
        m_lSetFaceCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_FACE, ptrStruFaceCond, struFaceCond.size(), null, null);
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
        for (int i = 0; i < staffStatEntity.getCard().length(); i++) {
            struFaceRecord.byCardNo[i] = staffStatEntity.getCard().getBytes()[i];
        }
        int picdataLength = 0;
        picdataLength = staffStatEntity.getImage().length;
        if (picdataLength < 0) {
            log.info("input file dataSize < 0");
            return;
        }
        HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
        ptrpicByte.byValue = staffStatEntity.getImage();
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

    /*
     * 批量下发卡号
     * */
    public void setCards(int iNum, int lUserID, List<StaffStatEntity> staffStatEntityList) throws UnsupportedEncodingException {
        /*
         * 下发卡号
         * */
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = iNum;  //下发张数
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lSetMultiCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_CARD, ptrStruCond, struCardCond.size(), null, null);
        if (m_lSetMultiCardCfgHandle == -1) {
            log.info("建立下发卡长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立下发卡长连接成功！");
        }
        HCNetSDK.NET_DVR_CARD_RECORD[] struCardRecord = (HCNetSDK.NET_DVR_CARD_RECORD[]) new HCNetSDK.NET_DVR_CARD_RECORD().toArray(iNum);
        HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        IntByReference pInt = new IntByReference(0);
        for (int i = 0; i < iNum; i++) {
            struCardRecord[i].read();
            struCardRecord[i].dwSize = struCardRecord[i].size();
            for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++) {
                struCardRecord[i].byCardNo[j] = 0;
            }
            System.arraycopy(staffStatEntityList.get(i).getCard().getBytes(), 0, struCardRecord[i].byCardNo, 0, staffStatEntityList.get(i).getCard().getBytes().length);
            struCardRecord[i].byCardType = 1; //普通卡
            struCardRecord[i].byLeaderCard = 0; //是否为首卡，0-否，1-是
            struCardRecord[i].byUserType = 0;
            struCardRecord[i].byDoorRight[0] = 1; //门1有权限
            struCardRecord[i].wCardRightPlan[0] = 1;//关联门计划模板，使用了前面配置的计划模板
            struCardRecord[i].struValid.byEnable = 1;    //卡有效期使能，下面是卡有效期从2000-1-1 11:11:11到2030-1-1 11:11:11
            struCardRecord[i].struValid.struBeginTime.wYear = 2000;
            struCardRecord[i].struValid.struBeginTime.byMonth = 1;
            struCardRecord[i].struValid.struBeginTime.byDay = 1;
            struCardRecord[i].struValid.struBeginTime.byHour = 11;
            struCardRecord[i].struValid.struBeginTime.byMinute = 11;
            struCardRecord[i].struValid.struBeginTime.bySecond = 11;
            struCardRecord[i].struValid.struEndTime.wYear = 2030;
            struCardRecord[i].struValid.struEndTime.byMonth = 1;
            struCardRecord[i].struValid.struEndTime.byDay = 1;
            struCardRecord[i].struValid.struEndTime.byHour = 11;
            struCardRecord[i].struValid.struEndTime.byMinute = 11;
            struCardRecord[i].struValid.struEndTime.bySecond = 11;
            if ((iCharEncodeType == 0) || (iCharEncodeType == 1) || (iCharEncodeType == 2)) {
                byte[] strCardName = staffStatEntityList.get(i).getName().getBytes("GBK");  //姓名
                for (int j = 0; j < HCNetSDK.NAME_LEN; j++) {
                    struCardRecord[i].byName[j] = 0;
                }
                System.arraycopy(strCardName, 0, struCardRecord[i].byName, 0, strCardName.length);
            }
            if (iCharEncodeType == 6) {
                byte[] strCardName = staffStatEntityList.get(i).getName().getBytes("UTF-8");  //姓名
                for (int j = 0; j < HCNetSDK.NAME_LEN; j++) {
                    struCardRecord[i].byName[j] = 0;
                }
                System.arraycopy(strCardName, 0, struCardRecord[i].byName, 0, strCardName.length);
            }
            struCardRecord[i].write();
            dwCardState = HikSingleton.hik.NET_DVR_SendWithRecvRemoteConfig(m_lSetMultiCardCfgHandle, struCardRecord[i].getPointer(), struCardRecord[i].size(), struCardStatus.getPointer(), struCardStatus.size(), pInt);
            struCardStatus.read();
            if (dwCardState == -1) {
                log.info("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
            } else if (dwCardState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("下发卡失败, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                //可以继续下发下一个
            } else if (dwCardState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                log.info("下发卡异常, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                //异常是长连接异常，不能继续下发后面的数据，需要重新建立长连接
                break;
            } else if (dwCardState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if (struCardStatus.dwErrorCode != 0) {
                    log.info("下发卡失败,错误码" + struCardStatus.dwErrorCode + ", 卡号：" + new String(struCardStatus.byCardNo).trim());
                } else {
                    log.info("下发卡成功, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 状态：" + struCardStatus.byStatus);
                }
                //可以继续下发下一个
            } else {
                log.info("其他状态：" + dwCardState);
            }
        }
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lSetMultiCardCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }

    /*
     * 批量下发人脸
     * */
    public void SetFaces(int iNum, int lUserID, List<StaffStatEntity> staffStatEntityList) {
        HCNetSDK.NET_DVR_FACE_COND struFaceCond = new HCNetSDK.NET_DVR_FACE_COND();
        struFaceCond.read();
        struFaceCond.dwSize = struFaceCond.size();
        struFaceCond.byCardNo = new byte[32]; //批量下发，该卡号不需要赋值
        struFaceCond.dwFaceNum = iNum;  //下发个数
        struFaceCond.dwEnableReaderNo = 1;//人脸读卡器编号
        struFaceCond.write();
        Pointer ptrStruFaceCond = struFaceCond.getPointer();
        m_lSetMultiFaceCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_SET_FACE, ptrStruFaceCond, struFaceCond.size(), null, null);
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
        for (int i = 0; i < iNum; i++) {
            HCNetSDK.NET_DVR_FACE_RECORD struFaceRecord = new HCNetSDK.NET_DVR_FACE_RECORD();
            struFaceRecord.read();
            struFaceRecord.dwSize = struFaceRecord.size();
            for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++) {
                struFaceRecord.byCardNo[j] = 0;
            }
            for (int j = 0; j < staffStatEntityList.get(i).getCard().length(); j++) {
                struFaceRecord.byCardNo[j] = staffStatEntityList.get(i).getCard().getBytes()[j];
            }
            int picdataLength = 0;
            try {
                picdataLength = staffStatEntityList.get(i).getImage().length;
            } catch (NullPointerException ignored) {
            }
            if (picdataLength < 0) {
                log.info("input file dataSize < 0");
                return;
            }
            log.info(i + "长度：" + picdataLength);
            HCNetSDK.BYTE_ARRAY ptrpicByte = new HCNetSDK.BYTE_ARRAY(picdataLength);
            ptrpicByte.byValue = staffStatEntityList.get(i).getImage();
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
        }
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lSetMultiFaceCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }

    /*
     * 批量删除卡号
     * */
    public void delCards(int iNum, int lUserID, List<StaffStatEntity> staffStatEntityList) throws InterruptedException {
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = iNum;  //删除多张
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lDelMultiCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(lUserID, HCNetSDK.NET_DVR_DEL_CARD, ptrStruCond, struCardCond.size(), null, null);
        if (m_lDelMultiCardCfgHandle == -1) {
            log.info("建立删除卡长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立删除卡长连接成功！");
        }
        HCNetSDK.NET_DVR_CARD_SEND_DATA[] struCardData = (HCNetSDK.NET_DVR_CARD_SEND_DATA[]) new HCNetSDK.NET_DVR_CARD_SEND_DATA().toArray(iNum);
        HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        IntByReference pInt = new IntByReference(0);
        for (int i = 0; i < iNum; i++) {
            struCardData[i].read();
            struCardData[i].dwSize = struCardData[i].size();
            for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++) {
                struCardData[i].byCardNo[j] = 0;
            }
            for (int j = 0; j < staffStatEntityList.get(i).getCard().length(); j++) {
                struCardData[i].byCardNo[j] = staffStatEntityList.get(i).getCard().getBytes()[j];
            }
            struCardData[i].write();
            dwDelCardState = HikSingleton.hik.NET_DVR_SendWithRecvRemoteConfig(m_lDelMultiCardCfgHandle, struCardData[i].getPointer(), struCardData[i].size(), struCardStatus.getPointer(), struCardStatus.size(), pInt);
            struCardStatus.read();
            if (dwDelCardState == -1) {
                log.info("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                break;
            } else if (dwDelCardState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT) {
//                log.info("配置等待");
                Thread.sleep(10);
                continue;
            } else if (dwDelCardState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("删除卡失败, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                break;
            } else if (dwDelCardState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                log.info("删除卡异常, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                break;
            } else if (dwDelCardState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if (struCardStatus.dwErrorCode != 0) {
                    log.info("删除卡成功,但是错误码" + struCardStatus.dwErrorCode + ", 卡号：" + new String(struCardStatus.byCardNo).trim());
                } else {
                    log.info("删除卡成功, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 状态：" + struCardStatus.byStatus);
                }
                continue;
            } else if (dwDelCardState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                log.info("删除卡完成");
                break;
            }
        }
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lDelMultiCardCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }
}
