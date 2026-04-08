package com.example.aichat.model.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageView;

import androidx.core.content.FileProvider;

import com.example.aichat.R;
import com.example.aichat.controller.main.chat.MessageController;
import com.example.aichat.model.entities.Message;
import com.example.aichat.model.entities.MessageStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ChatPdfExporter {

    private static final int PAGE_WIDTH = 1080;
    private static final int MIN_PAGE_HEIGHT = 1980;
    private static final int MARGIN = 0;
    private static final int MESSAGE_SPACING = 40;

    private final Activity activity;
    private final MessageController messageController;

    public ChatPdfExporter(Activity activity, UUID currentUserId) {
        this.activity = activity;
        this.messageController = new MessageController(currentUserId);
    }

    public void exportChat(List<Message> messages, String chatTitle) throws IOException {
        if (messages == null || messages.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(activity);
        PdfDocument pdfDocument = new PdfDocument();

        View headerView = createHeaderView(inflater, chatTitle);
        int headerHeight = measureView(headerView);

        View footerView = createFooterView(inflater);
        int footerHeight = measureView(footerView);

        int totalCalculatedHeight =
                calculateTotalHeight(inflater, messages, headerHeight, footerHeight);

        int totalHeight = Math.max(totalCalculatedHeight, MIN_PAGE_HEIGHT);

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                totalHeight,
                1
        ).create();

        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int currentY = 0;

        drawView(canvas, headerView, currentY, 0);
        currentY += headerHeight + MARGIN;

        for (Message message : messages) {
            boolean isMyMessage = messageController.isMyMessage(message);
            int layoutRes = isMyMessage
                    ? R.layout.my_message
                    : R.layout.other_message;

            View messageView = inflater.inflate(layoutRes, null, false);

            TextView messageText = messageView.findViewById(R.id.message_text);
            TextView timeText = messageView.findViewById(R.id.time_text);
            ImageView statusIcon = isMyMessage ? messageView.findViewById(R.id.status_icon) : null;

            messageText.setText(message.getText());
            timeText.setText(MessageController.getFormattedMessageTime(message));

            if (isMyMessage && statusIcon != null) {
                MessageStatus maxStatus = MessageController.getMaxStatus(message.getStatuses());
                int iconRes = MessageController.getStatusIconRes(maxStatus);
                statusIcon.setImageResource(iconRes);
            }

            int maxWidth = (int) (PAGE_WIDTH * 0.8);
            messageText.setMaxWidth(maxWidth);

            int widthMeasureSpec =
                    View.MeasureSpec.makeMeasureSpec(PAGE_WIDTH, View.MeasureSpec.AT_MOST);
            int heightMeasureSpec =
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            messageView.measure(widthMeasureSpec, heightMeasureSpec);
            messageView.layout(0, 0,
                    messageView.getMeasuredWidth(),
                    messageView.getMeasuredHeight());

            int itemHeight = messageView.getMeasuredHeight();

            int xOffset = isMyMessage
                    ? PAGE_WIDTH - messageView.getMeasuredWidth() - MARGIN
                    : MARGIN;

            drawView(canvas, messageView, currentY, xOffset);

            currentY += itemHeight + MESSAGE_SPACING;
        }

        int footerY = totalHeight - footerHeight - MARGIN;
        drawView(canvas, footerView, footerY, 0);

        pdfDocument.finishPage(page);

        File downloads = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
        );
        File pdfFile = new File(downloads, "chat_export.pdf");

        try (FileOutputStream fos = new FileOutputStream(pdfFile)) {
            pdfDocument.writeTo(fos);
        }

        pdfDocument.close();

        // -------------------- FIX: запуск sharePdf на UI-потоке --------------------
        activity.runOnUiThread(() -> sharePdf(pdfFile));
    }

    private int calculateTotalHeight(LayoutInflater inflater, List<Message> messages,
                                     int headerHeight, int footerHeight) {

        int totalHeight = headerHeight + MARGIN;

        for (Message message : messages) {
            int layoutRes = messageController.isMyMessage(message)
                    ? R.layout.my_message
                    : R.layout.other_message;

            View messageView = inflater.inflate(layoutRes, null, false);

            TextView messageText = messageView.findViewById(R.id.message_text);
            messageText.setText(message.getText());
            messageText.setMaxWidth((int) (PAGE_WIDTH * 0.8));

            int widthMeasureSpec =
                    View.MeasureSpec.makeMeasureSpec(PAGE_WIDTH, View.MeasureSpec.AT_MOST);
            int heightMeasureSpec =
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            messageView.measure(widthMeasureSpec, heightMeasureSpec);

            totalHeight += messageView.getMeasuredHeight() + MESSAGE_SPACING;
        }

        if (!messages.isEmpty()) totalHeight -= MESSAGE_SPACING;

        totalHeight += footerHeight;
        totalHeight += MARGIN;

        return totalHeight;
    }

    private View createHeaderView(LayoutInflater inflater, String chatTitle) {
        View headerView = inflater.inflate(R.layout.pdf_header, null);
        TextView titleText = headerView.findViewById(R.id.chat_title_text);
        titleText.setText(chatTitle);

        headerView.setLayoutParams(new LinearLayout.LayoutParams(
                PAGE_WIDTH,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return headerView;
    }

    private View createFooterView(LayoutInflater inflater) {
        View footerView = inflater.inflate(R.layout.pdf_footer, null);

        TextView logoLine1 = footerView.findViewById(R.id.logo_line_1);
        TextView logoLine2 = footerView.findViewById(R.id.logo_line_2);

        logoLine1.setText("Диалог из AIChat");
        logoLine2.setText("Установить сейчас");

        footerView.setLayoutParams(new LinearLayout.LayoutParams(
                PAGE_WIDTH,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        return footerView;
    }

    private int measureView(View view) {
        int widthMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(PAGE_WIDTH, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        view.measure(widthMeasureSpec, heightMeasureSpec);
        view.layout(0, 0,
                view.getMeasuredWidth(),
                view.getMeasuredHeight());

        return view.getMeasuredHeight();
    }

    private void drawView(Canvas canvas, View view, int yPosition, int xPosition) {
        canvas.save();
        canvas.translate(xPosition, yPosition);
        view.draw(canvas);
        canvas.restore();
    }

    private void sharePdf(File pdfFile) {
        Uri uri = FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".provider",
                pdfFile
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        activity.startActivity(
                Intent.createChooser(intent, "Поделиться чатом (PDF)")
        );
    }
}
