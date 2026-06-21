package com.example.aichat.controller.payment;

import com.example.aichat.dto.response.PaymentInfoResponse;
import com.example.aichat.model.connection.ConnectionDispatcher;
import com.example.aichat.model.connection.ConnectionSingleton;
import com.example.aichat.model.connection.HttpClient;
import com.example.aichat.view.payment.PaymentsActivity;
import java.util.Arrays;

public class PaymentsController {
    private final ConnectionDispatcher dispatcher;
    private final PaymentsActivity activity;

    public PaymentsController(PaymentsActivity activity){
        this.activity = activity;
        dispatcher = ConnectionSingleton.getInstance().getConnectionDispatcher();
    }

    public void getPayments() {
        dispatcher.sendHttpRequestAsync("/api/payments", HttpClient.HTTPMethod.GET, null, false)
                .thenAccept(cmd -> {
            if (cmd.isSuccess()) {
                var data = cmd.getData(PaymentInfoResponse[].class);
                activity.runOnUiThread(()-> {
                    activity.renderPayments(Arrays.asList(data));
                });
            }
        });
    }

}
