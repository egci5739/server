package com.face.server.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.face.server.entity.EquipmentEntity;
import com.face.server.entity.PassEntity;
import com.face.server.entity.StaffEntity;
import com.face.server.mapper.faceRecognition.PassMapper;
import com.face.server.mapper.staffInfo.StaffMapper;
import com.face.server.service.*;
import com.face.server.singleton.EquipmentSingleton;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.websocket.server.PathParam;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class ServerController {
    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private JsonService jsonService;

    @Autowired
    private CardService cardService;

    @Autowired
    private FaceService faceService;

    @Autowired
    private SubtractionService subtractionService;

    @Autowired
    private HikService hikService;

    @Autowired
    private SynchronizationService synchronizationService;

    @Autowired
    private PassMapper passMapper;

    @RequestMapping("/")
    public String getEquipment() {
        return "index";
    }

    /*
     * 人员操作
     * 下发、删除
     * */
    @RequestMapping("/staffOperation")
    private String staffOperation() {
        return "page/staffOperation";
    }

    //按条件获取人员信息
    @RequestMapping(value = "/staffOperation/getData", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String getStaffOperationData(@RequestBody JSONObject param) {
        log.info("查询人员：" + param.toJSONString());
        List<StaffEntity> staffEntityList = null;
        if (!param.getString("staffCardNumber").equals("")) {
            staffEntityList = staffMapper.getStaffByCard(param.getString("staffCardNumber"));
        } else if (!param.getString("staffName").equals("")) {
            staffEntityList = staffMapper.getStaffByName(param.getString("staffName"));
        } else if (!param.getString("staffCardId").equals("")) {
            staffEntityList = staffMapper.getStaffByCardId(param.getString("staffCardId"));
        } else if (!param.getString("staffUserId").equals("")) {
            staffEntityList = staffMapper.getStaffByUserId(param.getString("staffUserId"));
        }
        assert staffEntityList != null;
        for (StaffEntity staffEntity : staffEntityList) {
            staffEntity.setStaffImage(null);
        }
        return jsonService.toStaffJson(0, "", staffEntityList);
    }

    //单人下发
    @RequestMapping(value = "/staffOperation/setStaff", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String setStaff(@RequestBody JSONObject param) throws UnsupportedEncodingException, InterruptedException {
        log.info("单人下发：" + param.toJSONString());
        StaffEntity staffEntity = staffMapper.getStaffByNum(param.getString("staffCardNumber"));
        for (EquipmentEntity equipmentEntity : EquipmentSingleton.equipmentEntityList) {
            if (equipmentEntity.getlUserID() != -1) {
                cardService.SetOneCard(equipmentEntity, staffEntity);
                faceService.SetOneFace(equipmentEntity, staffEntity);
            }
        }
        return jsonService.toStringJson(0, "单人下发", "下发完成");
    }

    //多人下发
    @RequestMapping(value = "/staffOperation/setStaffs", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String setStaffs(@RequestBody JSONArray params) throws UnsupportedEncodingException, InterruptedException {
        log.info("多人下发：" + params.toJSONString());
        List<StaffEntity> staffEntityListNoImage = JSON.parseArray(params.toJSONString(), StaffEntity.class);
//        StringBuilder cards = new StringBuilder();
//        for (StaffEntity staffEntity : staffEntityListNoImage) {
//            cards.append(staffEntity.getStaffCardNumber()).append(",");
//        }
        String cards = subtractionService.staffListToString(staffEntityListNoImage);
        List<StaffEntity> staffEntityListWithImage = staffMapper.getStaffByCards(cards.substring(0, cards.length() - 1));
        for (EquipmentEntity equipmentEntity : EquipmentSingleton.equipmentEntityList) {
            if (equipmentEntity.getlUserID() != -1) {
                for (StaffEntity staffEntity : staffEntityListWithImage) {
                    if (staffEntity.getStaffImage() == null || staffEntity.getStaffName() == null || "0".equals(String.valueOf(staffEntity.getStaffCardNumber()))) {//姓名、卡号、照片有一个为空
                        log.info("人员信息不全：" + staffEntity);
                    } else {
                        cardService.SetOneCard(equipmentEntity, staffEntity);
                        faceService.SetOneFace(equipmentEntity, staffEntity);
                    }
                    Thread.sleep(100);
                }
            }
        }
        return jsonService.toStringJson(0, "多人下发", "下发完成");
    }

    //单人删除
    @RequestMapping(value = "/staffOperation/delStaff", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String delStaff(@RequestBody JSONObject param) throws InterruptedException {
        log.info("单人删除：" + param.toJSONString());
        StaffEntity staffEntity = new StaffEntity();
        staffEntity.setStaffCardNumber(param.getString("staffCardNumber"));
        for (EquipmentEntity equipmentEntity : EquipmentSingleton.equipmentEntityList) {
            if (equipmentEntity.getlUserID() != -1) {
                cardService.DelOneCard(equipmentEntity, staffEntity);
            }
        }
        return jsonService.toStringJson(0, "单人删除", "删除完成");
    }

    //多人删除
    @RequestMapping(value = "/staffOperation/delStaffs", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String delStaffs(@RequestBody JSONArray params) throws InterruptedException {
        log.info("多人删除：" + params.toJSONString());
        List<StaffEntity> staffEntityList = JSON.parseArray(params.toJSONString(), StaffEntity.class);
        for (EquipmentEntity equipmentEntity : EquipmentSingleton.equipmentEntityList) {
            if (equipmentEntity.getlUserID() != -1) {
                for (StaffEntity staffEntity : staffEntityList) {
                    cardService.DelOneCard(equipmentEntity, staffEntity);
                    Thread.sleep(100);
                }
            }
        }
        return jsonService.toStringJson(0, "多人删除", "删除完成");
    }

    /*
     * 同步管理
     * staffSynchronization
     * */
    @RequestMapping(value = "/staffSynchronization", method = RequestMethod.GET)
    private String staffSynchronization(Model model) {
        model.addAttribute("equipmentList", EquipmentSingleton.equipmentEntityList);
        return "page/staffSynchronization";
    }

    //按条件获取设备人员信息
    @RequestMapping(value = "/staffSynchronization/getData", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String getStaffSynchronizationData(@RequestBody JSONObject param) {
        log.info("按条件获取设备人员信息：" + param.toJSONString());
        List<StaffEntity> staffEntityList = new ArrayList<>();
        if (Integer.parseInt(param.getString("dataType")) == 0) {
            staffEntityList = cardService.GetAllCard(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("equipmentIndex"))));
        }
        return jsonService.toStaffJson(0, "", staffEntityList);
    }

    /*
     * 设备管理
     * */
    @RequestMapping(value = "/equipment", method = RequestMethod.GET)
    private String equipment(Model model) {
        model.addAttribute("equipmentList", EquipmentSingleton.equipmentEntityList);
        return "page/equipment";
    }

    //获取设备数据
    @RequestMapping(value = "/equipment/getData")
    @ResponseBody
    private String getEquipmentData() {
        log.info("获取设备列表");
        return jsonService.toEquipmentJson(0, "", EquipmentSingleton.equipmentEntityList);
    }

    //获取设备卡数量
    @RequestMapping(value = "/equipment/getCardNum", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String getCardNum(@RequestBody JSONObject param) {
        log.info("获取设备卡数量：" + param.toJSONString());
        if (EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))).getIsLogin() == 0) {
            return jsonService.toStringJson(0, "卡数量", "设备离线");
        }
//        List<StaffEntity> staffEntityList = cardService.GetAllCard(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))));
//        if (staffEntityList != null) return jsonService.toStringJson(0, "卡数量", String.valueOf(staffEntityList.size()));
//        else return jsonService.toStringJson(0, "卡数量", "获取卡数量异常");
        int num = hikService.getEquipmentCardNums(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))));
        return jsonService.toStringJson(0, "卡数量", String.valueOf(num));
    }

    //设备人员同步
    @RequestMapping(value = "/equipment/synchronization", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String synchronization(@RequestBody JSONObject param) {
        try {
            log.info("设备人员同步：" + param.toJSONString());
            if (EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))).getIsLogin() == 0) {
                return jsonService.toStringJson(0, "设备人员同步", "设备离线");
            }

            if (EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))).getSynchronizationTask() > 0) {
                ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
                ThreadInfo info = tmx.getThreadInfo(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))).getSynchronizationTask());
                log.info("任务ID：" + info.getThreadId() + "任务名称：" + info.getThreadName() + "状态：" + info.getThreadState());
                return jsonService.toStringJson(0, "设备同步", info.getThreadName());
            }
