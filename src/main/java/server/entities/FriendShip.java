package server.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "FRIENDSHIP_UK", columnNames = {"USER_ID", "PARTNER_ID"})
})
@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class FriendShip extends BaseEntity
{
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "USER_ID")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "PARTNER_ID")
    private User partner;

}
