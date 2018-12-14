package com.main.ocrapp;

/**
 * Created by Administrator on 2018/12/12/0012.
 */

public class list_item {
    private String cover;
    private String content;
    private String Time;
    public list_item(){
        super();
    }
    public list_item(String cover,String content,String Time){
        this.cover=cover;
        this.content=content;
        this.Time=Time;
    }
    public void setCover(String cover){
        this.cover=cover;
    }
    public String getCover(){
        return cover;
    }
    public void setContent(String Content){
        this.content=Content;
    }
    public String getContent(){
        return content;
    }
    public void setTime(String Time){this.Time=Time;}
    public String getTime(){return Time;}
}
