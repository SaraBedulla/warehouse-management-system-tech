package com.wms.service;

import com.wms.dto.request.UpdateUserRequest;
import com.wms.dto.request.UserRequest;
import com.wms.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(Long id, UpdateUserRequest request);
    void deleteUser(Long id);
}
