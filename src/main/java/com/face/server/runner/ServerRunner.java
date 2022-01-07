package com.face.server.runner;

import com.alibaba.fastjson.JSONObject;
import com.face.server.entity.EquipmentEntity;
import com.face.server.mapper.staffInfo.StaffMapper;
import com.face.server.service.*;
import com.face.server.singleton.EquipmentSingleton;
import com.face.server.singleton.HikSingleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ServerRunner implements ApplicationRunner {
    @Autowired
    private LoginService loginService;
    @Autowired
    private EquipmentSingleton equipmentSingleton;
    @Autowired
    private CardService cardService;
    @Autowired
    private StaffMapper staffMapper;
    @Autowired
    private FaceService faceService;
    @Autowired
    private SubtractionService subtractionService;
    @Autowired
    private SynchronizationService synchronizationService;
    @Autowired
    private AlarmService alarmService;

    @Override
    public void run(ApplicationArguments args) throws UnsupportedEncodingException, InterruptedException {


//        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
//        ThreadInfo info = tmx.getThreadInfo(7777777);
//        log.info(info.getThreadName());


        LocalDateTime localDateTime = LocalDateTime.now();
        log.info("启动时间:" + localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        /*
         * 初始化SDK
         * */
        if (HikSingleton.init()) {
            log.info("SDK初始化成功");
        } else {
            log.info("SDK初始化失败：" + HikSingleton.hik.NET_DVR_GetLastError());
        }
        /*
         * 初始化布防
         * */
        alarmService.alarmInit();

        /*
         * 初始化设备信息
         * */
        try {
            if (equipmentSingleton.getAll()) {
                log.info("获取设备初始成功：" + EquipmentSingleton.equipmentEntityList.toString());
                /*
                 * 登录设备
                 * i很重要
                 * 注意布防逻辑
                 * */
                int i = 0;
                for (EquipmentEntity equipmentEntity : EquipmentSingleton.equipmentEntityList) {
                    equipmentEntity.setIndex(i);
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
                        equipmentEntity.setIsAlarm(0);
                    }
                    log.info("设备登录详情：" + equipmentEntity);
                    i++;
                }
            } else {
                log.info("获取设备失败：数量为0");
            }
        } catch (Exception e) {
            log.info("获取设备失败：" + e.getMessage());
        }
    }
}
