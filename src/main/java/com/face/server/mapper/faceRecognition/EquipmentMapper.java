package com.face.server.mapper.faceRecognition;

import com.face.server.entity.EquipmentEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EquipmentMapper {
    List<EquipmentEntity> getAllEquipment();
}
