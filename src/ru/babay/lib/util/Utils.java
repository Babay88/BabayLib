package ru.babay.lib.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.StatFs;
import android.util.Pair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.xml.sax.XMLReader;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 23.04.13
 * Time: 22:07
 */
public class Utils {
    //private static Map<Pattern,String> patternMap = new HashMap<Pattern , String>();
    private static List<Pair<Pattern, String>>  patternList = new ArrayList<Pair<Pattern, String>>();

    public static void preparePatterns(){
        if (patternList.size() != 0)
            return;

        //Map<String,String> bbMap = new HashMap<String , String>();
        List<Pair<String, String>> bbList = new ArrayList<Pair<String, String>>();

        //Pattern.compile("\\[b\\]([\\s\\S]+?)\\[\\/b\\]")
        bbList.add(new Pair<String, String>("(\r\n|\r|\n|\n\r)", "<br/>"));
        bbList.add(new Pair<String, String>("(?i)\\[b\\]([\\s\\S]+?)\\[\\/b\\]", "<strong>$1</strong>"));
        bbList.add(new Pair<String, String>("(?i)\\[i\\]([\\s\\S]+?)\\[\\/i\\]", "<span style='font-style:italic;'>$1</span>"));
        bbList.add(new Pair<String, String>("(?i)\\[u\\]([\\s\\S]+?)\\[\\/u\\]", "<span style='text-decoration:underline;'>$1</span>"));
        bbList.add(new Pair<String, String>("(?i)\\[h1\\]([\\s\\S]+?)\\[\\/h1\\]", "<h1>$1</h1>"));
        bbList.add(new Pair<String, String>("(?i)\\[h2\\]([\\s\\S]+?)\\[\\/h2\\]", "<h2>$1</h2>"));
        //bbList.add(new Pair<String, String>("(?i)\\[h3\\]([\\s\\S]+?)\\[\\/h3\\]", "<h3>$1</h3>"));
        //bbList.add(new Pair<String, String>("(?i)\\[h4\\]([\\s\\S]+?)\\[\\/h4\\]", "<h4>$1</h4>"));
        //bbList.add(new Pair<String, String>("(?i)\\[h5\\]([\\s\\S]+?)\\[\\/h5\\]", "<h5>$1</h5>"));
        //bbList.add(new Pair<String, String>("(?i)\\[h6\\]([\\s\\S]+?)\\[\\/h6\\]", "<h6>$1</h6>"));
        bbList.add(new Pair<String, String>("(?i)\\[quote\\]([\\s\\S]+?)\\[\\/quote\\]", "<blockquote>$1</blockquote>"));
        bbList.add(new Pair<String, String>("(?i)\\[p\\]([\\s\\S]+?)\\[\\/p\\]", "<p>$1</p>"));
        //bbList.add(new Pair<String, String>("(?i)\\[p=([\\s\\S]+?),([\\s\\S]+?)\\]([\\s\\S]+?)\\[\\/p\\]", "<p style='text-indent:$1px;line-height:$2%;'>$3</p>"));
        //bbList.add(new Pair<String, String>("(?i)\\[center\\]([\\s\\S]+?)\\[\\/center\\]", "<div align='center'>$1"));
        //bbList.add(new Pair<String, String>("(?i)\\[align=([\\s\\S]+?)\\](.+?)\\[\\/align\\]", "<div align='$1'>$2"));
        bbList.add(new Pair<String, String>("(?i)\\[color=([\\s\\S]+?)\\](.+?)\\[\\/color\\]", "<span style='color:$1;'>$2</span>"));
        bbList.add(new Pair<String, String>("(?i)\\[size=([\\s\\S]+?)\\](.+?)\\[\\/size\\]", "<span style='font-size:$1;'>$2</span>"));
        bbList.add(new Pair<String, String>("(?i)\\[img\\]([\\s\\S]+?)\\[\\/img\\]", "<img src='$1' />"));
        bbList.add(new Pair<String, String>("(?i)\\[img=(.+?),(.+?)\\]([\\s\\S]+?)\\[\\/img\\]", "<img width='$1' height='$2' src='$3' />"));
        //bbList.add(new Pair<String, String>("(?i)\\[email\\](.+?)\\[\\/email\\]", "<a href='mailto:$1'>$1</a>"));
        //bbList.add(new Pair<String, String>("(?i)\\[email=(.+?)\\](.+?)\\[\\/email\\]", "<a href='mailto:$1'>$2</a>"));
        bbList.add(new Pair<String, String>("(?i)\\[url\\](.+?)\\[\\/url\\]", "<a href='$1'>$1</a>"));
        bbList.add(new Pair<String, String>("(?i)\\[url=\"(.+?)\"\\](.+?)\\[\\/url\\]", "<a href='$1'>$2</a>"));
        bbList.add(new Pair<String, String>("(?i)\\[url=(.+?)\\](.+?)\\[\\/url\\]", "<a href='$1'>$2</a>"));
        bbList.add(new Pair<String, String>("(?i)\\[youtube\\](.+?)\\[\\/youtube\\]", "<object width='640' height='380'><param name='movie' value='http://www.youtube.com/v/$1'></param><embed src='http://www.youtube.com/v/$1' type='application/x-shockwave-flash' width='640' height='380'></embed></object>"));
        bbList.add(new Pair<String, String>("(?i)\\[video\\](.+?)\\[\\/video\\]", "<video src='$1' />"));

        for (Pair<String, String> entry: bbList) {
            patternList.add(new Pair<Pattern, String>(Pattern.compile(entry.first), entry.second));
            //patternMap.put(Pattern.compile(entry.getKey().toString()), entry.getValue().toString());
        }
    }

