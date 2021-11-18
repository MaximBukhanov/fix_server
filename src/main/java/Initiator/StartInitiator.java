package Initiator;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;

public class StartInitiator {
    public static void main(String[] args) {
        SocketInitiator socketInitiator = null;
        try {
            SessionSettings initiatorSettings = new SessionSettings("./initiatorSettings.txt");
            Application initiatorApplication = new TradeAppInitiator();

            FileStoreFactory fileStoreFactory = new FileStoreFactory(initiatorSettings);
            FileLogFactory fileLogFactory = new FileLogFactory(initiatorSettings);
            MessageFactory messageFactory = new DefaultMessageFactory();

            socketInitiator = new SocketInitiator(initiatorApplication, fileStoreFactory, initiatorSettings, fileLogFactory, messageFactory);
            socketInitiator.start();

            while (!socketInitiator.isLoggedOn()) {
                continue;
            }

            SessionID sessionId = socketInitiator.getSessions().get(0);

            for (int j = 0; j < 5; j++) {
                try {
                    Thread.sleep(2000);
                    NewOrderSingle newOrderSingle = new NewOrderSingle(
                            new ClOrdID("456"),
                            new HandlInst('3'),
                            new Symbol("AJCB"),
                            new Side(Side.BUY),
                            new TransactTime(),
                            new OrdType(OrdType.MARKET)
                    );
                    Session.sendToTarget(newOrderSingle, sessionId);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (SessionNotFound sessionNotFound) {
                    sessionNotFound.printStackTrace();
                }
            }
            Session.lookupSession(sessionId).logout();
            socketInitiator.stop();

        } catch (ConfigError configError) {
            configError.printStackTrace();
        }
    }
}