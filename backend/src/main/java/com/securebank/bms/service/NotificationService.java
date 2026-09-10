package com.securebank.bms.service;

import com.securebank.bms.entity.Notification;
import com.securebank.bms.entity.UserAccount;
import com.securebank.bms.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void notify(UserAccount user, String title, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setTitle(title);
        n.setMessage(message);
        repository.save(n);
    }
}
