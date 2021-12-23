package net.twilken.amsupd;

import android.media.MediaScannerConnection;
import android.net.Uri;

import java.util.function.Consumer;

public class UpdaterMediaScannerClient implements MediaScannerConnection.MediaScannerConnectionClient {
    private final Consumer<String> notifyProgress;

    public UpdaterMediaScannerClient(Consumer<String> notifyProgress) {
        this.notifyProgress = notifyProgress;
    }

    @Override
    public void onMediaScannerConnected() {
        this.notifyProgress.accept("UpdaterMediaScannerClient connected\n");
    }

    @Override
    public void onScanCompleted(String s, Uri uri) {
        if (uri == null) {
            this.notifyProgress.accept(String.format("FAILED TO SCAN %s\n", s));
        } else {
            this.notifyProgress.accept(String.format("SCANNED %s -> %s\n", s, uri));
        }
    }
}
