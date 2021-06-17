package common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiverVoiceData {
    private byte[] data;
    private FriendDto sender;

    public ReceiverVoiceData(byte[] data, FriendDto sender) {
        this.data = data;
        this.sender = sender;
    }
}
