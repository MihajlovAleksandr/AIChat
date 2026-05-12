package com.example.aichat;

import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.aichat.model.utils.GlideAuthHelper;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;

import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.VH> {

    private final List<String> urls;

    public ImagePagerAdapter(List<String> urls) {
        this.urls = urls;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        PhotoView view = new PhotoView(parent.getContext());
        view.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        view.setScaleType(PhotoView.ScaleType.FIT_CENTER);

        // Настройка масштабирования
        view.setMinimumScale(1.0f);
        view.setMediumScale(2.5f);
        view.setMaximumScale(5.0f);
        view.setZoomTransitionDuration(200);

        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PhotoView photoView = holder.image;
        String url = urls.get(position);

        // Сброс масштаба перед загрузкой нового изображения
        photoView.setScale(1.0f, false);

        // Загрузка изображения
        Glide.with(photoView)
                .load(GlideAuthHelper.build(url, photoView.getContext()))
                .placeholder(R.drawable.ic_image_placeholder) // добавьте плейсхолдер
                .error(R.drawable.ic_image_broken) // добавьте иконку ошибки
                .into(photoView);

        // Настройка обработки жестов для совместимости с ViewPager2
        setupTouchHandling(photoView);
    }

    private void setupTouchHandling(PhotoView photoView) {
        final ViewPager2 viewPager = findViewPager(photoView);
        if (viewPager == null) return;

        final float[] startX = new float[1];
        final float[] startY = new float[1];
        final float[] startScale = new float[1];

        // Получаем PhotoViewAttacher для отслеживания масштаба
        PhotoViewAttacher attacher = new PhotoViewAttacher(photoView);
        attacher.setOnScaleChangeListener((scaleFactor, focusX, focusY) -> {
            startScale[0] = photoView.getScale();
        });

        photoView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = event.getX();
                    startY[0] = event.getY();
                    startScale[0] = photoView.getScale();
                    // Запрещаем ViewPager2 перехватывать события при масштабировании
                    if (startScale[0] <= photoView.getMinimumScale() + 0.01f) {
                        viewPager.requestDisallowInterceptTouchEvent(false);
                    } else {
                        viewPager.requestDisallowInterceptTouchEvent(true);
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    float currentScale = photoView.getScale();
                    float dx = Math.abs(event.getX() - startX[0]);
                    float dy = Math.abs(event.getY() - startY[0]);

                    // Если изображение масштабировано - блокируем свайп ViewPager2
                    if (currentScale > photoView.getMinimumScale() + 0.01f) {
                        viewPager.requestDisallowInterceptTouchEvent(true);
                    }
                    // Если нет - разрешаем свайп при горизонтальном движении
                    else if (dx > dy && dx > 10) {
                        viewPager.requestDisallowInterceptTouchEvent(false);
                    } else {
                        viewPager.requestDisallowInterceptTouchEvent(true);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Возвращаем нормальное поведение
                    viewPager.requestDisallowInterceptTouchEvent(false);
                    break;
            }

            // Важно: возвращаем false, чтобы PhotoView сам обрабатывал жесты
            return false;
        });
    }

    private ViewPager2 findViewPager(PhotoView photoView) {
        android.view.ViewParent parent = photoView.getParent();

        while (parent != null) {
            if (parent instanceof ViewPager2) {
                return (ViewPager2) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return urls != null ? urls.size() : 0;
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        // Очищаем ресурсы Glide при переиспользовании
        if (holder.image != null) {
            Glide.with(holder.image).clear(holder.image);
        }
    }

    public static class VH extends RecyclerView.ViewHolder {
        public final PhotoView image;

        public VH(@NonNull PhotoView itemView) {
            super(itemView);
            this.image = itemView;
        }
    }
}