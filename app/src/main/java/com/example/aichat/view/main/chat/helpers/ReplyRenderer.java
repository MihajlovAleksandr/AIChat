package com.example.aichat.view.main.chat.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageReply;
import com.example.aichat.R;
import io.noties.markwon.Markwon;
import java.util.List;
import java.util.UUID;

public class ReplyRenderer {

    private final LayoutInflater inflater;
    private final Markwon markwon;
    private final MessageFinder messageFinder;
    private final ReplyClickListener replyClickListener;

    public interface MessageFinder {
        Message findMessageById(UUID id);
    }

    public interface ReplyClickListener {
        void onReplyClicked(UUID replyMessageId);
    }

    public ReplyRenderer(Context context, MessageFinder finder, ReplyClickListener clickListener) {
        this.inflater = LayoutInflater.from(context);
        this.markwon = Markwon.create(context);
        this.messageFinder = finder;
        this.replyClickListener = clickListener;
    }

    public void renderReplies(LinearLayout container, List<MessageReply> replies) {
        container.removeAllViews();

        if (replies == null || replies.isEmpty()) return;

        int limit = Math.min(3, replies.size());

        for (int i = 0; i < limit; i++) {
            MessageReply reply = replies.get(i);

            View view = inflater.inflate(R.layout.item_reply, container, false);
            TextView replyText = view.findViewById(R.id.reply_text);
            ImageView replyIcon = view.findViewById(R.id.reply_icon);

            replyIcon.setImageResource(reply.startIndexQuote == null || reply.endIndexQuote == null
                    ? R.drawable.ic_reply : R.drawable.ic_quote);

            Message original = messageFinder.findMessageById(reply.replyMessageId);
            if (original != null) {
                String quote = extractQuote(original.getText(), reply);
                markwon.setMarkdown(replyText, quote);
            } else {
                replyText.setText("> (сообщение удалено)");
            }

            view.setOnClickListener(v -> onClick(reply));
            replyText.setOnClickListener(v->onClick(reply));
            container.addView(view);
        }
    }

    private void onClick(MessageReply reply){
        if (replyClickListener != null) {
            replyClickListener.onReplyClicked(reply.replyMessageId);
        }
    }

    private String extractQuote(String text, MessageReply reply) {
        if (text == null) return "";

        if (reply.startIndexQuote == null || reply.endIndexQuote == null) {
            return text.length() > 70 ? text.substring(0, 70) + "..." : text;
        }

        try {
            return text.substring(reply.startIndexQuote, reply.endIndexQuote);
        } catch (Exception e) {
            return text;
        }
    }
}
