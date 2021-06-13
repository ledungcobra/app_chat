package common.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class NotificationDto implements Serializable
{

    private Long id;

    private String message;

    private Object payload;

    private UserDto fromUser;

    private boolean isSeen = false;

    public NotificationDto(String message, UserDto fromUser)
    {
        this.message = message;
        this.fromUser = fromUser;
    }
}
