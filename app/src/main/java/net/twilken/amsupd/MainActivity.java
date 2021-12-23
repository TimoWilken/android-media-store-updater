package net.twilken.amsupd;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    @NonNull
    private static ArrayList<File> listFilesRecursively(@NonNull File directory) {
        ArrayList<File> result = new ArrayList<>(128);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    result.addAll(listFilesRecursively(file));
                } else {
                    result.add(file);
                }
            }
        }
        return result;
    }

    public void appendText(@NonNull String text) {
        TextView scrollingText = this.findViewById(R.id.scrollingText);
        scrollingText.post(() -> scrollingText.append(text));
    }

    public void onClickRun(@NonNull View button) {
        if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            this.updateMediaStore();
        } else {
            Toast.makeText(this.getBaseContext(), "Need storage permission", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMediaStore() {
        ((TextView)this.findViewById(R.id.scrollingText)).setText("");
        MediaScannerConnection connection = new MediaScannerConnection(this.getBaseContext(),
                new UpdaterMediaScannerClient(this::appendText));
        connection.connect();
        for (String imageDir : ((EditText)this.findViewById(R.id.scanDirs)).getText().toString().split("\n")) {
            for (File image : listFilesRecursively(new File(Environment.getExternalStorageDirectory(), imageDir))) {
                String path = image.getAbsolutePath(), mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(path).toLowerCase(Locale.ROOT));
                if (mimeType == null) {
                    this.appendText(String.format("NOT SCANNING %s: unknown MIME type\n", path));
                } else {
                    this.appendText(String.format("SCAN %s (%s)\n", path, mimeType));
                    connection.scanFile(path, mimeType);
                }
            }
        }
        connection.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ScrollView scroller = this.findViewById(R.id.scroller);
        FloatingActionButton scrollFab = this.findViewById(R.id.scrollFab);
        scroller.setOnScrollChangeListener((view, x, y, oldX, oldY) -> {
            String hint;
            int image;
            if (view.canScrollVertically(+1)) {
                // We can scroll further down.
                hint = this.getString(R.string.scroll_to_bottom_hint);
                image = android.R.drawable.stat_sys_download;
            } else {
                // Already at the bottom, scroll to the top instead.
                hint = this.getString(R.string.scroll_to_top_hint);
                image = android.R.drawable.stat_sys_upload;
            }
            scrollFab.setTooltipText(hint);
            scrollFab.setContentDescription(hint);
            scrollFab.setImageResource(image);
        });
        ((TextView)this.findViewById(R.id.scrollingText)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                scroller.post(() -> scroller.fullScroll(View.FOCUS_DOWN));
            }
        });

        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (this.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> Toast.makeText(this.getBaseContext(), isGranted ?
                            "Permission granted" : "Permission denied", Toast.LENGTH_SHORT).show()
            ).launch(permission);
        }
    }

    public void onClickScroll(@NonNull View scrollButton) {
        ScrollView scroller = this.findViewById(R.id.scroller);
        if (scroller.canScrollVertically(+1)) {
            // We can scroll further down, so do that.
            scroller.post(() -> scroller.fullScroll(View.FOCUS_DOWN));
        } else {
            // Already at the bottom, scroll to the top instead.
            scroller.post(() -> scroller.fullScroll(View.FOCUS_UP));
        }
    }
}
