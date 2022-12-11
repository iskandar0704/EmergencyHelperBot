package org.example.repository;

import org.example.entity.UserEntity;
import org.example.enums.ProfileStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserEntity,Integer> {
    Optional<UserEntity> findByChatId(Long chatId);

    @Query("select u.chatId from UserEntity u where u.status = ?1")
    Long findByStatus(ProfileStatus status);
}
