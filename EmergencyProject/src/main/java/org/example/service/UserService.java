package org.example.service;

import org.example.entity.UserEntity;
import org.example.enums.ProfileStatus;
import org.example.enums.UserStep;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository repository;

    public UserEntity getByChatId(Long chatId){
        Optional<UserEntity> optional = repository.findByChatId(chatId);

        if(optional.isEmpty()){
            UserEntity entity = new UserEntity();
            entity.setChatId(chatId);
            entity.setCreatedDate(LocalDateTime.now());
            entity.setStatus(ProfileStatus.USER);
            entity.setStep(UserStep.NEW);
            return entity;
        }

        return optional.get();
    }

    public Long getChatIdByStatus(ProfileStatus status){
        Long chatId = repository.findByStatus(status);

        return chatId;
    }


    public void save(UserEntity entity){
        repository.save(entity);
    }
}
