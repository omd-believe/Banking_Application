package com.banking.user.service;

import com.banking.user.client.AccountClient;
import com.banking.user.config.JwtService;
import com.banking.user.dto.*;
import com.banking.user.entity.User;
import com.banking.user.exception.UserNotFoundException;
import com.banking.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private UserRepository userRepository;
    private AccountClient accountClient;
    private PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       AccountClient accountClient,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService){
        this.userRepository =userRepository;
        this.accountClient = accountClient;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponseDto login(LoginRequestDto loginDto){

        User user = userRepository.findByEmail(loginDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Invalid email or Password"));

        if(!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())){
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());

        UserResponseDto userResponseDto = new UserResponseDto();
        userResponseDto.setId(user.getId());
        userResponseDto.setName(user.getName());
        userResponseDto.setEmail(user.getEmail());
        userResponseDto.setRole(user.getRole());

        return new AuthResponseDto(token, userResponseDto);

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

        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
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

    public UserResponseDto updateUser(Long userId, UserRequestDto updatedUserDto) {

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        existingUser.setName(updatedUserDto.getName());
        existingUser.setEmail(updatedUserDto.getEmail());

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
