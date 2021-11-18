package Acceptor;

import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.*;
import quickfix.fix42.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TradeAppAcceptor {
    /**
     * Создание ответного сообщения
     *
     * @param socketChannel
     * @param message
     * @return
     * @throws IOException
     */
    public ByteBuffer createExecutionReport(SocketChannel socketChannel, String message, Attachment attachment) throws IOException {
        StringField field = null;
        ByteBuffer response = null;
        quickfix.Message messageFix = null;

        Integer inMsgSeqNo = attachment.getInMsgSeqNo(socketChannel);
        Integer outMsgSeqNo = attachment.getOutMsgSeqNo(socketChannel);

        try {
            messageFix = MessageUtils.parse(new DefaultMessageFactory(), new DataDictionary("./FIX42.xml"), message);

            if (!messageFix.getHeader().getField(new TargetCompID()).getValue().equals("EXECUTOR"))
                return null;

            field = messageFix.getHeader().getField(new MsgType());
            String target = messageFix.getHeader().getField(new SenderCompID()).getValue();
            int msgSeqNo = messageFix.getHeader().getField(new MsgSeqNum()).getValue();

            switch (field.getValue()) {
                case "A": {
                    //Logon
                    inMsgSeqNo++;
                    outMsgSeqNo++;
                    if (msgSeqNo == 1) {
                        Logon logon = new Logon(
                                new EncryptMethod(0),
                                new HeartBtInt(30));

                        setHeader(target, outMsgSeqNo, logon);

                        response = ByteBuffer.wrap(logon.toString().getBytes());
                    }

                    attachment.putOutMsgSeqNo(socketChannel, outMsgSeqNo);
                    attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
                    break;
                }
                case "D": {
                    //NewOrderSingle
                    inMsgSeqNo++;
                    outMsgSeqNo++;
                    if (msgSeqNo > 1 && msgSeqNo == inMsgSeqNo) {
                        NewOrderSingle order = (NewOrderSingle) messageFix;
                        ExecutionReport executionReport = new ExecutionReport(
                                new OrderID("123456"),
                                new ExecID("789"),
                                new ExecTransType(ExecTransType.NEW),
                                new ExecType(ExecType.NEW),
                                new OrdStatus(OrdStatus.NEW),
                                order.getSymbol(),
                                order.getSide(),
                                new LeavesQty(0),
                                new CumQty(0),
                                new AvgPx(0));

                        setHeader(target, outMsgSeqNo, executionReport);

                        response = ByteBuffer.wrap(executionReport.toString().getBytes());
                    } else if (msgSeqNo > 1 && msgSeqNo != inMsgSeqNo){
                        //ResendRequest send
                        //TODO inSeqNo, outSeqNo???
                        ResendRequest resendRequest = new ResendRequest();
                        if (msgSeqNo == 0) {
                            resendRequest.set(new BeginSeqNo(0));
                        } else {
                            resendRequest.set(new BeginSeqNo(inMsgSeqNo));
                        }
                        resendRequest.set(new EndSeqNo(msgSeqNo));

                        setHeader(target, outMsgSeqNo, resendRequest);

                        response = ByteBuffer.wrap(resendRequest.toString().getBytes());
                        break;
                    }

                    attachment.putOutMsgSeqNo(socketChannel, outMsgSeqNo);
                    attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
                    break;
                }
                case "5": {
                    //Logout
                    inMsgSeqNo++;
                    outMsgSeqNo++;
                    if (msgSeqNo > 1 && msgSeqNo == inMsgSeqNo) {
                        Logout logout = new Logout();
                        setHeader(target, outMsgSeqNo, logout);
                        response = ByteBuffer.wrap(logout.toString().getBytes());
                    }

                    attachment.putOutMsgSeqNo(socketChannel, outMsgSeqNo);
                    attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
                    break;
                }
                case "0": {
                    //Heartbeat
                    inMsgSeqNo++;
                    attachment.putInMsgSeqNo(socketChannel, inMsgSeqNo);
                    break;
                }
                case "1": {
                    //send HeartBeat
                    inMsgSeqNo++;
                    outMsgSeqNo++;
                    Heartbeat heartbeat = new Heartbeat();
                    setHeader(target, outMsgSeqNo, heartbeat);

                    response = ByteBuffer.wrap(heartbeat.toString().getBytes());
                }
            }
        } catch (InvalidMessage | ConfigError | FieldNotFound invalidMessage) {
            invalidMessage.printStackTrace();
        }
        return response;
    }

    private void setHeader (String targetCompID, int outMsgSeqNo, Message message) {
        quickfix.fix42.Message.Header header = (quickfix.fix42.Message.Header) message.getHeader();
        header.set(new SenderCompID("EXECUTOR"));
        header.set(new TargetCompID(targetCompID));
        header.set(new SendingTime());
        header.set(new MsgSeqNum(outMsgSeqNo));
    }
}
