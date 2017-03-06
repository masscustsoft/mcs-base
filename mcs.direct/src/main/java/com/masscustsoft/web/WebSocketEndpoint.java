package com.masscustsoft.web;

import java.util.Map;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.masscustsoft.model.JsonResult;
import com.masscustsoft.service.DirectConfig;
import com.masscustsoft.service.DirectSession;
import com.masscustsoft.util.GlbHelper;
import com.masscustsoft.util.LightUtil;
import com.masscustsoft.util.ThreadHelper;

@ServerEndpoint("/websocket")
public class WebSocketEndpoint {

	public WebSocketEndpoint() {
	}

	@OnOpen
	public void onOpen(Session session) {
		session.setMaxIdleTimeout(1000000);
		System.out.println("websocket idle="+session.getMaxIdleTimeout()+", ses="+session);
		session.getId();
	}
	
	@OnClose
	public void onClose(Session session, CloseReason reason) {
		System.out.println("Close "+session+", reason="+reason);
	}
	
	@OnError
	public void onError(Session session, Throwable throwable) {
		System.out.println("Close "+session+", reason="+throwable);
	}

	@OnMessage
	public void onMessage(String message, Session socket) {
		if (message == null)
			return;
		try {
			String json = message.trim();
			if (json.startsWith("\""))
				json = json.substring(1);
			if (json.endsWith("\""))
				json = json.substring(0, json.length() - 1);
			if (!json.startsWith("{") || !json.endsWith("}"))
				throw new Exception("SessionTimeout");

			System.out.println("WebSocket onMessage: json=" + json);

			Map<String, Object> m = (Map) LightUtil.parseJson(json);

			String sessionId = (String) m.get("sessionId");
			if (sessionId == null)
				throw new Exception("SessionTimeout");
			String[] ss = sessionId.split(":");
			if (ss.length != 2)
				throw new Exception("SessionTimeout");
			sessionId = ss[1];
			String cfgId = ss[0];
			DirectConfig directCfg = (DirectConfig) GlbHelper.get("$DirectCfg$" + cfgId);
			ThreadHelper.set("beanFactory", directCfg.getBeanFactory());

			directCfg.initThread();
			DirectSession ses = directCfg.getSessionService().getSession(sessionId);
			if (ses == null)
				throw new Exception("SessionTimeout");

			String cmd = (String) m.get("cmd");
			if (cmd == null)
				cmd = "";
			ses.updateSession();

			JsonResult ret = new JsonResult();
			try {
				ses.getDirectConfig().doProcess(cmd, ret);
			} catch (Exception e) {
				ret.setError(e);
			}
			String res = LightUtil.toJsonString(ret).toString().replace("\\n", "<br>");
			socket.getBasicRemote().sendText(res);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Ping Message fail");
		}
	}
}
