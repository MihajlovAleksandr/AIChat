package com.example.aichat.model.utils.mappers;

import com.example.aichat.dto.request.UserDataRequest;
import com.example.aichat.dto.response.UserDataResponse;
import com.example.aichat.model.entities.UserData;

public class UserDataMapper implements Mapper<UserDataRequest, UserData, UserDataResponse> {
    @Override
    public UserDataRequest ToDTO(UserData userData) {
        return new UserDataRequest(userData.getName(), userData.getAge(), userData.getGender());
    }

    @Override
    public UserData ToModel(UserDataResponse userDataResponse) {
        return new UserData(userDataResponse.name, userDataResponse.age, userDataResponse.gender);
    }
}
