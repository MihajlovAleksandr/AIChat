package com.example.aichat.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aichat.R;
import com.example.aichat.controller.LoginController;
import com.example.aichat.controller.main.MessageController;
import com.example.aichat.model.entities.Message;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    TextInputLayout emailInputLayout;
    TextInputLayout passwordInputLayout;
    EditText emailEditText;
    EditText passwordEditText;
    Button loginButton;
    ImageView imageView;
    TextView registerTextView;
    private LoginController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        registerTextView = findViewById(R.id.registerTextView);
        String text = "Еще нет аккаунта? Зарегистрироваться";
        SpannableString spannableString = new SpannableString(text);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.link_color));
                ds.setUnderlineText(true);
            }
        };
        int startIndex = text.indexOf("Зарегистрироваться");
        int endIndex = startIndex + "Зарегистрироваться".length();
        spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        registerTextView.setText(spannableString);
        registerTextView.setMovementMethod(LinkMovementMethod.getInstance());
        emailInputLayout = findViewById(R.id.emailInputLayout);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login);
        imageView = findViewById(R.id.imageView);
        controller = new LoginController(this,
                emailInputLayout,
                passwordInputLayout,
                emailEditText,
                passwordEditText,
                loginButton,
                imageView);
    }

    public static class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

        private List<Message> messages;
        private MessageController messageController;

        public MessageAdapter(List<Message> messages, int currentUserId) {
            this.messages = messages;
            this.messageController = new MessageController(currentUserId);
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layoutRes = viewType == 0 ? R.layout.my_message : R.layout.other_message;
            View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = messages.get(position);
            holder.messageText.setText(message.getText());
            holder.timeText.setText(messageController.getFormattedMessageTime(message));
        }

        @Override
        public int getItemViewType(int position) {
            return messageController.isMyMessage(messages.get(position)) ? 0 : 1;
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        static class MessageViewHolder extends RecyclerView.ViewHolder {
            TextView messageText;
            TextView timeText;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                messageText = itemView.findViewById(R.id.message_text);
                timeText = itemView.findViewById(R.id.time_text);
            }
        }
    }
}
