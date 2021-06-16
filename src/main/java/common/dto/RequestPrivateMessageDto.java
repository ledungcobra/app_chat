package common.dto;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RequestPrivateMessageDto implements Serializable {
    private Long ownerId;
    private Long friendId;
    private Integer offset;
    private Integer numberOfMessages;

    public RequestPrivateMessageDto(Long ownerId, Long friendId, Integer offset, Integer numberOfMessages) {
        this.ownerId = ownerId;
        this.friendId = friendId;
        this.offset = offset;
        this.numberOfMessages = numberOfMessages;
    }
}
