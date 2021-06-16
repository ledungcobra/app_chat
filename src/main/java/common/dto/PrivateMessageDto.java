package common.dto;

import lombok.*;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PrivateMessageDto implements Serializable
{

    public PrivateMessageDto() {
    }

    public PrivateMessageDto(Long id, String content, UserDto sender, FriendDto receiver, Boolean isSeenByReceiver) {
        this.id = id;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.isSeenByReceiver = isSeenByReceiver;
    }

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    private UserDto sender;

    private FriendDto receiver;

    private Boolean isSeenByReceiver;

    public String toString()
    {
        return this.sender + ": " + this.content;
    }
}
