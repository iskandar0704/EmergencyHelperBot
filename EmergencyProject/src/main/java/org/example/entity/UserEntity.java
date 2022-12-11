package org.example.entity;

import lombok.Data;
import org.example.enums.ProfileStatus;
import org.example.enums.UserStep;
import org.telegram.telegrambots.meta.api.objects.Location;

import javax.persistence.*;
import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column
    private Long chatId;
    @Column
    private String name;
    @Column
    private String surname;
    @Column
    private String phone;
    @Column
    @Enumerated(value = EnumType.STRING)
    private ProfileStatus status;
    @Column
    @Enumerated(value = EnumType.STRING)
    private UserStep step;

    @Column
    private Location location;
    @Column
    private LocalDateTime createdDate;
}
