package com.imooc.netty;

import com.imooc.SpringUtil;
import com.imooc.enums.MsgActionEnum;
import com.imooc.service.UserService;
import com.imooc.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 *      处理消息 的 handler
 *      TextWebSocketFrame ： 在 netty 中，是用于为 websocket 专门处理文本的对象，frame 是消息的载体
 *
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

	// 用于记录和管理 所有客户端的channle
	public static ChannelGroup users=new
			DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


	protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {

		//获取 客户端传输过来的消息
		String content=msg.text();

		Channel currentChannel=ctx.channel();
		//1.获取客户端发来的消息
		DataContent dataContent= JsonUtils.jsonToPojo(content,DataContent.class);
        Integer action=dataContent.getAction();
		//2.判断消息类型，根据不同的类型来处理不同的业务
		if (action== MsgActionEnum.CONNECT.type){
			//  2.1 当websocket 第一次open的时候，初始化channel,把 用户的channel和 userid关联起来
			String senderId=dataContent.getChatMsg().getSenderId();
			UserChannelRel.put(senderId,currentChannel);
            //测试
			for (Channel c:users){
				System.out.println(c.id().asLongText());
			}
			UserChannelRel.output();
		}else if (action== MsgActionEnum.CHAT.type){
			//  2.2 聊天类型的消息， 把聊天记录保持到数据库中，同时标记消息的签收状态【未签收】

			 ChatMsg chatMsg=dataContent.getChatMsg();
			 String msgText=chatMsg.getMsg();
			 String receiverId=chatMsg.getReceiverId();
			 String senderId=chatMsg.getSenderId();
             //保存消息到数据库，并且标记为未签收
			UserService userService=(UserService)SpringUtil.getBean("userServiceImpl");
			String msgId =userService.saveMsg(chatMsg);
			chatMsg.setMsgId(msgId);


			DataContent dataContentMsg=new DataContent();
			dataContentMsg.setChatMsg(chatMsg);
			//发送消息
			//从全局用户Channel 关系中获取接受方的channel
			 Channel receiveChannel=UserChannelRel.get(receiverId);
			 if (receiveChannel==null){
			 	//TODO channel为空，代表用户离线，推送消息

			 }else{
			 	  Channel findChannel=users.find(receiveChannel.id());
			 	  if (findChannel!=null){
			 	  	  //用户在线
					  receiveChannel.writeAndFlush(new
							  TextWebSocketFrame(
							  		JsonUtils.objectToJson(dataContentMsg)));
				  }else{
			 	  	     //用户离线 推送消息
				  }
			 }

		}else if (action== MsgActionEnum.SIGNED.type){
			//   2.3 签收消息类型，针对具体的消息进行签收，修改数据库中对应消息的签收状态【已签收】
			UserService userService=(UserService)SpringUtil.getBean("userServiceImpl");
			// 扩展字段在 signed 类型 的消息中，代表需要去签收的消息id，逗号间隔
			String msgIdStr =dataContent.getExtand();
            String msgIds[]=msgIdStr.split(",");

			List<String> msgIdList=new ArrayList<>();
			for (String mid :msgIds){
				 if (StringUtils.isNotBlank(mid)){
				 	 msgIdList.add(mid);
				 }
			}
			System.out.println(msgIdList.toString());
			if (msgIdList!=null&&!msgIdList.isEmpty()&&msgIdList.size()>0){
				   //批量签收
				userService.updateMsgSigned(msgIdList);

			}

		}else if (action== MsgActionEnum.KEEPALIVE.type){
			//   2.4 心跳类型的消息
			System.out.println("收到来自channel为【"+currentChannel+"】心跳包...");
		}




	}


	/**
	 *               当客户端连接服务端之后（打开链接）
	 *               获取客户端的 Channel,并且放到 ChannelGroup 中去进行管理
	 * @param ctx
	 * @throws Exception
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {

		users.add(ctx.channel());
	}

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		//当触发 handlerRemove,channelGroup 会自动移除对应的客户端的channel
//		System.out.println("客户端断开，channel对应的长id为："+ctx.channel().id().asLongText());

         String channelId=ctx.channel().id().asShortText();
		System.out.println("客户端被移除，channelId为："+channelId);
		users.remove(ctx.channel());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

		//发生异常之后关闭连接（关闭channel）,随后从 ChannelGroup中移除
		ctx.channel().close();
		users.remove(ctx.channel());

	}
}
