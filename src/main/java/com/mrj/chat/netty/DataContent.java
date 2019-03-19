package com.mrj.chat.netty;

import java.io.Serializable;

public class DataContent implements Serializable {

    private Integer action; // 动作类型
    private ChatMsg chatMsg; //用户的聊天内容
    private String extend;   //拓展参数

    public DataContent() {
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public ChatMsg getChatMsg() {
        return chatMsg;
    }

    public void setChatMsg(ChatMsg chatMsg1) {
        this.chatMsg = chatMsg1;
    }

    public String getExtend() {
        return extend;
    }

    public void setExtend(String extend) {
        this.extend = extend;
    }

    @Override
    public String toString() {
        return "DataContent{" +
                "action=" + action +
                ", chatMsg=" + chatMsg +
                ", extend='" + extend + '\'' +
                '}';
    }


}
