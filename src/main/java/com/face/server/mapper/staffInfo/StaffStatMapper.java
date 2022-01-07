package com.face.server.mapper.staffInfo;

import com.face.server.entity.StaffStatEntity;
import lombok.Value;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StaffStatMapper {
    //获取所以变动信息
    List<StaffStatEntity> getAll();

    //删除改后的信息，根据EMPID
    void deleteByEMPID(@Param("EMPID") int EMPID);

    //删除改后的信息，根据卡号
    void deleteByCard(@Param("card") String card);
}
