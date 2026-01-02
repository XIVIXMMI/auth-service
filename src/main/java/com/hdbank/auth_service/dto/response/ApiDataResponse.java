package com.hdbank.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiDataResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public static <T> ApiDataResponse<T> success(String message, T data){
        return ApiDataResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiDataResponse<T> error(String message){
        return ApiDataResponse.<T>builder()
                .success(false)
                .message(message)
                .build();
    }

    public static <T> ApiDataResponse<T> error(String message, T data){
        return ApiDataResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .build();
    }
}
