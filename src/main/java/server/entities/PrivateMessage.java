package server.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.io.Serializable;

@EqualsAndHashCode
@Data
@Entity
@Table(name = "PRIVATE_MESSAGE")
public class PrivateMessage extends BaseEntity implements Serializable, Message {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CONTENT")
    private String content;

    @JoinColumn(name = "SENDER_ID")
    @ManyToOne
    private User sender;

    @JoinColumn(name = "RECEIVER_ID")
    @ManyToOne
    private User receiver;

    @Column(name = "IS_SEEN_BY_RECEIVER")
    private Boolean isSeenByReceiver =  false;

}
