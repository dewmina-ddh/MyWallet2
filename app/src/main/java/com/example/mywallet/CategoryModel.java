package com.example.mywallet;

public class CategoryModel {
    private int id;
    private String name;
    private String type; // "INCOME" or "EXPENSE"
    private String iconName;

    public CategoryModel() {
    }

    public CategoryModel(int id, String name, String type, String iconName) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.iconName = iconName;
    }

    public CategoryModel(String name, String type, String iconName) {
        this.name = name;
        this.type = type;
        this.iconName = iconName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
}