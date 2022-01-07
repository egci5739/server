package com.face.server.mapper.faceRecognition;

import com.face.server.entity.PassEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface PassMapper {
    //新增通行记录
    void insertPass(PassEntity passEntity);

    //获取历史通行数据
    List<PassEntity> getData(@Param("passEquipmentIp") String passEquipmentIp, @Param("passResult") int passResult, @Param("startTime") Timestamp startTime, @Param("endTime") Timestamp endTime);
}
