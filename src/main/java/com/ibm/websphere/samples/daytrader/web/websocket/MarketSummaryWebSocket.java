/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.websphere.samples.daytrader.web.websocket;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.ibm.websphere.samples.daytrader.impl.direct.TradeDirect;
import com.ibm.websphere.samples.daytrader.interfaces.TradeServices;
import com.ibm.websphere.samples.daytrader.util.Log;


/** This class is a WebSocket EndPoint that sends the Market Summary in JSON form and
 *  encodes recent quote price changes when requested or when triggered by CDI events.
 **/

@ServerEndpoint(value = "/marketsummary",decoders={ActionDecoder.class})
public class MarketSummaryWebSocket {

 

  private static final List<Session> sessions = new CopyOnWriteArrayList<>();
  private Session currentSession = null;   
  private final CountDownLatch latch = new CountDownLatch(1);


  private  TradeServices tradeAction = new TradeDirect();

  // should never be used
  public MarketSummaryWebSocket(){
  }

  @OnOpen
  public void onOpen(final Session session, EndpointConfig ec) {  
    Log.trace("MarketSummaryWebSocket:onOpen -- session -->" + session + "<--");

    sessions.add(session);
    currentSession = session;
    latch.countDown();
  } 

  @OnMessage
  public void sendMarketSummary(ActionMessage message) {

    String action = message.getDecodedAction();
    
    Log.trace("MarketSummaryWebSocket:sendMarketSummary -- received -->" + action + "<--");
    try { 
      latch.await();
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }


    if (action != null && action.equals("updateMarketSummary")) {

      try {

        String mkSummary = tradeAction.getMarketSummary().toJSON();

        Log.trace("MarketSummaryWebSocket:sendMarketSummary -- sending -->" + mkSummary + "<--");

        // Make sure onopen is finished
        try { 
          latch.await();
        } catch (Exception e) {
          e.printStackTrace();
          return;
        }

        currentSession.getAsyncRemote().sendText(mkSummary.toString());

      } catch (Exception e) {
        e.printStackTrace();
      }
    } 
  }

  @OnError
  public void onError(Throwable t) {
    Log.trace("MarketSummaryWebSocket:onError -- session -->" + currentSession + "<--");
    t.printStackTrace();
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    Log.trace("MarketSummaryWebSocket:onClose -- session -->" + session + "<--");
    sessions.remove(session);
  }
}