    public static String bbcode(String text) {
        String html = text;
        preparePatterns();


        for (Pair<Pattern, String> entry : patternList) {
            html = entry.first.matcher(html).replaceAll(entry.second);
            //html = html.replaceAll(entry.getKey().toString(), entry.getValue().toString());
        }

        return html;
    }

    public static Map<String, String> parseTagAttribs(XMLReader xmlReader){
        try {
            Field elementField = xmlReader.getClass().getDeclaredField("theNewElement");
            elementField.setAccessible(true);
            Object element = elementField.get(xmlReader);
            Field attsField = element.getClass().getDeclaredField("theAtts");
            attsField.setAccessible(true);
            Object atts = attsField.get(element);
            Field dataField = atts.getClass().getDeclaredField("data");
            dataField.setAccessible(true);
            String[] data = (String[])dataField.get(atts);
            Field lengthField = atts.getClass().getDeclaredField("length");
            lengthField.setAccessible(true);
            int len = (Integer)lengthField.get(atts);

            Map<String, String> params = new HashMap<String, String>();
            for(int i = 0; i < len; i++) {
                params.put(data[i * 5 + 1], data[i * 5 + 4]);
                    /*if("attrA".equals(data[i * 5 + 1])) {
                        myAttributeA = data[i * 5 + 4];
                    } else if("attrB".equals(data[i * 5 + 1])) {
                        myAttributeB = data[i * 5 + 4];
                    }*/
            }
            return params;
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }
        return null;
    }

    public static String getHtml(String src) throws IOException
    {
        HttpClient httpClient = new DefaultHttpClient();
        HttpContext localContext = new BasicHttpContext();
        HttpGet httpGet = new HttpGet(src);
        BasicResponseHandler responseHandler = new BasicResponseHandler();
        return httpClient.execute(httpGet, responseHandler);

        /*BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        response.getEntity().getContent()
                )
        );

        String line = null;
        while ((line = reader.readLine()) != null){
            result += line + "\n";
            Toast.makeText(activity.this, line.toString(), Toast.LENGTH_LONG).show();

        }*/

    }


    public static Drawable makeDrawableFromBitmap(Context context, Bitmap bm, int maxWidth){
        /*float sizeMult = context.getResources().getDisplayMetrics().density * 1.5f;
        if (maxWidth != 0 && bm.getWidth() >= maxWidth / sizeMult)
            sizeMult = context.getResources().getDisplayMetrics().density;*/
        Point size = getSizeForImage(bm.getWidth(), bm.getHeight(), context, maxWidth);
        Drawable d = new BitmapDrawable(bm);
        d.setBounds(0, 0, size.x, size.y);
        return d;
    }

    public static Point getSizeForImage(int width, int height, Context context, int maxWidth){
        float sizeMult = context.getResources().getDisplayMetrics().density ;
        //if (maxWidth != 0 && width >= maxWidth / sizeMult)
        //    sizeMult = context.getResources().getDisplayMetrics().density;
        if (maxWidth != 0 && width * sizeMult > maxWidth)
            sizeMult = maxWidth / (float)width;

        return new Point((int) (sizeMult * width), (int) (sizeMult * height));
    }

    public static byte[] readFullyAsByteArray(File file) {
        try {
            InputStream inputStream = new FileInputStream(file);
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

    public static String firstNWords(String source, final int n){
        int pos = 0;
        for (int i=0; i<n; i++){
            pos = source.indexOf(" ", pos+1);
            if (pos == -1)
                return source;
        }
        return source.substring(0, pos);
    }

    public static File getInternalStorageDir(Context context, String subdir) {
        File file = context.getFilesDir();
        file.mkdir();
        file = new File(file, subdir);
        file.mkdir();
        return file;
    }

    public static int getInternalStorageFreeSpace(Context context) {
        try {
            File file = getInternalStorageDir(context, "images");
            StatFs stats = new StatFs("/data");
            int availableBlocks = stats.getAvailableBlocks();
            int blockSizeInBytes = stats.getBlockSize();
            return availableBlocks * blockSizeInBytes;
        } catch (Exception e) {
            return 0;
        }
    }

    static public boolean hasExternalStorage(boolean requireWriteAccess) {
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
}