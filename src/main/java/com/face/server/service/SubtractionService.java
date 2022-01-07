package com.face.server.service;

import com.face.server.entity.StaffEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class SubtractionService {
    /*
     * 获取需要变动的人员信息：下发和删除
     * */
    public List<String> complement(List<StaffEntity> subtrahend, List<StaffEntity> minuend) {
        List<String> subtrahendList = subtrahend.stream().map(StaffEntity::getStaffCardNumber).collect(toList());
        List<String> minuendList = minuend.stream().map(StaffEntity::getStaffCardNumber).collect(toList());
        return subtrahendList.stream().filter(item -> {
            return !minuendList.contains(item);
        }).collect(toList());
    }

    /*
     * 将人员List按照卡号生成新的数组
     * */
    public String staffListToString(List<StaffEntity> staffEntityList) {
        List<String> stringList = staffEntityList.stream().map(StaffEntity::getStaffCardNumber).collect(toList());
        return String.join(",", stringList);
    }
}
