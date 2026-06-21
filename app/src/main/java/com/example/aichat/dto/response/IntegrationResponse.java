package com.example.aichat.dto.response;

import com.example.aichat.model.entities.IntegrationTypes;
import java.util.List;

public class IntegrationResponse {
    public List<IntegrationTypes> types;
    public IntegrationResponse(List<IntegrationTypes> types) {
        this.types = types;
    }

    public IntegrationResponse() {

    }

    @Override
    public String toString() {
        return "IntegrationResponse" + types.size();
    }
}
