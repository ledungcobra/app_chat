package server.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.io.Serializable;

@EqualsAndHashCode
@Data
@Entity
@Table(name = "PRIVATE_MESSAGE")
public class PrivateMessage extends BaseEntity implements Serializable
{

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

    @OneToOne
    @JoinColumn(name = "NEXT_ID")
    private PrivateMessage nextMessage;


}
