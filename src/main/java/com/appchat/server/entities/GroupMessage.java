package com.appchat.server.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.io.Serializable;

@EqualsAndHashCode
@Data
@Entity
@Table(name = "PRIVATE_MESSAGE")
public class GroupMessage extends BaseEntity implements Serializable, Message {

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

}
