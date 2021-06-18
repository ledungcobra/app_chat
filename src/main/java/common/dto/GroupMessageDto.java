package common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import server.entities.Group;
import server.entities.User;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupMessageDto implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    private String content;

    private UserDto sender;

    private GroupDto groupReceiver;

    private GroupMessageDto nextMessage;

    @Override
    public String toString() {
        return this.sender + ": " + this.content;
    }
}
