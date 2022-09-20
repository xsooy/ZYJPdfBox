package com.example.test.bean;

import com.google.gson.annotations.SerializedName;

public class ResourceBean {

    private String id;
    private String name;
    private String icon;
    @SerializedName("file_type")
    private String fileType;
    @SerializedName("display_style")
    private String displayStyle;
    @SerializedName("page_count")
    private int pageCount;
    @SerializedName("print_count")
    private int printCount;
    private boolean free;
    @SerializedName("base_member_available")
    private boolean baseMemberAvailable;
    @SerializedName("preschool_member_available")
    private boolean preschoolMemberAvailable;
    @SerializedName("school_member_available")
    private boolean schoolMemberAvailable;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getDisplayStyle() {
        return displayStyle;
    }

    public void setDisplayStyle(String displayStyle) {
        this.displayStyle = displayStyle;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getPrintCount() {
        return printCount;
    }

    public void setPrintCount(int printCount) {
        this.printCount = printCount;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
    }

    public boolean isBaseMemberAvailable() {
        return baseMemberAvailable;
    }

    public void setBaseMemberAvailable(boolean baseMemberAvailable) {
        this.baseMemberAvailable = baseMemberAvailable;
    }

    public boolean isPreschoolMemberAvailable() {
        return preschoolMemberAvailable;
    }

    public void setPreschoolMemberAvailable(boolean preschoolMemberAvailable) {
        this.preschoolMemberAvailable = preschoolMemberAvailable;
    }

    public boolean isSchoolMemberAvailable() {
        return schoolMemberAvailable;
    }

    public void setSchoolMemberAvailable(boolean schoolMemberAvailable) {
        this.schoolMemberAvailable = schoolMemberAvailable;
    }
}
