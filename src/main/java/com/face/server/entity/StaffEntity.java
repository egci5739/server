package com.face.server.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StaffEntity {
    private String staffUserId;//员工号
    private int staffEmpId;//人员empId
    private String staffName;//姓名
    private String staffCardId;//证件号
    private String staffCardNumber;//卡号
    private String staffBirthday;//出生日期
    private String staffCompany;//公司
    private byte[] staffImage;//照片

    public String getStaffUserId() {
        return staffUserId;
    }

    public void setStaffUserId(String staffUserId) {
        this.staffUserId = staffUserId;
    }

    private int staffValidity;//状态


    public int getStaffEmpId() {
        return staffEmpId;
    }

    public void setStaffEmpId(int staffEmpId) {
        this.staffEmpId = staffEmpId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffCardId() {
        return staffCardId;
    }

    public void setStaffCardId(String staffCardId) {
        this.staffCardId = staffCardId;
    }

    public String getStaffCardNumber() {
        return staffCardNumber;
    }

    public void setStaffCardNumber(String staffCardNumber) {
        this.staffCardNumber = staffCardNumber;
    }

    public String getStaffBirthday() {
        return staffBirthday;
    }

    public void setStaffBirthday(String staffBirthday) {
        this.staffBirthday = staffBirthday;
    }

    public String getStaffCompany() {
        return staffCompany;
    }

    public void setStaffCompany(String staffCompany) {
        this.staffCompany = staffCompany;
    }

    public byte[] getStaffImage() {
        return staffImage;
    }

    public void setStaffImage(byte[] staffImage) {
        this.staffImage = staffImage;
    }

    public int getStaffValidity() {
        return staffValidity;
    }

    public void setStaffValidity(int staffValidity) {
        this.staffValidity = staffValidity;
    }

    @Override
    public String toString() {
        return "StaffEntity{" +
                "staffName='" + staffName + '\'' +
                ", staffCardNumber='" + staffCardNumber + '\'' +
                '}';
    }
}