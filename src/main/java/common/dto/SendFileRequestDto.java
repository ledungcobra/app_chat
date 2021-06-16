package common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SendFileRequestDto implements Serializable {

    public byte[] fileContent;
    public UserDto sender;
    public Object receiver;
    public String fileName;
    public int fileSize;
    public boolean isPrivate;

    public SendFileRequestDto(byte[] fileContent, UserDto senderId, Object receiverId, String fileName, int fileSize, boolean isPrivate) {
        this.fileContent = fileContent;
        this.sender = senderId;
        this.receiver = receiverId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.isPrivate = isPrivate;
    }
}
