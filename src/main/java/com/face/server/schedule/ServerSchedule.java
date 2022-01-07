package com.face.server.schedule;

import com.face.server.entity.EquipmentEntity;
import com.face.server.entity.StaffStatEntity;
import com.face.server.mapper.staffInfo.StaffStatMapper;
import com.face.server.service.*;
import com.face.server.singleton.EquipmentSingleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.List;

@Slf4j
@Component
public class ServerSchedule {
    @Autowired
    private StaffStatMapper staffStatMapper;

    @Autowired
    private SendInfoForOnguardService sendInfoForOnguardService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private AlarmService alarmService;

    /*
     * 定时获取onGuard对接库变动信息
     * */
    @Scheduled(cron = "0/15 * * * * *")
    public void onGuardSchedule() throws UnsupportedEncodingException, InterruptedException {
        List<StaffStatEntity> staffStatEntityList = staffStatMapper.getAll();
        log.info("人员信息：" + staffStatEntityList.toString());
        for (EquipmentEntity equipmentEntity : EquipmentSingleton.equipmentEntityList) {
            if (equipmentEntity.getlUserID() != -1) {
                for (StaffStatEntity staffStatEntity : staffStatEntityList) {
                    if (staffStatEntity.getType() == 1) {//下发
                        if (staffStatEntity.getImage() == null || staffStatEntity.getName() == null || "0".equals(String.valueOf(staffStatEntity.getCard()))) {//姓名、卡号、照片有一个为空
                            log.info("人员信息不全：" + staffStatEntity);
                        } else {
                            sendInfoForOnguardService.setCard(equipmentEntity.getlUserID(), staffStatEntity);
                            sendInfoForOnguardService.setFace(equipmentEntity.getlUserID(), staffStatEntity);
                        }
                    } else if (staffStatEntity.getType() == 2) {//删除
                        if ("0".equals(String.valueOf(staffStatEntity.getCard()))) {
                            log.info("卡号信息不全：" + staffStatEntity);
                        } else {
                            sendInfoForOnguardService.delCard(equipmentEntity.getlUserID(), staffStatEntity);
                        }
                    } else {//未知操作
                        log.info("未知操作：" + staffStatEntity);
                    }
                    //删除处理后的数据，2种情况
                    if ("0".equals(String.valueOf(staffStatEntity.getCard())) && "0".equals(String.valueOf(staffStatEntity.getEmpId()))) {//无卡号、无EMPID，不删
                        log.info("卡号和EMPID不全");
                    } else if ("0".equals(String.valueOf(staffStatEntity.getCard()))) {//无卡号，根据EMPID删除
                        staffStatMapper.deleteByEMPID(staffStatEntity.getEmpId());
                    } else {//无EMPID，根据卡号删除
                        staffStatMapper.deleteByCard(staffStatEntity.getCard());
                    }
                    Thread.sleep(100);
                }
            }
        }
    }

    /*
     * 定时更新设备状态
     * 网络通断和布防
     * 这里逻辑要清晰
     * */
    @Scheduled(cron = "30/10 * * * * ? ")
    public void equipmentStatusSchedule() {
        for (EquipmentEntity equipmentEntity : EquipmentSingleton.equipmentEntityList) {
            if (ping(equipmentEntity.getEquipmentIp())) { //网络正常
                if (equipmentEntity.getlUserID() == -1) {//未登录过
                    equipmentEntity.setlUserID(loginService.Login(equipmentEntity));
                    if (equipmentEntity.getlUserID() > -1) {//登录成功
                        equipmentEntity.setIsLogin(1);
                        if (alarmService.SetupAlarmChan(equipmentEntity.getlUserID()) > -1) {//布防成功
                            equipmentEntity.setIsAlarm(1);
                        } else {//布防失败
                            equipmentEntity.setIsAlarm(0);
                        }
                    } else {//登录失败
                        equipmentEntity.setIsLogin(0);
                    }
                } else {//已有登录信息
                    equipmentEntity.setIsLogin(1);
                    if (equipmentEntity.getIsAlarm() == 0) {//未布防过
                        if (alarmService.SetupAlarmChan(equipmentEntity.getlUserID()) > -1) {//布防成功
                            equipmentEntity.setIsAlarm(1);
                        } else {//布防失败
                            equipmentEntity.setIsAlarm(0);
                        }
                    }
                }
            } else {//网络异常
                equipmentEntity.setIsLogin(0);
            }
            log.info("设备状态：" + equipmentEntity);
        }
    }

    /*
     * 判断设备网络状态
     * */
    public static boolean ping(String ipAddress) {
        int timeOut = 3000;  //超时应该在3钞以上
        boolean status;
        try {
            status = InetAddress.getByName(ipAddress).isReachable(timeOut);
        } catch (IOException e) {
            status = false;
        }
        // 当返回值是true时，说明host是可用的，false则不可。
        return status;
    }
}
