package ru.babay.lib.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import org.json.JSONArray;
import ru.babay.lib.BugHandler;
import ru.babay.lib.Settings;
import ru.babay.lib.transport.ImageCache;
import ru.babay.lib.transport.TTL;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 13.12.12
 * Time: 16:16
 */
public class Util {
    private static final long WEEK = 7 * 24 * 60 * 60 * 1000;
    private static final SimpleDateFormat weekFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
    private static final String KEY = "s %wp)8E_APk#7!K]>9M;%PC_e_OP%";

    public enum ScreenSize {Small, Medium, Large}

    private static ScreenSize sScreenSize;

    public static String formatDate(Calendar date) {
        if (date == null)
            return "";

        if (isToday(date))
            return String.format("%d:%02d", date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE));
        else if (System.currentTimeMillis() - date.getTimeInMillis() < WEEK)
            return weekFormat.format(date.getTime());
        else
            return dateFormat.format(date.getTime());
    }

    public static boolean isToday(Calendar date) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(System.currentTimeMillis());

        return now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
                && now.get(Calendar.YEAR) == date.get(Calendar.YEAR);
    }


    public static String md5(String s) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes(), 0, s.length());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String MD5(String s){
        return md5(Long.toHexString(WEEK) + s);
    }

    public static Bitmap getResizedBitmap(File file, int maxSize) throws IOException {
        if (maxSize == 0)
            maxSize = Integer.MAX_VALUE;

        final int REQUIRED_SIZE = maxSize;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        BitmapFactory.decodeStream(new FileInputStream(file), null, o);

        //Find the correct scale value. It should be the power of 2.
        int scale = 1;
        while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
            scale *= 2;

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        o2.inDither = false;                     //Disable Dithering mode
        o2.inPurgeable = true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
        o2.inInputShareable = true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future

        FileInputStream fs = new FileInputStream(file);
        Bitmap source = BitmapFactory.decodeFileDescriptor(fs.getFD(), null, o2);

        if (o.outWidth < maxSize && o.outHeight < maxSize)
            return source;

        Bitmap resizedBitmap = resizeBitmap(source, maxSize, 0);
        source.recycle();
        return resizedBitmap;
    }

    public static Bitmap getResizedBitmap(File file, int maxWidth, int maxHeight) throws IOException {
        //if (maxWidth == 0)
        //    maxWidth = Integer.MAX_VALUE;
        //if (maxHeight == 0)
        //    maxHeight = Integer.MAX_VALUE;
        if (maxWidth == 0 && maxHeight == 0)
            maxWidth = maxHeight = Integer.MAX_VALUE;


        //final int REQUIRED_SIZE = maxSize;
        final int REQUIRED_WIDTH = maxWidth;
        final int REQUIRED_HEIGHT = maxHeight;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        FileInputStream fs = new FileInputStream(file);
        BitmapFactory.decodeStream(fs, null, o);
        fs.close();

        //Find the correct scale value. It should be the power of 2.
        int scale = 1;
        while (o.outWidth / scale / 2 >= REQUIRED_WIDTH && o.outHeight / scale / 2 >= REQUIRED_HEIGHT)
            scale *= 2;

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        o2.inDither = false;                     //Disable Dithering mode
        o2.inPurgeable = true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
        o2.inInputShareable = true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future

        fs = new FileInputStream(file);
        Bitmap source = BitmapFactory.decodeFileDescriptor(fs.getFD(), null, o2);
        fs.close();

        if (o.outWidth < maxWidth && o.outHeight < maxHeight)
            return source;

        //Bitmap resizedBitmap = resizeBitmap(source, maxWidth, maxHeight, 0);
        //source.recycle();

        //System.gc();
        return source;
    }

    public static Bitmap resizeBitmap(Bitmap source, int maxSize, int rotation) {
        Matrix matrix = new Matrix();
        if (maxSize != 0) {
            float currentMaxSize = Math.max(source.getWidth(), source.getHeight());
            float scale = maxSize / currentMaxSize;
            matrix.postScale(scale, scale);
        }
        if (rotation != 0)
            matrix.postRotate(rotation);

        //int width = Math.round(source.getWidth() * scale);
        //int height = Math.round(source.getHeight() * scale);

        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap resizeBitmap(Bitmap source, int maxWidth, int maxHeight, int rotation) {
        Matrix matrix = new Matrix();

        if (maxWidth != 0 || maxHeight != 0) {
            if (maxWidth == 0)
                maxWidth = Integer.MAX_VALUE;
            if (maxHeight == 0)
                maxHeight = Integer.MAX_VALUE;

            float scaleX = maxWidth / (float)source.getWidth();
            float scaleY = maxHeight / (float)source.getHeight();
            float scale = Math.min(scaleX, scaleY);
            matrix.postScale(scale, scale);
        }
        if (rotation != 0)
            matrix.postRotate(rotation);

        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static void removeFilesInFolder(File dir) {
        for (File file : dir.listFiles()) {
            file.delete();
        }
    }

    /*public static String getCommentAmountStr(ItemBase item, Context context) {
        int amount = item.getCommentsAmount();
        int amountMod10 = amount % 10;
        int amountMod100 = amount % 100;
        if (amountMod10 > 4 || amountMod10 == 0 || (amountMod100 < 20 && amountMod100 > 10))
            return context.getString(R.string.commentCount5, item.getCommentsAmount());
        else if (amountMod10 == 1)
            return context.getString(R.string.commentCount1, item.getCommentsAmount());
        else return context.getString(R.string.commentCount2, item.getCommentsAmount());
    }*/

    public static ScreenSize getScreenSize(Context context) {
        if (sScreenSize == null) {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            float width = dm.widthPixels / dm.xdpi;
            if (width < 2.02)
                sScreenSize = ScreenSize.Small;
            else if (width < 2.5f)
                sScreenSize = ScreenSize.Medium;
            else sScreenSize = ScreenSize.Large;
        }

        return sScreenSize;
    }

    public static File getInternalStoragePath(Context context) {
        File file = context.getFilesDir();
        file.mkdir();
        return file;
    }

    public static File getExternalStoragePath(Context context) {
        File file = Environment.getExternalStorageDirectory();
        file = new File(file, "Android");
        file.mkdir();
        file = new File(file, "data");
        file.mkdir();
        file = new File(file, context.getPackageName());
        file.mkdir();

        File nomedia = new File(file, ".nomedia");
        if (!nomedia.exists())
            try {
                nomedia.createNewFile();
            } catch (IOException e) {
            }

        return file;
    }

    public static void cleanupImageCacheDir(File dir) {
        deleteRecursive(new File(dir, ImageCache.getFolderFor(TTL.Day)));
        deleteRecursive(new File(dir, ImageCache.getFolderFor(TTL.TwoDays)));
        deleteRecursive(new File(dir, ImageCache.getFolderFor(TTL.Week)));
    }

    static void deleteRecursive(File fileOrDirectory) {
        if (!fileOrDirectory.exists())
            return;

        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    static public boolean hasExternalStorage(boolean requireWriteAccess) {
        //TODO: After fix the bug,  add "if (VERBOSE)" before logging errors.
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return !requireWriteAccess || checkFsWritable();
        } else if (!requireWriteAccess && Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private static boolean checkFsWritable() {
        // Create a temporary file to see whether a volume is really writeable.
        // It's important not to put it in the root directory which may have a
        // limit on the number of files.
        String directoryName = Environment.getExternalStorageDirectory().toString() + "/Android";
        File directory = new File(directoryName);
        if (!directory.isDirectory()) {
            if (!directory.mkdirs()) {
                return false;
            }
        }
        return directory.canWrite();
    }


    public static String readFully(InputStream inputStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return new String(baos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] readFullyAsByteArray(File file) {
        try {
            return readFullyAsByteArray(new FileInputStream(file));
        } catch (Exception e) {
            return null;
        }
    }


    public static byte[] readFullyAsByteArray(InputStream inputStream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONArray readJsonArray(Context context, int fileId) {
        try {
            InputStream is = context.getResources().openRawResource(fileId);
            Writer writer = new StringWriter();
            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }

            return new JSONArray(writer.toString());
        } catch (Exception e) {
            return null;
        }
    }

    public static File makePathAndGetFile(Context context, String filePath, boolean isExternal) {
        File file = isExternal ? getExternalStoragePath(context) : getInternalStoragePath(context);

        String[] array = filePath.split("/");
        for (int t = 0; t < array.length - 1; t++) {
            file = new File(file, array[t]);
            file.mkdir();
        }

        return new File(file, array[array.length - 1]);
    }

    public static boolean scaleAndRotiteImage(File fileSource, File fileDest, final int MAX_SIZE) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new FileInputStream(fileSource), null, o);

        int angle = getOrientationFix(fileSource.getPath());

        if (angle == 0 && o.outWidth <= MAX_SIZE && o.outHeight <= MAX_SIZE)
            return false; // don't need resize

        Bitmap resized = null;

        if (o.outWidth <= MAX_SIZE && o.outHeight <= MAX_SIZE) {
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inDither = false;                     //Disable Dithering mode
            o2.inPurgeable = true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
            o2.inInputShareable = true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future

            FileInputStream fs = new FileInputStream(fileSource);
            Bitmap source = BitmapFactory.decodeFileDescriptor(fs.getFD(), null, o2);
            resized = Util.resizeBitmap(source, 0, angle);
            source.recycle();
        } else {
            //final int REQUIRED_SIZE = Settings.MAX_IMAGE_SIZE;

            //Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= MAX_SIZE && o.outHeight / scale / 2 >= MAX_SIZE)
                scale *= 2;

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            o2.inDither = false;                     //Disable Dithering mode
            o2.inPurgeable = true;                   //Tell to gc that whether it needs free memory, the Bitmap can be cleared
            o2.inInputShareable = true;              //Which kind of reference will be used to recover the Bitmap data after being clear, when it will be used in the future

            FileInputStream fs = new FileInputStream(fileSource);
            Bitmap source = BitmapFactory.decodeFileDescriptor(fs.getFD(), null, o2);

            //resize and save
            resized = Util.resizeBitmap(source, MAX_SIZE, angle);
            source.recycle();
        }

        if (resized != null) {
            FileOutputStream out = new FileOutputStream(fileDest);
            resized.compress(Bitmap.CompressFormat.JPEG, 70, out);
            resized.recycle();
            return true;
        }
        return false;
    }

    static int getOrientationFix(String path) {

        //6 - screen normal, need 90 CW
        //3 - rotited 90 cw, need 180CW
        //1 - rotated 90 ccw, need 0
        // 8 - rotated 180,  need 270 cw


        try {
            ExifInterface exif = new ExifInterface(path);     //Since API Level 5
            String exifOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            if ("6".equals(exifOrientation))
                return 90;
            if ("3".equals(exifOrientation))
                return 180;
            if ("8".equals(exifOrientation))
                return 270;
        } catch (IOException e) {
            BugHandler.logE(e);
        }
        return 0;
    }

    public static void copy(InputStream in, OutputStream out) throws IOException, InterruptedException {
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len != -1) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }
}
