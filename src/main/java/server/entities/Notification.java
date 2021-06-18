package server.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table
public class Notification extends BaseEntity
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;

    public Notification(String message, User receiveUser)
    {
        this.message = message;
        this.receiveUser = receiveUser;
    }

    @ManyToOne
    @JoinColumn(name = "FROM_USER")
    private User fromUser;

    @ManyToOne
    @JoinColumn(name = "RECEIVE_USER")
    private User receiveUser;

    private Boolean isSeen = false;

    public Notification()
    {

    }
}
