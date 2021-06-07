package server.entities;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.io.Serializable;
import java.util.Date;

@MappedSuperclass
@Data
public abstract class BaseEntity implements Serializable
{

    @Column(name = "CREATED_AT")
    private Date createdAt ;

    @Column(name = "UPDATED_AT")
    private Date updatedAt;

    @PrePersist
    public void prePersist()
    {
        this.createdAt = new Date();
    }

    @PreUpdate
    public void preUpdate()
    {
        this.updatedAt = new Date();
    }

}
