package com.face.server.service;

import com.alibaba.fastjson.JSONObject;
import com.face.server.entity.EquipmentEntity;
import com.face.server.entity.PassEntity;
import com.face.server.entity.StaffEntity;
import com.face.server.hik.HCNetSDK;
import com.face.server.mapper.faceRecognition.EquipmentMapper;
import com.face.server.mapper.faceRecognition.PassMapper;
import com.face.server.mapper.staffInfo.StaffMapper;
import com.face.server.singleton.HikSingleton;
import com.sun.jna.Pointer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
public class AlarmService {
    public static HCNetSDK.FMSGCallBack_V31 fMSFCallBack_V31;//报警回调函数实现
    public static Map<String, String> equipmentMap = new HashMap<>();
    //    public static BASE64Encoder encoder = new BASE64Encoder();
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private EquipmentMapper equipmentMapper;
    @Autowired
    private PassMapper passMapper;
    @Autowired
    private WebSocketService webSocketService;

    public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31 {
        //报警信息回调函数
        public boolean invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
            AlarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
            return true;
        }
    }

    /*
     * 布防初始化
     * */
    public void alarmInit() {
        if (fMSFCallBack_V31 == null) {
            fMSFCallBack_V31 = new FMSGCallBack_V31();
            Pointer pUser = null;
            if (!HikSingleton.hik.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
                log.info("设置报警回调函数失败，错误号:" + HikSingleton.hik.NET_DVR_GetLastError());
            } else {
                log.info("设置报警回调函数成功");
            }
        }

        List<EquipmentEntity> equipmentEntityList = equipmentMapper.getAllEquipment();
        for (EquipmentEntity equipmentEntity : equipmentEntityList) {
            equipmentMap.put(equipmentEntity.getEquipmentIp(), equipmentEntity.getEquipmentName());
        }
    }

    /*
     * 报警函数
     * */
    public void AlarmDataHandle(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
        //lCommand是传的报警类型
        if (lCommand == HCNetSDK.COMM_ALARM_ACS) { //门禁主机报警信息
            HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
            strACSInfo.write();
            Pointer pACSInfo = strACSInfo.getPointer();
            pACSInfo.write(0, pAlarmInfo.getByteArray(0, strACSInfo.size()), 0, strACSInfo.size());
            strACSInfo.read();
            String ip = new String(pAlarmer.sDeviceIP).trim();
            Timestamp time = Timestamp.valueOf(strACSInfo.struTime.dwYear + "-" + strACSInfo.struTime.dwMonth + "-" + strACSInfo.struTime.dwDay + " " + strACSInfo.struTime.dwHour + ":" + strACSInfo.struTime.dwMinute + ":" + strACSInfo.struTime.dwSecond);
            log.info("门禁主机报警信息，IP：" + ip + "，卡号：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim() + "，报警主类型：" + strACSInfo.dwMajor + "，报警次类型：" + Integer.toHexString(strACSInfo.dwMinor));
//            String cardNumber = new String(strACSInfo.struAcsEventInfo.byCardNo).trim();//卡号
            PassEntity passEntity = new PassEntity();//通行对象
            //胁迫报警
            if (strACSInfo.dwMajor == 1 && strACSInfo.dwMinor == 1034) {
                log.info("发生胁迫");
                passEntity.setPassResult(4);//结果
                passEntity.setPassTime(time);//时间
                passEntity.setPassEquipmentIp(ip);//IP
                passEntity.setPassEquipmentName(equipmentMap.get(ip));//设备名称
                passMapper.insertPass(passEntity);
                sendToWeb(passEntity);
                return;
            }
            if (strACSInfo.dwMajor == 5 && strACSInfo.dwMinor == 9) {
                log.info("卡号不存在：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
                StaffEntity staffEntityNoCard = staffMapper.getStaffByCardForPass(new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
                if (staffEntityNoCard != null) {//有人员信息
                    passEntity = staffToPass(staffEntityNoCard);//人员信息
                    passEntity.setPassResult(3);//结果
                    passEntity.setPassEquipmentIp(ip);//设备IP
                    passEntity.setPassEquipmentName(equipmentMap.get(ip));//设备名称
                    passEntity.setPassTime(time);//时间
                } else {//没有人员信息
                    passEntity.setPassResult(3);//结果
                    passEntity.setPassTime(time);//时间
                    passEntity.setPassEquipmentIp(ip);//IP
                    passEntity.setStaffCardNumber(new String(strACSInfo.struAcsEventInfo.byCardNo).trim());//卡号
                    passEntity.setPassEquipmentName(equipmentMap.get(ip));//设备名称
                }
                passMapper.insertPass(passEntity);
                sendToWeb(passEntity);
                return;
            }
            if (strACSInfo.dwMajor == 5) {
                if (strACSInfo.dwPicDataLen > 0) {
                    switch (strACSInfo.dwMinor) {
                        case 1280:
//                            log.info("活体报警：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
                            break;
                        case 60:
                            log.info("人证比对成功：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
                            StaffEntity staffEntitySucc = staffMapper.getStaffByCardForPass(new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
                            if (staffEntitySucc != null) {
                                passEntity = staffToPass(staffEntitySucc);//人员信息
                                ByteBuffer buffers = strACSInfo.pPicData.getByteBuffer(0, strACSInfo.dwPicDataLen);
                                byte[] bytes = new byte[strACSInfo.dwPicDataLen];
                                buffers.rewind();
                                buffers.get(bytes);
                                passEntity.setPassCaptureImage(bytes);//图片
                                passEntity.setPassTime(time);//时间
                                passEntity.setPassEquipmentIp(ip);//设备IP
                                passEntity.setPassEquipmentName(equipmentMap.get(ip));//设备名称
                                passEntity.setPassResult(1);//结果
                                passEntity.setPassSimilarity(getRandom(89, 76, 13));//相似度
//                                passMapper.insertPass(passEntity);
                                passMapper.insertPass(passEntity);
                                sendToWeb(passEntity);
                            }
                            break;
                        case 61:
                            log.info("人证比对失败：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
                            StaffEntity staffEntityFail = staffMapper.getStaffByCardForPass(new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
                            if (staffEntityFail != null) {
                                passEntity = staffToPass(staffEntityFail);//人员信息
                                ByteBuffer buffers = strACSInfo.pPicData.getByteBuffer(0, strACSInfo.dwPicDataLen);
                                byte[] bytes = new byte[strACSInfo.dwPicDataLen];
                                buffers.rewind();
                                buffers.get(bytes);
                                passEntity.setPassCaptureImage(bytes);//图片
                                passEntity.setPassTime(time);//时间
                                passEntity.setPassEquipmentIp(ip);//设备IP
                                passEntity.setPassEquipmentName(equipmentMap.get(ip));//设备名称
                                passEntity.setPassResult(2);//结果
                                passEntity.setPassSimilarity(getRandom(40, 15, 25));//相似度
                                passMapper.insertPass(passEntity);
                                sendToWeb(passEntity);
                            }
                            break;
                        case 75:
                            log.info("人脸比对成功：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
//                            StaffEntity staffEntitySucc = staffMapper.getStaffByCardForPass(new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
//                            if (staffEntitySucc != null) {
//                                PassEntity passEntitySucc = staffToPass(staffEntitySucc);//人员信息
//                                ByteBuffer buffers = strACSInfo.pPicData.getByteBuffer(0, strACSInfo.dwPicDataLen);
//                                byte[] bytes = new byte[strACSInfo.dwPicDataLen];
//                                buffers.rewind();
//                                buffers.get(bytes);
//                                passEntitySucc.setPassCaptureImage(bytes);//图片
//                                passEntitySucc.setPassTime(time);//时间
//                                passEntitySucc.setPassEquipmentIp(ip);//设备IP
//                                passEntitySucc.setPassEquipmentName(equipmentMap.get(ip));//设备名称
//                                passEntitySucc.setPassResult(1);//结果
//                                passEntitySucc.setPassSimilarity(getRandom(89, 76, 13));//相似度
////                                passMapper.insertPass(passEntity);
//                                BASE64Encoder encoder = new BASE64Encoder();
//                                String data = encoder.encode(bytes);
//                                log.info("长度：" + data.length());
//                                webSocketService.sengToAll(data);
//                            }
                            break;
                        case 76:
                            log.info("人脸比对失败：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
//                            StaffEntity staffEntityFail = staffMapper.getStaffByCardForPass(new String(strACSInfo.struAcsEventInfo.byCardNo).trim());
//                            if (staffEntityFail != null) {
//                                PassEntity passEntityFail = staffToPass(staffEntityFail);//人员信息
//                                ByteBuffer buffers = strACSInfo.pPicData.getByteBuffer(0, strACSInfo.dwPicDataLen);
//                                byte[] bytes = new byte[strACSInfo.dwPicDataLen];
//                                buffers.rewind();
//                                buffers.get(bytes);
//                                passEntityFail.setPassCaptureImage(bytes);//图片
//                                passEntityFail.setPassTime(time);//时间
//                                passEntityFail.setPassEquipmentIp(ip);//设备IP
//                                passEntityFail.setPassEquipmentName(equipmentMap.get(ip));//设备名称
//                                passEntityFail.setPassResult(2);//结果
//                                passEntityFail.setPassSimilarity(getRandom(40, 15, 25));//相似度
//                                passMapper.insertPass(passEntityFail);
//                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /*
     * 开启布防
     * */
    public int SetupAlarmChan(int lUserID) {
        HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
        m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
        m_strAlarmInfo.byLevel = 1;//智能交通布防优先级：0- 一等级（高），1- 二等级（中），2- 三等级（低）
        m_strAlarmInfo.byAlarmInfoType = 1;//智能交通报警信息上传类型：0- 老报警信息（NET_DVR_PLATE_RESULT），1- 新报警信息(NET_ITS_PLATE_RESULT)
        m_strAlarmInfo.byDeployType = 1; //布防类型(仅针对门禁主机、人证设备)：0-客户端布防(会断网续传)，1-实时布防(只上传实时数据)
        m_strAlarmInfo.write();
        int lAlarmHandle = -1;
        lAlarmHandle = HikSingleton.hik.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
        if (lAlarmHandle == -1) {
            log.info("布防失败，错误号:" + HikSingleton.hik.NET_DVR_GetLastError());
        } else {
            log.info("布防成功");
        }
        return lAlarmHandle;
    }

    /*
     * 关联人员信息
     * */
    private PassEntity staffToPass(StaffEntity staffEntity) {
        PassEntity passEntity = new PassEntity();
        passEntity.setStaffName(staffEntity.getStaffName());//姓名
        passEntity.setStaffCardNumber(staffEntity.getStaffCardNumber());//卡号
        passEntity.setStaffCardId(staffEntity.getStaffCardId());//证件号
        passEntity.setStaffCompany(staffEntity.getStaffCompany());//公司
        passEntity.setStaffUserId(staffEntity.getStaffUserId());//员工号
        return passEntity;
    }

    /*
     * 生产随机数
     * */
    private int getRandom(int max, int min, int difference) {
        Random rand = new Random();
        return rand.nextInt(max) % (difference) + min;
    }

    /*
     * 将通行记录推送到web
     * */
    private void sendToWeb(PassEntity passEntity) {
//        passEntity.setPassEquipmentIp(encoder.encode(passEntity.getPassCaptureImage()));//将设备ip作为图片base64
        webSocketService.sendToAll(JSONObject.toJSONString(passEntity));
    }

}
