package com.example.aichat.model.utils;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aichat.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InternalFileAdapter extends RecyclerView.Adapter<InternalFileAdapter.ViewHolder> {

    private final List<Uri> fileUris;
    private final boolean isImageMode;
    private final OnFileInteractionListener listener;
    private final Context context;

    public interface OnFileInteractionListener {
        void onFileClick(Uri uri);
        void onDeleteClick(Uri uri);
    }

    public InternalFileAdapter(List<Uri> fileUris,
                               boolean isImageMode,
                               OnFileInteractionListener listener,
                               Context context) {
        this.fileUris = fileUris;
        this.isImageMode = isImageMode;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (isImageMode) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image_grid, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_file, parent, false);
        }
        return new ViewHolder(view, isImageMode);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = fileUris.get(position);

        if (isImageMode) {
            Glide.with(holder.itemView.getContext())
                    .load(uri)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_cancel)
                    .centerCrop()
                    .into(holder.imageView);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFileClick(uri);
            });

            if (holder.deleteButton != null) {
                holder.deleteButton.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(uri);
                });
                holder.deleteButton.setVisibility(View.VISIBLE);
            }
        } else {
            String fileName = getFileName(uri);
            String fileSize = getFileSize(uri);
            String lastModified = getLastModified(uri);

            if (holder.fileNameText != null) {
                holder.fileNameText.setText(fileName);
            }

            if (holder.fileSizeText != null) {
                holder.fileSizeText.setText(fileSize);
            }

            if (holder.fileDateText != null) {
                holder.fileDateText.setText(lastModified);
            }

            if (holder.fileIcon != null) {
                setFileIcon(holder.fileIcon, fileName);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFileClick(uri);
            });

            if (holder.fileDeleteButton != null) {
                holder.fileDeleteButton.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(uri);
                });
                holder.fileDeleteButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return fileUris != null ? fileUris.size() : 0;
    }

    private String getFileName(Uri uri) {
        if (context == null) return "File";

        String fileName = null;

        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         context.getContentResolver().query(uri, null, null, null, null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        fileName = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }

        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }

        return fileName != null ? fileName : "Unknown";
    }

    private String getFileSize(Uri uri) {
        if (context == null) return "";

        long size = 0;

        try (android.database.Cursor cursor =
                     context.getContentResolver().query(uri, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (index != -1) {
                    size = cursor.getLong(index);
                }
            }
        } catch (Exception ignored) {}

        return formatFileSize(size);
    }

    private String getLastModified(Uri uri) {
        if (context == null) return "";

        try {
            String path = getRealPathFromUri(uri);

            if (path != null) {
                File file = new File(path);

                if (file.exists()) {
                    long lastModified = file.lastModified();
                    SimpleDateFormat sdf =
                            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    return sdf.format(new Date(lastModified));
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    private String getRealPathFromUri(Uri uri) {
        if (context == null) return null;

        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         context.getContentResolver().query(uri, null, null, null, null)) {

                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex("_data");
                    if (index != -1) {
                        return cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format(Locale.getDefault(), "%.1f %s",
                size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void setFileIcon(ImageView iconView, String fileName) {
        iconView.setImageResource(R.drawable.ic_file);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView deleteButton;

        TextView fileNameText;
        TextView fileSizeText;
        TextView fileDateText;
        ImageView fileIcon;
        ImageView fileDeleteButton;

        ViewHolder(@NonNull View itemView, boolean isImageMode) {
            super(itemView);

            if (isImageMode) {
                imageView = itemView.findViewById(R.id.image_view);
                deleteButton = itemView.findViewById(R.id.delete_button);
            } else {
                fileNameText = itemView.findViewById(R.id.file_name);
                fileSizeText = itemView.findViewById(R.id.file_size);
                fileDateText = itemView.findViewById(R.id.file_date);
                fileIcon = itemView.findViewById(R.id.file_icon);
                fileDeleteButton = itemView.findViewById(R.id.delete_button);
            }
        }
    }
}