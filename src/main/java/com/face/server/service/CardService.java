package com.face.server.service;

import com.face.server.entity.EquipmentEntity;
import com.face.server.entity.StaffEntity;
import com.face.server.hik.HCNetSDK;
import com.face.server.singleton.HikSingleton;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CardService {
    int m_lSetCardCfgHandle = -1; //下发卡长连接句柄
    int m_lSetMultiCardCfgHandle = -1;//批量下发卡长连接句柄
    int m_lGetCardCfgHandle = -1; //查询卡长连接句柄
    int m_lDelCardCfgHandle = -1;//删除卡长连接句柄
    int m_lDelMultiCardCfgHandle = -1;//批量删除卡长连接句柄
    int dwState = -1; //下发卡数据状态
    int iCharEncodeType = 0;//设备字符集
    int setCardsProgress = 0;//下发卡进度
    int delCardsProgress = 0;//删除卡进度

    public int getSetCardsTotal() {
        return setCardsTotal;
    }

    int setCardsTotal = 0;//下发卡总数
    int delCardsTotal = 0;//删除卡总数

    public int getDelCardsTotal() {
        return delCardsTotal;
    }

    /*
     * 获取下发卡号进度
     * */
    public int getSetCardsProgress() {
        return setCardsProgress;
    }

    /*
     * 获取删除卡号进度
     * */
    public int getDelCardsProgress() {
        return delCardsProgress;
    }


    /*
     * 查询所有卡信息
     * */
//    @Async
    public List<StaffEntity> GetAllCard(EquipmentEntity equipmentEntity) {
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = 0xffffffff; //查询所有
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lGetCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_GET_CARD, ptrStruCond, struCardCond.size(), null, null);
        if (m_lGetCardCfgHandle == -1) {
            log.info("建立下发卡长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return null;
        } else {
            log.info("建立下发卡长连接成功！");
        }
        HCNetSDK.NET_DVR_CARD_RECORD struCardRecord = new HCNetSDK.NET_DVR_CARD_RECORD();
        struCardRecord.read();
        struCardRecord.dwSize = struCardRecord.size();
        struCardRecord.write();
        IntByReference pInt = new IntByReference(0);
        List<StaffEntity> staffEntityList = new ArrayList<>();
        while (true) {
            dwState = HikSingleton.hik.NET_DVR_GetNextRemoteConfig(m_lGetCardCfgHandle, struCardRecord.getPointer(), struCardRecord.size());
            struCardRecord.read();
            if (dwState == -1) {
                log.info("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_NEEDWAIT) {
//                log.info("配置等待");
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("获取卡参数失败");
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                log.info("获取卡参数异常");
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                try {
                    String strName = "";
                    if ((iCharEncodeType == 0) || (iCharEncodeType == 1) || (iCharEncodeType == 2)) {
                        strName = new String(struCardRecord.byName, "GBK").trim();
                    }
                    if (iCharEncodeType == 6) {
                        strName = new String(struCardRecord.byName, "UTF-8").trim();
                    }
//                    i++;
//                    log.info(i + "获取卡参数成功, 卡号: " + new String(struCardRecord.byCardNo).trim() + ", 卡类型：" + struCardRecord.byCardType + ", 姓名：" + strName);
                    StaffEntity staffEntity = new StaffEntity();
                    staffEntity.setStaffCardNumber(new String(struCardRecord.byCardNo).trim());
                    staffEntity.setStaffName(strName);
                    staffEntityList.add(staffEntity);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                log.info(equipmentEntity.getEquipmentName() + "获取卡参数完成");
                break;
            }
        }
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lGetCardCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
        return staffEntityList;
    }

    /*
     * 下发单张卡
     * */
    public void SetOneCard(EquipmentEntity equipmentEntity, StaffEntity staffEntity) throws UnsupportedEncodingException, InterruptedException {
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = 1;  //下发一张
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lSetCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_SET_CARD, ptrStruCond, struCardCond.size(), null, null);
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
        for (int i = 0; i < staffEntity.getStaffCardNumber().length(); i++) {
            struCardRecord.byCardNo[i] = staffEntity.getStaffCardNumber().getBytes()[i];
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
        if ((iCharEncodeType == 0) || (iCharEncodeType == 1) || (iCharEncodeType == 2)) {
            byte[] strCardName = staffEntity.getStaffName().getBytes("GBK");  //姓名
            for (int i = 0; i < HCNetSDK.NAME_LEN; i++) {
                struCardRecord.byName[i] = 0;
            }
            System.arraycopy(strCardName, 0, struCardRecord.byName, 0, strCardName.length);
        }
        if (iCharEncodeType == 6) {
            byte[] strCardName = staffEntity.getStaffName().getBytes("UTF-8");  //姓名
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
     * 批量下发卡
     * */
    public void SetMultiCard(EquipmentEntity equipmentEntity, List<StaffEntity> staffEntityList) throws UnsupportedEncodingException, InterruptedException {
        setCardsTotal = staffEntityList.size();
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = staffEntityList.size();  //下发张数
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lSetMultiCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_SET_CARD, ptrStruCond, struCardCond.size(), null, null);
        if (m_lSetMultiCardCfgHandle == -1) {
            log.info("建立下发卡长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立下发卡长连接成功！");
        }
        HCNetSDK.NET_DVR_CARD_RECORD[] struCardRecord = (HCNetSDK.NET_DVR_CARD_RECORD[]) new HCNetSDK.NET_DVR_CARD_RECORD().toArray(staffEntityList.size());
        HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        IntByReference pInt = new IntByReference(0);
        setCardsProgress = 0;
        for (int i = 0; i < staffEntityList.size(); i++) {
            struCardRecord[i].read();
            struCardRecord[i].dwSize = struCardRecord[i].size();
            for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++) {
                struCardRecord[i].byCardNo[j] = 0;
            }
            System.arraycopy(staffEntityList.get(i).getStaffCardNumber().getBytes(), 0, struCardRecord[i].byCardNo, 0, staffEntityList.get(i).getStaffCardNumber().getBytes().length);
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
                byte[] strCardName = staffEntityList.get(i).getStaffName().getBytes("GBK");  //姓名
                for (int j = 0; j < HCNetSDK.NAME_LEN; j++) {
                    struCardRecord[i].byName[j] = 0;
                }
                System.arraycopy(strCardName, 0, struCardRecord[i].byName, 0, strCardName.length);
            }
            if (iCharEncodeType == 6) {
                byte[] strCardName = staffEntityList.get(i).getStaffName().getBytes("UTF-8");  //姓名
                for (int j = 0; j < HCNetSDK.NAME_LEN; j++) {
                    struCardRecord[i].byName[j] = 0;
                }
                System.arraycopy(strCardName, 0, struCardRecord[i].byName, 0, strCardName.length);
            }
            struCardRecord[i].write();
            dwState = HikSingleton.hik.NET_DVR_SendWithRecvRemoteConfig(m_lSetMultiCardCfgHandle, struCardRecord[i].getPointer(), struCardRecord[i].size(), struCardStatus.getPointer(), struCardStatus.size(), pInt);
            struCardStatus.read();
            if (dwState == -1) {
                log.info("NET_DVR_SendWithRecvRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FAILED) {
                log.info("下发卡失败, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                //可以继续下发下一个
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_EXCEPTION) {
                log.info("下发卡异常, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 错误码：" + struCardStatus.dwErrorCode);
                //异常是长连接异常，不能继续下发后面的数据，需要重新建立长连接
                break;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_SUCCESS) {
                if (struCardStatus.dwErrorCode != 0) {
                    log.info("下发卡失败,错误码" + struCardStatus.dwErrorCode + ", 卡号：" + new String(struCardStatus.byCardNo).trim());
                } else {
                    log.info("下发卡成功, 卡号: " + new String(struCardStatus.byCardNo).trim() + ", 状态：" + struCardStatus.byStatus);
                }
                //可以继续下发下一个
            } else {
                log.info("其他状态：" + dwState);
            }
            setCardsProgress++;
            Thread.currentThread().setName("卡号下发：" + setCardsProgress + "/" + setCardsTotal);
        }
        setCardsProgress = 0;
        setCardsTotal = 0;
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lSetMultiCardCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }

    /*
     * 删除卡号
     * */
    public void DelOneCard(EquipmentEntity equipmentEntity, StaffEntity staffEntity) throws InterruptedException {
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = 1;  //下发一张
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lDelCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_DEL_CARD, ptrStruCond, struCardCond.size(), null, null);
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
        for (int i = 0; i < staffEntity.getStaffCardNumber().length(); i++) {
            struCardData.byCardNo[i] = staffEntity.getStaffCardNumber().getBytes()[i];
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
     * 批量删除卡号
     * */
    public void DelMultiCard(EquipmentEntity equipmentEntity, List<StaffEntity> staffEntityList) throws InterruptedException {
        delCardsTotal = staffEntityList.size();
        HCNetSDK.NET_DVR_CARD_COND struCardCond = new HCNetSDK.NET_DVR_CARD_COND();
        struCardCond.read();
        struCardCond.dwSize = struCardCond.size();
        struCardCond.dwCardNum = staffEntityList.size();  //删除多张
        struCardCond.write();
        Pointer ptrStruCond = struCardCond.getPointer();
        m_lDelMultiCardCfgHandle = HikSingleton.hik.NET_DVR_StartRemoteConfig(equipmentEntity.getlUserID(), HCNetSDK.NET_DVR_DEL_CARD, ptrStruCond, struCardCond.size(), null, null);
        if (m_lDelMultiCardCfgHandle == -1) {
            log.info("建立删除卡长连接失败，错误码为" + HikSingleton.hik.NET_DVR_GetLastError());
            return;
        } else {
            log.info("建立删除卡长连接成功！");
        }
        HCNetSDK.NET_DVR_CARD_SEND_DATA[] struCardData = (HCNetSDK.NET_DVR_CARD_SEND_DATA[]) new HCNetSDK.NET_DVR_CARD_SEND_DATA().toArray(staffEntityList.size());
        HCNetSDK.NET_DVR_CARD_STATUS struCardStatus = new HCNetSDK.NET_DVR_CARD_STATUS();
        struCardStatus.read();
        struCardStatus.dwSize = struCardStatus.size();
        struCardStatus.write();
        IntByReference pInt = new IntByReference(0);
        delCardsProgress = 0;
        for (int i = 0; i < staffEntityList.size(); i++) {
            struCardData[i].read();
            struCardData[i].dwSize = struCardData[i].size();
            for (int j = 0; j < HCNetSDK.ACS_CARD_NO_LEN; j++) {
                struCardData[i].byCardNo[j] = 0;
            }
            for (int j = 0; j < staffEntityList.get(i).getStaffCardNumber().length(); j++) {
                struCardData[i].byCardNo[j] = staffEntityList.get(i).getStaffCardNumber().getBytes()[j];
            }
            struCardData[i].write();
            dwState = HikSingleton.hik.NET_DVR_SendWithRecvRemoteConfig(m_lDelMultiCardCfgHandle, struCardData[i].getPointer(), struCardData[i].size(), struCardStatus.getPointer(), struCardStatus.size(), pInt);
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
//                continue;
            } else if (dwState == HCNetSDK.NET_SDK_CONFIG_STATUS_FINISH) {
                log.info("删除卡完成");
                break;
            }
            delCardsProgress++;
            Thread.currentThread().setName("卡号删除：" + delCardsProgress + "/" + delCardsTotal);
        }
        delCardsProgress = 0;
        delCardsTotal = 0;
        if (!HikSingleton.hik.NET_DVR_StopRemoteConfig(m_lDelMultiCardCfgHandle)) {
            log.info("NET_DVR_StopRemoteConfig接口调用失败，错误码：" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("NET_DVR_StopRemoteConfig接口成功");
        }
    }
}
