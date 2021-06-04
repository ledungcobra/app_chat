package com.appchat.server.entities;


import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;


@MappedSuperclass
@Getter
@Setter
public class File extends BaseEntity{

    @Column(name = "FILE_NAME", nullable = false)
    protected String fileName;

    @Column(name = "FILE_SIZE", nullable = false)
    protected Integer fileSize;

    @Column(name = "FILE_PATH")
    protected String url;

    @Column(name = "IS_DELETED")
    protected Boolean isDeleted = false;

    @JoinColumn(name = "SENDER_ID")
    @ManyToOne
    private User sender;

}
