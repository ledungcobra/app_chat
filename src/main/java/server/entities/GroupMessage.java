package server.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.awt.*;
import java.io.Serializable;

@EqualsAndHashCode
@Data
@Entity
@Table(name = "GROUP_MESSAGE")
public class GroupMessage extends BaseEntity implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CONTENT")
    private String content;

    @JoinColumn(name = "SENDER_ID")
    @ManyToOne
    private User sender;

    @JoinColumn(name = "GROUP_ID")
    @ManyToOne
    private Group groupReceiver;

    @OneToOne
    @JoinColumn(name = "NEXT_ID")
    private GroupMessage nextMessage;

}
