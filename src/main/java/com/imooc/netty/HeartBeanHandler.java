package com.imooc.netty;

import com.imooc.SpringUtil;
import com.imooc.enums.MsgActionEnum;
import com.imooc.service.UserService;
import com.imooc.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *     用于检测 channel 的心跳的handler
 *
 *
 *    继承 ChannelInboundHandlerAdapter ，从而不需要实现channelRead0 方法
 *
 *
 */
public class HeartBeanHandler extends ChannelInboundHandlerAdapter {


	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		  //判断evt 是否是 IdleStateEvent （用于触发用户事件，包括 读空闲/写空闲/读写空闲）
		  if (evt instanceof IdleStateEvent){
			  IdleStateEvent event =(IdleStateEvent) evt;//强制类型转换

			  if (event.state()== IdleState.READER_IDLE){
				  System.out.println("进入读空闲");
			  }else if (event.state()== IdleState.WRITER_IDLE){
				  System.out.println("进入写空闲");
			  }else if (event.state()== IdleState.ALL_IDLE){
				  System.out.println("channel关闭前，users的数量为："+ChatHandler.users.size());

				  Channel channel=ctx.channel();
				  //关闭无用的channel，以防资源的浪费
				  channel.close();

				  System.out.println("channel关闭后，users的数量为："+ChatHandler.users.size());


			  }
		  }
	}


}
