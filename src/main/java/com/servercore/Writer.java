package com.servercore;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.Queue;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Writer {
    static void writeMessagesToTheClient(SelectionKey selectionKey, Map<SocketChannel, Queue<ByteBuffer>> pendingData) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        Queue<ByteBuffer> queue = pendingData.get(socketChannel);

        ByteBuffer buffer;

        while ((buffer = queue.peek())!=null){
            socketChannel.write(buffer);
            if(!buffer.hasRemaining()){
                queue.poll();
            }else {
                return;
            }
        }
        socketChannel.register(selectionKey.selector(),SelectionKey.OP_READ);
    }
}
