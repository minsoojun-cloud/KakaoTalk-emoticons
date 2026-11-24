package com.minsoojun.kakaostickerstudio;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Lightweight ContentProvider serving camera capture temp files and shared sticker PNG/ZIP files
 * from getCacheDir()/shared_files/ via content://com.minsoojun.kakaostickerstudio.fileprovider/...
 */
public class StickerFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.minsoojun.kakaostickerstudio.fileprovider";

    public static Uri getUriForFile(File file) {
        return new Uri.Builder()
                .scheme("content")
                .authority(AUTHORITY)
                .path(file.getName())
                .build();
    }

    private File getSharedDir() {
        File dir = new File(getContext().getCacheDir(), "shared_files");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private File resolveFile(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("..") || name.contains("/")) {
            throw new FileNotFoundException("Invalid URI path: " + uri);
        }
        return new File(getSharedDir(), name);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = resolveFile(uri);
            if (projection == null) {
                projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
            }
            String[] cols = new String[projection.length];
            Object[] values = new Object[projection.length];
            int i = 0;
            for (String col : projection) {
                if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                    cols[i] = OpenableColumns.DISPLAY_NAME;
                    values[i++] = file.getName();
                } else if (OpenableColumns.SIZE.equals(col)) {
                    cols[i] = OpenableColumns.SIZE;
                    values[i++] = file.length();
                }
            }
            MatrixCursor cursor = new MatrixCursor(cols, 1);
            cursor.addRow(values);
            return cursor;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                String ext = name.substring(dot + 1).toLowerCase();
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                if (mime != null) {
                    return mime;
                }
            }
        }
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No external inserts");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            File file = resolveFile(uri);
            return file.delete() ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("No external updates");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = resolveFile(uri);
        int fileMode = ParcelFileDescriptor.MODE_READ_ONLY;
        if ("w".equals(mode) || "wt".equals(mode)) {
            fileMode = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("rw".equals(mode) || "rwt".equals(mode)) {
            fileMode = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
        }
        return ParcelFileDescriptor.open(file, fileMode);
    }
}
