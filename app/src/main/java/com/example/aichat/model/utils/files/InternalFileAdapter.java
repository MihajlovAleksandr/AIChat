package com.example.aichat.model.utils.files;

import android.content.Context;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    public InternalFileAdapter(
            List<Uri> fileUris,
            boolean isImageMode,
            OnFileInteractionListener listener,
            Context context
    ) {
        this.fileUris = fileUris;
        this.isImageMode = isImageMode;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view;

        if (isImageMode) {
            view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(
                            R.layout.item_image_grid,
                            parent,
                            false
                    );
        } else {
            view = LayoutInflater
                    .from(parent.getContext())
                    .inflate(
                            R.layout.item_file,
                            parent,
                            false
                    );
        }

        return new ViewHolder(
                view,
                isImageMode
        );
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {
        Uri uri =
                fileUris.get(
                        position
                );

        if (isImageMode) {
            bindImageItem(
                    holder,
                    uri
            );
        } else {
            bindFileItem(
                    holder,
                    uri
            );
        }
    }

    private void bindImageItem(
            @NonNull ViewHolder holder,
            @NonNull Uri uri
    ) {
        if (holder.imageView != null) {
            Glide.with(
                            holder.itemView.getContext()
                    )
                    .load(
                            uri
                    )
                    .placeholder(
                            R.drawable.ic_image
                    )
                    .error(
                            R.drawable.ic_cancel
                    )
                    .centerCrop()
                    .into(
                            holder.imageView
                    );
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(
                        uri
                );
            }
        });

        if (holder.deleteButton != null) {
            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(
                            uri
                    );
                }
            });

            holder.deleteButton.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void bindFileItem(
            @NonNull ViewHolder holder,
            @NonNull Uri uri
    ) {
        String fileName =
                getFileName(
                        uri
                );

        String fileSize =
                getFileSize(
                        uri
                );

        String lastModified =
                getLastModified(
                        uri
                );

        if (holder.fileNameText != null) {
            holder.fileNameText.setText(
                    fileName
            );
        }

        if (holder.fileSizeText != null) {
            holder.fileSizeText.setText(
                    fileSize
            );
        }

        if (holder.fileExtText != null) {
            String extension =
                    getFileExtension(
                            fileName
                    );

            if (extension.isEmpty()) {
                holder.fileExtText.setVisibility(
                        View.GONE
                );
            } else {
                holder.fileExtText.setVisibility(
                        View.VISIBLE
                );

                holder.fileExtText.setText(
                        extension
                );
            }
        }

        if (holder.fileDateText != null) {
            if (lastModified == null || lastModified.trim().isEmpty()) {
                holder.fileDateText.setVisibility(
                        View.GONE
                );
            } else {
                holder.fileDateText.setVisibility(
                        View.VISIBLE
                );

                holder.fileDateText.setText(
                        lastModified
                );
            }
        }

        if (holder.fileIcon != null) {
            setFileIcon(
                    holder.fileIcon,
                    fileName
            );
        }

        applyFileTheme(
                holder
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFileClick(
                        uri
                );
            }
        });

        /*
         * В новом item_file.xml нет delete_button.
         * Вместо него используется file_menu — кнопка действий справа.
         */
        if (holder.fileActionButton != null) {
            holder.fileActionButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(
                            uri
                    );
                }
            });

            holder.fileActionButton.setVisibility(
                    View.VISIBLE
            );
        }
    }

    @Override
    public int getItemCount() {
        return fileUris != null
                ? fileUris.size()
                : 0;
    }

    private String getFileName(@NonNull Uri uri) {
        if (context == null) {
            return "File";
        }

        String fileName =
                null;

        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         context
                                 .getContentResolver()
                                 .query(
                                         uri,
                                         null,
                                         null,
                                         null,
                                         null
                                 )) {

                if (cursor != null && cursor.moveToFirst()) {
                    int index =
                            cursor.getColumnIndex(
                                    android.provider.OpenableColumns.DISPLAY_NAME
                            );

                    if (index != -1) {
                        fileName =
                                cursor.getString(
                                        index
                                );
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (fileName == null) {
            fileName =
                    uri.getLastPathSegment();
        }

        return fileName != null
                ? fileName
                : "Unknown";
    }

    private String getFileSize(@NonNull Uri uri) {
        if (context == null) {
            return "";
        }

        long size =
                0L;

        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         context
                                 .getContentResolver()
                                 .query(
                                         uri,
                                         null,
                                         null,
                                         null,
                                         null
                                 )) {

                if (cursor != null && cursor.moveToFirst()) {
                    int index =
                            cursor.getColumnIndex(
                                    android.provider.OpenableColumns.SIZE
                            );

                    if (index != -1) {
                        size =
                                cursor.getLong(
                                        index
                                );
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (size <= 0L && "file".equals(uri.getScheme())) {
            try {
                String path =
                        uri.getPath();

                if (path != null) {
                    File file =
                            new File(
                                    path
                            );

                    if (file.exists()) {
                        size =
                                file.length();
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return formatFileSize(
                size
        );
    }

    private String getLastModified(@NonNull Uri uri) {
        if (context == null) {
            return "";
        }

        try {
            String path =
                    getRealPathFromUri(
                            uri
                    );

            if (path == null && "file".equals(uri.getScheme())) {
                path =
                        uri.getPath();
            }

            if (path != null) {
                File file =
                        new File(
                                path
                        );

                if (file.exists()) {
                    long lastModified =
                            file.lastModified();

                    SimpleDateFormat formatter =
                            new SimpleDateFormat(
                                    "dd.MM.yyyy HH:mm",
                                    Locale.getDefault()
                            );

                    return formatter.format(
                            new Date(
                                    lastModified
                            )
                    );
                }
            }
        } catch (Exception ignored) {
        }

        return "";
    }

    @Nullable
    private String getRealPathFromUri(@NonNull Uri uri) {
        if (context == null) {
            return null;
        }

        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor =
                         context
                                 .getContentResolver()
                                 .query(
                                         uri,
                                         null,
                                         null,
                                         null,
                                         null
                                 )) {

                if (cursor != null && cursor.moveToFirst()) {
                    int index =
                            cursor.getColumnIndex(
                                    "_data"
                            );

                    if (index != -1) {
                        return cursor.getString(
                                index
                        );
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String formatFileSize(long size) {
        if (size <= 0L) {
            return "";
        }

        final String[] units =
                new String[]{
                        "B",
                        "KB",
                        "MB",
                        "GB",
                        "TB"
                };

        int digitGroups =
                (int) (
                        Math.log10(
                                size
                        )
                                / Math.log10(
                                1024
                        )
                );

        digitGroups =
                Math.max(
                        0,
                        Math.min(
                                digitGroups,
                                units.length - 1
                        )
                );

        return String.format(
                Locale.getDefault(),
                "%.1f %s",
                size / Math.pow(
                        1024,
                        digitGroups
                ),
                units[digitGroups]
        );
    }

    @NonNull
    private String getFileExtension(@Nullable String fileName) {
        if (fileName == null) {
            return "";
        }

        int dotIndex =
                fileName.lastIndexOf(
                        '.'
                );

        if (dotIndex < 0 || dotIndex >= fileName.length() - 1) {
            return "";
        }

        return fileName
                .substring(
                        dotIndex + 1
                )
                .toUpperCase(
                        Locale.getDefault()
                );
    }

    private void setFileIcon(
            @NonNull ImageView iconView,
            @Nullable String fileName
    ) {
        iconView.setImageResource(
                R.drawable.ic_file
        );
    }

    private void applyFileTheme(@NonNull ViewHolder holder) {
        Context itemContext =
                holder.itemView.getContext();

        int primary =
                resolveThemeColor(
                        itemContext,
                        com.google.android.material.R.attr.colorPrimary,
                        0xFF20A39A
                );

        int onSurface =
                resolveThemeColor(
                        itemContext,
                        com.google.android.material.R.attr.colorOnSurface,
                        0xFF1A1A1A
                );

        int onSurfaceVariant =
                resolveThemeColor(
                        itemContext,
                        com.google.android.material.R.attr.colorOnSurfaceVariant,
                        0xFF666666
                );

        if (holder.fileIcon != null) {
            holder.fileIcon.setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        }

        if (holder.fileStatus != null) {
            holder.fileStatus.setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        }

        if (holder.fileActionButton != null) {
            holder.fileActionButton.setImageTintList(
                    ColorStateList.valueOf(
                            primary
                    )
            );
        }

        if (holder.fileNameText != null) {
            holder.fileNameText.setTextColor(
                    onSurface
            );
        }

        if (holder.fileSizeText != null) {
            holder.fileSizeText.setTextColor(
                    onSurfaceVariant
            );
        }

        if (holder.fileExtText != null) {
            holder.fileExtText.setTextColor(
                    onSurfaceVariant
            );
        }

        if (holder.fileDateText != null) {
            holder.fileDateText.setTextColor(
                    onSurfaceVariant
            );
        }

        if (holder.fileProgressText != null) {
            holder.fileProgressText.setTextColor(
                    primary
            );
        }
    }

    private int resolveThemeColor(
            @NonNull Context context,
            int attr,
            int fallback
    ) {
        try {
            android.util.TypedValue typedValue =
                    new android.util.TypedValue();

            boolean resolved =
                    context
                            .getTheme()
                            .resolveAttribute(
                                    attr,
                                    typedValue,
                                    true
                            );

            if (resolved) {
                return typedValue.data;
            }

        } catch (Exception ignored) {
        }

        return fallback;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imageView;
        ImageView deleteButton;

        TextView fileNameText;
        TextView fileSizeText;
        TextView fileExtText;
        TextView fileDateText;
        TextView fileProgressText;

        ImageView fileIcon;
        ImageView fileStatus;
        ImageView fileActionButton;

        ViewHolder(
                @NonNull View itemView,
                boolean isImageMode
        ) {
            super(
                    itemView
            );

            if (isImageMode) {
                imageView =
                        itemView.findViewById(
                                R.id.image_view
                        );

                deleteButton =
                        itemView.findViewById(
                                R.id.delete_button
                        );

            } else {

                fileNameText =
                        itemView.findViewById(
                                R.id.file_name
                        );

                fileSizeText =
                        itemView.findViewById(
                                R.id.file_size
                        );

                fileExtText =
                        itemView.findViewById(
                                R.id.file_ext
                        );

                fileDateText =
                        itemView.findViewById(
                                R.id.file_date
                        );

                fileProgressText =
                        itemView.findViewById(
                                R.id.file_progress_text
                        );

                fileIcon =
                        itemView.findViewById(
                                R.id.play_icon
                        );

                fileStatus =
                        itemView.findViewById(
                                R.id.file_status
                        );

                fileActionButton =
                        itemView.findViewById(
                                R.id.file_menu
                        );
            }
        }
    }
}
