package com.example.aichat.model.utils.mappers;

public interface Mapper<TRequest, TModel, TResponse> extends MapperRequest<TRequest, TModel>, MapperResponse <TModel, TResponse> {

}
