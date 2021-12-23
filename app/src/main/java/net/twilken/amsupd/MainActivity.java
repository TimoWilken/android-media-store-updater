package net.twilken.amsupd;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {
    private final Lock textUpdateLock = new ReentrantLock();

    @NonNull
    private static ArrayList<File> listFilesRecursively(@NonNull File directory) {
        ArrayList<File> result = new ArrayList<>(128);
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            if (file.isDirectory()) {
                result.addAll(listFilesRecursively(file));
            } else {
                result.add(file);
            }
        }
        return result;
    }

    public void appendText(@NonNull String text) {
        textUpdateLock.lock();
        try {
            this.runOnUiThread(() -> {
                ((TextView)this.findViewById(R.id.scrollingText)).append(text);
                ScrollView scroller = this.findViewById(R.id.scroller);
                scroller.post(() -> scroller.fullScroll(View.FOCUS_DOWN));
            });
        } finally {
            textUpdateLock.unlock();
        }
    }

    public void onClickRun(@NonNull View button) {
        if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            this.updateMediaStore();
        } else {
            Toast.makeText(this.getBaseContext(), "Need storage permission", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMediaStore() {
        MediaScannerConnection connection = new MediaScannerConnection(this.getBaseContext(),
                new UpdaterMediaScannerClient(this::appendText));
        connection.connect();
        for (String imageDir : new String[]{"DCIM/Camera", "Pictures"}) {
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
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (this.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> Toast.makeText(this.getBaseContext(), isGranted ?
                            "Permission granted" : "Permission denied", Toast.LENGTH_SHORT).show()
            ).launch(permission);
        }
    }
}
