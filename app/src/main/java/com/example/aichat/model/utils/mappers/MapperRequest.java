package com.example.aichat.model.utils.mappers;

public interface MapperRequest <TRequest, TModel> {
    TRequest ToDTO(TModel model);
}
