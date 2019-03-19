package com.mrj.chat.netty;

public class ChatMsg {

	private String senderId;		// 发送者的用户id	
	private String receiverId;		// 接受者的用户id
	private String msg;				// 聊天内容
	private String msgId;			// 用于消息的签收

	public ChatMsg() {
	}

	public String getSenderId() {
		return senderId;
	}
	public void setSenderId(String senderId) {
		this.senderId = senderId;
	}
	public String getReceiverId() {
		return receiverId;
	}
	public void setReceiverId(String receiverId) {
		this.receiverId = receiverId;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getMsgId() {
		return msgId;
	}
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}


	@Override
	public String toString() {
		return "ChatMsg{" +
				"senderId='" + senderId + '\'' +
				", receiverId='" + receiverId + '\'' +
				", msg='" + msg + '\'' +
				", msgId='" + msgId + '\'' +
				'}';
	}
}
