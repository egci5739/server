package com.face.server.service;

import com.face.server.entity.EquipmentEntity;
import com.face.server.entity.StaffEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Slf4j
@Service
public class SynchronizationService {
//    @Autowired
//    private CardService cardService;
//    @Autowired
//    private FaceService faceService;

//    /*
//     * 下发单人
//     * */
////    @Async
//    public void setOneStaff(EquipmentEntity equipmentEntity, StaffEntity staffEntity) throws UnsupportedEncodingException, InterruptedException {
//        cardService.SetOneCard(equipmentEntity, staffEntity);
//        faceService.SetOneFace(equipmentEntity, staffEntity);
//    }
//
//    /*
//     * 单人删除
//     * */
//    public void delOneStaff(EquipmentEntity equipmentEntity, StaffEntity staffEntity) throws InterruptedException {
//        cardService.DelOneCard(equipmentEntity, staffEntity);
//    }


    /*
     * 下发多人
     * */
    @Async
    public void synchronization(EquipmentEntity equipmentEntity, List<StaffEntity> setStaffEntityList, List<StaffEntity> delStaffEntityList) throws UnsupportedEncodingException, InterruptedException {
        log.info("下发任务ID：" + Thread.currentThread().getId());
        equipmentEntity.setSynchronizationTask(Thread.currentThread().getId());
        CardService cardService = new CardService();
        if (delStaffEntityList.size() > 0) {
            cardService.DelMultiCard(equipmentEntity, delStaffEntityList);
        }
        if (setStaffEntityList.size() > 0) {
            FaceService faceService = new FaceService();
            cardService.SetMultiCard(equipmentEntity, setStaffEntityList);
            faceService.SetMultiFace(equipmentEntity, setStaffEntityList);
        }
        equipmentEntity.setSynchronizationTask(0);
    }

//    /*
//     * 多人删除
//     * */
//    @Async
//    public void delMultiStaffs(EquipmentEntity equipmentEntity, List<StaffEntity> staffEntityList) throws InterruptedException {
//        CardService cardService = new CardService();
//        cardService.DelMultiCard(equipmentEntity, staffEntityList);
//    }

    @Async
    public void test() {
        Thread.currentThread().setName("666");
        log.info("线程名称:" + Thread.currentThread().getName() + "；线程ID：" + Thread.currentThread().getId());
    }
}
