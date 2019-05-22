package com.cw.localsfz.bean;

import java.util.Arrays;

/**
 * 作者：李阳
 * 时间：2019/3/13
 * 描述：
 */
public class PeopleBean {

    private String peopleName;
    private String peopleSex;
    private String peopleNation;
    private String peopleBirthday;
    private String peopleAddress;
    private String peopleIDCode;
    private String department;
    private String startDate;
    private String endDate;
    private byte[] photo;
    private byte[] model;

    public PeopleBean(String peopleName, String peopleSex, String peopleNation, String peopleBirthday, String peopleAddress, String peopleIDCode, String department, String startDate, String endDate, byte[] photo, byte[] model) {
        this.peopleName = peopleName;
        this.peopleSex = peopleSex;
        this.peopleNation = peopleNation;
        this.peopleBirthday = peopleBirthday;
        this.peopleAddress = peopleAddress;
        this.peopleIDCode = peopleIDCode;
        this.department = department;
        this.startDate = startDate;
        this.endDate = endDate;
        this.photo = photo;
        this.model = model;
    }

    public String getPeopleName() {
        return this.peopleName;
    }

    public void setPeopleName(String peopleName) {
        this.peopleName = peopleName;
    }

    public String getPeopleSex() {
        return this.peopleSex;
    }

    public void setPeopleSex(String peopleSex) {
        this.peopleSex = peopleSex;
    }

    public String getPeopleNation() {
        return this.peopleNation;
    }

    public void setPeopleNation(String peopleNation) {
        this.peopleNation = peopleNation;
    }

    public String getPeopleBirthday() {
        return this.peopleBirthday;
    }

    public void setPeopleBirthday(String peopleBirthday) {
        this.peopleBirthday = peopleBirthday;
    }

    public String getPeopleAddress() {
        return this.peopleAddress;
    }

    public void setPeopleAddress(String peopleAddress) {
        this.peopleAddress = peopleAddress;
    }

    public String getPeopleIDCode() {
        return this.peopleIDCode;
    }

    public void setPeopleIDCode(String peopleIDCode) {
        this.peopleIDCode = peopleIDCode;
    }

    public String getDepartment() {
        return this.department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getStartDate() {
        return this.startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return this.endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public byte[] getPhoto() {
        return this.photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public byte[] getModel() {
        return this.model;
    }

    public void setModel(byte[] model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "PeopleBean{peopleName='" + this.peopleName + '\'' + ", peopleSex='" + this.peopleSex + '\'' + ", peopleNation='" + this.peopleNation + '\'' + ", peopleBirthday='" + this.peopleBirthday + '\'' + ", peopleAddress='" + this.peopleAddress + '\'' + ", peopleIDCode='" + this.peopleIDCode + '\'' + ", department='" + this.department + '\'' + ", startDate='" + this.startDate + '\'' + ", endDate='" + this.endDate + '\'' + ", photo=" + Arrays.toString(this.photo) + ", model=" + Arrays.toString(this.model) + '}';
    }

}
