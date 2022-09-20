package com.example.test.bean;

import com.google.gson.annotations.SerializedName;

public class CategoriesBean {

    private String id;
    private String name;
    private int depth;
    @SerializedName("parent_id")
    private String parentId;
    @SerializedName("children_count")
    private int childrenCount;
    private boolean free;
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

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        this.childrenCount = childrenCount;
    }

    public boolean isFree() {
        return free;
    }

    public void setFree(boolean free) {
        this.free = free;
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
