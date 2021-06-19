package common.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import server.entities.Group;
import server.entities.Message;
import server.entities.User;

import javax.persistence.*;
import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupMessageDto implements Serializable, Message {

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    private String content;

    private UserDto sender;

    private GroupDto groupReceiver;

    private GroupMessageDto nextMessage;

    public GroupMessageDto() {

    }

    public GroupMessageDto(Long id, String content, UserDto sender, Receiver receiver) {
        this.id = id;
        this.content = content;
        this.sender = sender;
        this.groupReceiver = (GroupDto) receiver;
    }

    //TODO
//    @Override
//    public String toString() {
//        return this.sender + ": " + this.content;
//    }


    @Override
    public String toString() {
        return "GroupMessageDto{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", sender=" + sender +
                ", groupReceiver=" + groupReceiver +
                ", nextMessage=" + nextMessage +
                '}';
    }

    @Override
    public Receiver getReceiver() {
        return groupReceiver;
    }
}
