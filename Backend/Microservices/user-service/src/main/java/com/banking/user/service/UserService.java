package com.banking.user.service;

import com.banking.user.client.AccountClient;
import com.banking.user.dto.AccountDto;
import com.banking.user.dto.UserRequestDto;
import com.banking.user.dto.UserResponseDto;
import com.banking.user.entity.User;
import com.banking.user.exception.UserNotFoundException;
import com.banking.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private UserRepository userRepository;
    private AccountClient accountClient;

    public UserService(UserRepository userRepository, AccountClient accountClient){
        this.userRepository =userRepository;
        this.accountClient = accountClient;
    }

    public List<UserResponseDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponseDto> responseDtos = new ArrayList<>();
        for (User u : users){
            UserResponseDto userResponseDto = new UserResponseDto();
            userResponseDto.setId(u.getId());
            userResponseDto.setName(u.getName());
            userResponseDto.setEmail(u.getEmail());
            userResponseDto.setRole(u.getRole());

            responseDtos.add(userResponseDto);
        }

        return responseDtos;
    }

    public UserResponseDto registerUser(UserRequestDto requestDto) {

        User user = new User();
        user.setName(requestDto.getName());
        user.setEmail(requestDto.getEmail());
        user.setPassword(requestDto.getPassword());
        user.setRole(requestDto.getRole());


        User savedUser = userRepository.save(user);


        UserResponseDto responseDto = new UserResponseDto();
        responseDto.setId(savedUser.getId());
        responseDto.setName(savedUser.getName());
        responseDto.setEmail(savedUser.getEmail());
        responseDto.setRole(savedUser.getRole());

        return responseDto;
    }

    public UserResponseDto getUserById(Long userId) {

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setId(u.getId());
        userResponseDto.setName(u.getName());
        userResponseDto.setEmail(u.getEmail());
        userResponseDto.setRole(u.getRole());
        return userResponseDto;
    }

    public UserResponseDto updateUser(Long userId, User updatedUser) {

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());

        User u = userRepository.save(existingUser);
        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setId(u.getId());
        userResponseDto.setName(u.getName());
        userResponseDto.setEmail(u.getEmail());
        userResponseDto.setRole(u.getRole());
        return userResponseDto;

    }

    public void deleteUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        userRepository.delete(user);
    }


    public List<AccountDto> getAccountsOfUser(Long userId) {

        System.out.println("Received userId = " + userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        return accountClient.getAccountsByUserId(userId);
    }

}
