package com.face.server.singleton;

import com.face.server.entity.EquipmentEntity;
import com.face.server.mapper.faceRecognition.EquipmentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipmentSingleton {
    @Autowired
    private EquipmentMapper equipmentMapper;

    public static List<EquipmentEntity> equipmentEntityList;

    public boolean getAll() throws Exception {
        equipmentEntityList = equipmentMapper.getAllEquipment();
        return equipmentEntityList.size() > 0;
    }
}
