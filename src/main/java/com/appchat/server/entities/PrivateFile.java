package com.appchat.server.entities;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "PRIVATE_FILE")
@Data
public class PrivateFile extends File
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "RECEIVER_ID")
    @ManyToOne
    private User receiver;


}
