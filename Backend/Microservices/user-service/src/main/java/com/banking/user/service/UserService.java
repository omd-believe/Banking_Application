package com.banking.user.service;

import com.banking.user.client.AccountClient;
import com.banking.user.dto.AccountDto;
import com.banking.user.entity.User;
import com.banking.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private UserRepository userRepository;
    private AccountClient accountClient;

    public UserService(UserRepository userRepository, AccountClient accountClient){
        this.userRepository =userRepository;
        this.accountClient = accountClient;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User registerUser(User user) {
        return userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));
    }

    public User updateUser(Long userId, User updatedUser) {

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        userRepository.delete(user);
    }


    public List<AccountDto> getAccountsOfUser(Long userId) {

        System.out.println("Received userId = " + userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        return accountClient.getAccountsByUserId(userId);
    }

}
