package net.lax1dude.eaglercraft.tailsconnect;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Relay broadcasts must carry both endpoints so each player has an isolated stream. */
public final class TailsConnectFrame {
    public static final int MAX_PACKET = 1048500;
    public final String sender, recipient;
    public final byte[] payload;

    private TailsConnectFrame(String sender, String recipient, byte[] payload) {
        this.sender = sender;
        this.recipient = recipient;
        this.payload = payload;
    }

    public static boolean validId(String id) {
        return id != null && id.matches("[0-9a-f]{32}");
    }

    public static byte[] encode(String sender, String recipient, byte[] payload) {
        if (!validId(sender) || !validId(recipient) || payload.length == 0 || payload.length > MAX_PACKET)
            throw new IllegalArgumentException("Invalid TailsConnect packet");
        byte[] frame = new byte[68 + payload.length];
        frame[0] = 'T'; frame[1] = 'C'; frame[2] = '2'; frame[3] = 0;
        System.arraycopy(sender.getBytes(StandardCharsets.US_ASCII), 0, frame, 4, 32);
        System.arraycopy(recipient.getBytes(StandardCharsets.US_ASCII), 0, frame, 36, 32);
        System.arraycopy(payload, 0, frame, 68, payload.length);
        return frame;
    }

    public static TailsConnectFrame decode(byte[] frame) {
        if (frame.length <= 68 || frame.length > MAX_PACKET + 68 || frame[0] != 'T'
                || frame[1] != 'C' || frame[2] != '2' || frame[3] != 0) return null;
        String sender = new String(frame, 4, 32, StandardCharsets.US_ASCII);
        String recipient = new String(frame, 36, 32, StandardCharsets.US_ASCII);
        if (!validId(sender) || !validId(recipient)) return null;
        return new TailsConnectFrame(sender, recipient, Arrays.copyOfRange(frame, 68, frame.length));
    }
}
