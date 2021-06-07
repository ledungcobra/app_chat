package server.entities;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "GROUP_FILE")
@Data
public class GroupFile extends File
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "GROUP_ID")
    private Group group;

}
