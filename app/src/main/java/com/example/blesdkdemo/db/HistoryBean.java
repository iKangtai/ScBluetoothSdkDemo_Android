package com.example.blesdkdemo.db;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(tableName = "tb_history")
public class HistoryBean implements Serializable {
    @DatabaseField
    private String fhrAudio;
    @DatabaseField
    private String fhr_json;
    @DatabaseField(generatedId = true)
    private int id;
    @DatabaseField(canBeNull = false)
    private String time;

    public HistoryBean() {
    }

    public HistoryBean(String str, String str2, String str3) {
        this.time = str;
        this.fhr_json = str2;
        this.fhrAudio = str3;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public String getTime() {
        return this.time;
    }

    public void setTime(String str) {
        this.time = str;
    }

    public String getFhr_json() {
        return this.fhr_json;
    }

    public void setFhr_json(String str) {
        this.fhr_json = str;
    }

    public String getFhrAudio() {
        return this.fhrAudio;
    }

    public void setFhrAudio(String str) {
        this.fhrAudio = str;
    }

    public String toString() {
        return "HistoryBean{id=" + this.id + ", time='" + this.time + '\'' + ", fhr_json='" + this.fhr_json + '\'' + ", fhrAudio='" + this.fhrAudio + '\'' + '}';
    }
}