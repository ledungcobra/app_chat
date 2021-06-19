package common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ResponseMessageDto implements Serializable {
    private FriendDto friendDto;
    private List<PrivateMessageDto> messageDtoList;

    public ResponseMessageDto(FriendDto friendDto, List<PrivateMessageDto> messageDtoList) {
        this.friendDto = friendDto;
        this.messageDtoList = messageDtoList;
    }
}