//        if (cardService.getSetCardsProgress() != 0 || cardService.getDelCardsProgress() != 0 || faceService.getSetFacesProgress() != 0) {
//            if (cardService.getSetCardsProgress() != 0) {
//                return jsonService.toStringJson(0, "设备人员同步", "卡号下发任务进行中：" + cardService.getSetCardsProgress() + "/" + cardService.getSetCardsTotal());
//            } else if (faceService.getSetFacesProgress() != 0) {
//                return jsonService.toStringJson(0, "设备人员同步", "人脸下发任务进行中：" + faceService.getSetFacesProgress() + "/" + faceService.getSetFacesTotal());
//            } else if (cardService.getDelCardsProgress() != 0) {
//                return jsonService.toStringJson(0, "设备人员同步", "人员删除任务进行中：" + cardService.getDelCardsProgress() + "/" + cardService.getDelCardsTotal());
//            }
//        }
            List<StaffEntity> equipment = cardService.GetAllCard(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))));
            List<StaffEntity> database = staffMapper.getAll();//注意这里
            //下发
            List<String> set = subtractionService.complement(database, equipment);
            List<StaffEntity> setStaffEntityList = new ArrayList<>();
            if (set.size() > 0) {
                String setCardList = String.join(",", set);
                setStaffEntityList = staffMapper.getStaffByCards(setCardList);
//            cardService.SetMultiCard(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))), staffEntityList);
//            faceService.SetMultiFace(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))), staffEntityList);
            }

            //删除
            List<StaffEntity> delStaffEntityList = new ArrayList<>();
            List<String> del = subtractionService.complement(equipment, database);
            if (del.size() > 0) {
                for (String card : del) {
                    StaffEntity staffEntity = new StaffEntity();
                    staffEntity.setStaffCardNumber(card);
                    delStaffEntityList.add(staffEntity);
                }
//            cardService.DelMultiCard(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))), staffEntityList);
//            synchronizationService.delMultiStaffs(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))), staffEntityList);
            }
            if (setStaffEntityList.size() > 0 || delStaffEntityList.size() > 0) {
                synchronizationService.synchronization(EquipmentSingleton.equipmentEntityList.get(Integer.parseInt(param.getString("index"))), setStaffEntityList, delStaffEntityList);
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return jsonService.toStringJson(0, "设备同步", "同步任务启动成功");
    }

    /*
     * websocket
     * 实时监控
     * */
    @RequestMapping("/monitor")
    public String monitor() {
        return "page/monitor";
    }

    /*
     * 历史通行数据
     * */
    @RequestMapping("/history")
    public String history(Model model) {
        model.addAttribute("equipmentList", EquipmentSingleton.equipmentEntityList);
        return "page/history";
    }

    //获取通行历史数据
    @RequestMapping(value = "/history/getHistoryData", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    private String getHistoryData(@RequestBody JSONObject param) {
        log.info("历史通行数据：" + param.toJSONString());
        List<PassEntity> passEntityList = new ArrayList<>();
        Timestamp startTime = Timestamp.valueOf(param.getString("startTime"));
        Timestamp endTime = Timestamp.valueOf(param.getString("endTime"));
        String passEquipmentIp = param.getString("passEquipmentIp");
        int passResult = Integer.parseInt(param.getString("passResult"));
        passEntityList = passMapper.getData(passEquipmentIp, passResult, startTime, endTime);
        return jsonService.toPassJson(0, "", passEntityList);
    }
}