package com.face.server.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.face.server.entity.EquipmentEntity;
import com.face.server.entity.PassEntity;
import com.face.server.entity.StaffEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class JsonService {
    /*
     * 生成人员数据
     * */
    public String toStaffJson(int code, String msg, List<StaffEntity> staffEntityList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("msg", msg);
        jsonObject.put("count", staffEntityList.size());
        JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(staffEntityList));
        jsonObject.put("data", jsonArray);
        return jsonObject.toJSONString();
    }

    /*
     * 生成设备数据
     * */
    public String toEquipmentJson(int code, String msg, List<EquipmentEntity> equipmentEntityList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("msg", msg);
        jsonObject.put("count", equipmentEntityList.size());
        JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(equipmentEntityList));
        jsonObject.put("data", jsonArray);
        return jsonObject.toJSONString();
    }

    /*
     * 生成历史通行数据
     * */
    public String toPassJson(int code, String msg, List<PassEntity> passEntityList) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (PassEntity passEntity : passEntityList) {
            passEntity.setTime(simpleDateFormat.format(passEntity.getPassTime()));
            switch (passEntity.getPassResult()) {
                case 1:
                    passEntity.setResult("比对成功");
                    break;
                case 2:
                    passEntity.setResult("比对失败");
                    break;
                case 3:
                    passEntity.setResult("卡号不存在");
                    break;
                case 4:
                    passEntity.setResult("胁迫报警");
                    break;
                default:
                    passEntity.setResult("未知事件");
                    break;
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("msg", msg);
        jsonObject.put("count", passEntityList.size());
        JSONArray jsonArray = JSONArray.parseArray(JSON.toJSONString(passEntityList));
        jsonObject.put("data", jsonArray);
        return jsonObject.toJSONString();
    }

    /*
     * 生成String-Json
     * */
    public String toStringJson(int code, String title, String content) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", code);
        jsonObject.put("title", title);
        jsonObject.put("content", content);
        return jsonObject.toJSONString();
    }
}
