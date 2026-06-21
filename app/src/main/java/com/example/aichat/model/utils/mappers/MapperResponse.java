package com.example.aichat.model.utils.mappers;

public interface MapperResponse<TModel, TResponse> {
    TModel ToModel(TResponse response);
}
