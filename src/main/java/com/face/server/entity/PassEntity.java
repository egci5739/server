package com.face.server.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
public class PassEntity {
    private int passId;//id
    private String staffUserId;//员工号
    private int staffEmpId;//人员empId
    private String staffName;//姓名
    private String staffCardId;//证件号
    private String staffCardNumber;//卡号
    private String staffCompany;//公司
    private byte[] passCaptureImage;//抓拍图
    private Timestamp passTime;//通行时间
    private String passEquipmentName;//设备名称
    private String passEquipmentIp;//设备ip
    private int passResult;//通行结果：1比对通过；2比对失败；3卡号不存在；4胁迫报警
    private int passSimilarity;//相似度
    private String passNote;//通行备注

    //特殊字段，用于客户端显示时间和事件
    private String time;
    private String result;

    public int getPassSimilarity() {
        return passSimilarity;
    }

    public void setPassSimilarity(int passSimilarity) {
        this.passSimilarity = passSimilarity;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getPassId() {
        return passId;
    }

    public void setPassId(int passId) {
        this.passId = passId;
    }

    public String getStaffUserId() {
        return staffUserId;
    }

    public void setStaffUserId(String staffUserId) {
        this.staffUserId = staffUserId;
    }

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

    public String getStaffCompany() {
        return staffCompany;
    }

    public void setStaffCompany(String staffCompany) {
        this.staffCompany = staffCompany;
    }

    public byte[] getPassCaptureImage() {
        return passCaptureImage;
    }

    public void setPassCaptureImage(byte[] passCaptureImage) {
        this.passCaptureImage = passCaptureImage;
    }

    public Timestamp getPassTime() {
        return passTime;
    }

    public void setPassTime(Timestamp passTime) {
        this.passTime = passTime;
    }

    public String getPassEquipmentName() {
        return passEquipmentName;
    }

    public void setPassEquipmentName(String passEquipmentName) {
        this.passEquipmentName = passEquipmentName;
    }

    public String getPassEquipmentIp() {
        return passEquipmentIp;
    }

    public void setPassEquipmentIp(String passEquipmentIp) {
        this.passEquipmentIp = passEquipmentIp;
    }

    public int getPassResult() {
        return passResult;
    }

    public void setPassResult(int passResult) {
        this.passResult = passResult;
    }

    public String getPassNote() {
        return passNote;
    }

    public void setPassNote(String passNote) {
        this.passNote = passNote;
    }
}
