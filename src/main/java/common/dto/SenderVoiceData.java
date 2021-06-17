package common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SenderVoiceData {
    private byte[] data;
    private FriendDto receiver;

    public SenderVoiceData(byte[] data, FriendDto receiver) {
        this.data = data;
        this.receiver = receiver;
    }
}
