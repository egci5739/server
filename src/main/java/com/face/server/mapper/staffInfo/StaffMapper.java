package com.face.server.mapper.staffInfo;

import com.face.server.entity.PassEntity;
import com.face.server.entity.StaffEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StaffMapper {
    //获取单个人员
    StaffEntity getStaffByNum(@Param("staffCardNumber") String staffCardNumber);

    //获取全部人员
//    List<StaffEntity> getAll(@Param("sum") int sum);
    List<StaffEntity> getAll();

    //根据姓名获取
    List<StaffEntity> getStaffByName(@Param("staffName") String staffName);

    //根据员工号获取
    List<StaffEntity> getStaffByUserId(@Param("staffUserId") String staffUserId);

    //根据证件号获取
    List<StaffEntity> getStaffByCardId(@Param("staffCardId") String staffCardId);

    //根据卡号获取
    List<StaffEntity> getStaffByCard(@Param("staffCardNumber") String staffCardNumber);

    //根据卡号集合获取
    List<StaffEntity> getStaffByCards(@Param("staffCardNumbers") String staffCardNumbers);//5011,5021

    //根据卡号获取单个人员信息，给通行记录使用
    StaffEntity getStaffByCardForPass(@Param("staffCardNumber") String staffCardNumber);

}
