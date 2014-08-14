package ru.babay.lib.view;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import ru.babay.lib.BugHandler;
import ru.babay.lib.R;
import ru.babay.lib.model.Image;
import ru.babay.lib.transport.CachedFile;
import ru.babay.lib.transport.ImageCache;
import ru.babay.lib.transport.TTL;
import ru.babay.lib.util.Html;
import ru.babay.lib.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: babay
 * Date: 30.12.12
 * Time: 18:25
 */
public class HtmlViewFixTouchConsume extends TextView {
    boolean dontConsumeNonUrlClicks = true;
    boolean linkHit;
    String mHtml;
    Handler handler;
    boolean replaceMore;
    static Pattern morePattern = Pattern.compile("(?i)\\[more=\"(.*)\"\\]");
    static Pattern youtubePattern = Pattern.compile("^.*(youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=|\\&v=)([^#\\&\\?]*).*");
    Map<String, UrlImgInfo> iframeInfoMap;
    boolean useExternalStorage;
    ContentClickListener contentClickListener;

    public List<ImgInfo> getImgInfoList() {
        return imgInfoList;
    }

    //Map<String, ImgInfo> imgInfoMap;
    List<ImgInfo> imgInfoList = new ArrayList<ImgInfo>();
    int maxImageWidth;
    int videoCounter = 0;
    String defImageServer;
    SpannableStringBuilder strBuilder;
    int openEditedCommentDivCounter = 0;
    int openEditedCommentDivStartPos = 0;
    ArrayList<AddSpanInfo> addSpanInfo;
    final ArrayList<CachedFile> downloaders = new ArrayList<CachedFile>();
    boolean isAttachedToWindow;
    private Object strBuilderSync = new Object();

    public HtmlViewFixTouchConsume(Context context) {
        super(context);
        sharedConstructor();
    }

    public HtmlViewFixTouchConsume(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructor();
    }

    public HtmlViewFixTouchConsume(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        sharedConstructor();
    }

    void sharedConstructor() {
        if (isInEditMode()) {
            maxImageWidth = 500;
        } else {
            Rect rc = new Rect();
            getWindowVisibleDisplayFrame(rc);
            maxImageWidth = (int) (rc.width() * .9f);
        }
        handler = new Handler();
        setMovementMethod(LocalLinkMovementMethod.getInstance());
    }

    public void setMaxImageWidth(int maxImageWidth) {
        int oldMaxImageWidth = this.maxImageWidth;
        if (maxImageWidth == this.maxImageWidth)
            return;
        this.maxImageWidth = maxImageWidth;
        if (strBuilder != null) {
            FitImageSpan[] spans = strBuilder.getSpans(0, strBuilder.length(), FitImageSpan.class);
            if (spans != null) {
                for (FitImageSpan imageSpan : spans) {
                    Drawable dr = imageSpan.getDrawable();
                    if (imageSpan.fitWidth && dr instanceof BitmapDrawable) {
                        BitmapDrawable drawable = (BitmapDrawable) dr;

                        int h = dr.getIntrinsicHeight() * maxImageWidth / dr.getIntrinsicWidth();
                        drawable.setBounds(0, 0, maxImageWidth, h);
                    }
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        if (wMode == MeasureSpec.AT_MOST || wMode == MeasureSpec.EXACTLY) {
            int pad = getPaddingLeft() + getPaddingRight();
            setMaxImageWidth(wSize - pad);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void stopLoading() {
        synchronized (downloaders) {
            for (CachedFile cachedFile : downloaders)
                cachedFile.abort();
            downloaders.clear();
        }
    }

    void postDelayedStopLoading() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isAttachedToWindow)
                    stopLoading();
            }
        }, 3000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        linkHit = false;
        try {
            boolean res = super.onTouchEvent(event);
            if (dontConsumeNonUrlClicks)
                return linkHit;
            return res;
        } catch (ActivityNotFoundException e) {
            BugHandler.logD(e);
            return false;
        }
    }

    public void setTextViewHTML(String html) {
        setTextViewHTML(html, true);
    }

    public void setTextViewHTML(String html, boolean downloadImages) {
        stopLoading();
        iframeInfoMap = null;
        imgInfoList.clear();
        html = html.replaceAll("[\\n\\r]", "");
        if (replaceMore) {
            html = morePattern.matcher(html).replaceAll("<br/>$0");
        }
        mHtml = Utils.bbcode(html);

        CharSequence sequence = ru.babay.lib.util.Html.fromHtml(mHtml, getContext(), null, new VideoTagHandler(), urlHandler);

        synchronized (strBuilderSync) {
            strBuilder = new SpannableStringBuilder(sequence);
            if (addSpanInfo != null) {
                for (int i = 0; i < addSpanInfo.size(); i++) {
                    AddSpanInfo s = addSpanInfo.get(i);
                    if (s.start >= 0 && s.end < strBuilder.length())
                        strBuilder.setSpan(s.span, s.start, s.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                addSpanInfo = null;
            }

            try {
                if (replaceMore) {
                    Matcher matcher = morePattern.matcher(sequence);
                    if (matcher.find()) {
                        int start = matcher.start();
                        int end = matcher.end();
                        ForegroundColorSpan span = new ForegroundColorSpan(Color.BLUE);
                        strBuilder.setSpan(span, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        String moreText = matcher.group(1);
                        strBuilder.replace(start, end, moreText);
                    }

                }
            } catch (Exception e) {
                BugHandler.logD(e);
            }

            setText(strBuilder);
        }
    }

    private HandledUrlSpan.UrlHandler urlHandler = new HandledUrlSpan.UrlHandler() {
        @Override
        public boolean onUrlClick(String url, View widget) {
            if (url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".gif")) {
                Image image = new Image(url, 0, 0, TTL.TwoDays);
                if (contentClickListener != null) {
                    contentClickListener.onImageClick(HtmlViewFixTouchConsume.this, image);
                    return true;
                }
            }
            Uri uri = Uri.parse(url);
            Context context = widget.getContext();
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            //intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());

            PackageManager packManager = getContext().getPackageManager();
            List<ResolveInfo> resolvedInfoList = packManager.
                    queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            String packageName = getContext().getPackageName();

            for (ResolveInfo resolveInfo : resolvedInfoList) {
                if (resolveInfo.activityInfo.packageName.startsWith(packageName)) {
                    intent.setClassName(resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name);
                    break;
                }
            }
            getContext().startActivity(intent);

            return true;
        }
    };

    private class VideoTagHandler implements Html.TagHandler {

        @Override
        public boolean handleTag(boolean opening, String tag, Editable output, Attributes attributes, XMLReader xmlReader) {
            if (opening && "iframe".equals(tag)) {
                return parseIframe(attributes, output);
            } else if (opening && "img".equals(tag)) {
                return parseImageTag2(attributes, output);
            } else if ("div".equals(tag)) {
                if (opening && "edited_comment".equals(attributes.getValue("", "class"))) {
                    openEditedCommentDivCounter = 1;
                    openEditedCommentDivStartPos = output.length();
                } else if (!opening && openEditedCommentDivCounter == 1) {
                    int start = openEditedCommentDivStartPos;
                    int end = output.length();
                    addSpanToList(start, end, new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE));
                    addSpanToList(start, end, new ForegroundColorSpan(Color.GRAY));
                    /*int flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
                    output.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE), start, end, flag);
                    output.setSpan(new ForegroundColorSpan(Color.GRAY), start, end, flag);*/
                    openEditedCommentDivCounter = openEditedCommentDivStartPos = 0;
                } else if (openEditedCommentDivCounter > 0)
                    openEditedCommentDivCounter += opening ? 1 : -1;
                return false;
            }

            return false;
        }
    }

    boolean parseImageTag2(Attributes attributes, Editable output) {
        if (attributes == null)
            return false;

        String source = attributes.getValue("", "src");
        if (source == null || source.length() == 0)
            return false;

        final String src = source.substring(0, 1).equals("/") ? defImageServer + source : source;

        String alt = attributes.getValue("", "alt");
        if (alt == null || alt.length() == 0)
            alt = "[IMG]";

        final ImgInfo info = new ImgInfo();
        info.src = src;
        info.start = output.length();
        info.end = info.start + alt.length();
        output.append(alt);

        Bitmap bm = ImageCache.getBitmapFromMemCache(src, maxImageWidth, 0);
        if (bm != null) {
            info.imageDrawable = Utils.makeDrawableFromBitmap(getContext(), bm, maxImageWidth);
            setImageSpan(info, info.imageDrawable, output);
            return true;
        }

        Point size = ImageCache.getImageSize(getContext(), src, TTL.TwoDays); // find image size in cache
        if (size != null) {
            if (size.x < 100 & size.y < 100) { // image is small - load it now
                bm = ImageCache.getImage(getContext(), src, TTL.TwoDays);
                if (bm != null) {
                    info.imageDrawable = Utils.makeDrawableFromBitmap(getContext(), bm, maxImageWidth);
                    setImageSpan(info, info.imageDrawable, output);
                    return true;
                }
            }
            Drawable dr = new ColorDrawable(Color.argb(0, 0, 0, 0));
            Point p = Utils.getSizeForImage(size.x, size.y, getContext(), maxImageWidth);
            dr.setBounds(0, 0, p.x, p.y);
            setImageSpan(info, dr, output);
        } else
            try {
                String widthStr = attributes.getValue("", "width");
                String heightStr = attributes.getValue("", "width");
                int w = Integer.parseInt(widthStr);
                int h = Integer.parseInt(heightStr);
                if (w != 0 && h != 0) {
                    Drawable d = getContext().getResources().getDrawable(R.drawable.empty);
                    Point p = Utils.getSizeForImage(w, h, getContext(), maxImageWidth);
                    d.setBounds(0, 0, p.x, p.y);
                    setImageSpan(info, d, output);
                }
            } catch (Throwable t) {
            }
        if (info.span == null) {
            Drawable dr = new ColorDrawable(Color.argb(0, 0, 0, 0));
            dr.setBounds(0, 0, 50, 50);
            setImageSpan(info, dr, output);
        }

        CachedFile cachedFile = ImageCache.loadImage(getContext(), src, TTL.TwoDays, maxImageWidth, useExternalStorage, new ImageCache.BitmapReceiver() {
            @Override
            public void onBitmapReceived(Bitmap bm, CachedFile downloader) {
                synchronized (downloaders) {
                    downloaders.remove(downloader);
                }
                info.imageDrawable = Utils.makeDrawableFromBitmap(getContext(), bm, maxImageWidth);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (strBuilderSync) {
                            setImageSpan(info, info.imageDrawable, strBuilder);
                            setText(strBuilder);
                        }

                        //replaceImageSpan(info.pos, info.len, info.imageDrawable, dummyImageSpan, null);
                        //addImgSpan(info);
                    }
                });
            }

            @Override
            public void onFail(Throwable e, CachedFile downloader) {
                synchronized (downloaders) {
                    downloaders.remove(downloader);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Drawable dr = new ColorDrawable(Color.argb(128, 128, 0, 0));
                        dr.setBounds(0, 0, 50, 50);
                        synchronized (strBuilderSync) {
                            setImageSpan(info, dr, strBuilder);
                        }
                    }
                });
            }
        });
        synchronized (downloaders) {
            downloaders.add(cachedFile);
        }
        return true;
    }

    void setImageSpan(final ImgInfo info, Drawable dr, Editable output) {
        if (info.start < 0 || info.end > output.length())
            return;
        if (info.span != null) {
            output.removeSpan(info.span);
        } else {
            ClickableSpan span = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (strBuilder != null) {
                        int start = strBuilder.getSpanStart(this);
                        int end = strBuilder.getSpanEnd(this);
                        HandledUrlSpan[] spans = strBuilder.getSpans(start, end, HandledUrlSpan.class);
                        if (spans != null && spans.length > 0) {
                            spans[0].onClick(widget);
                            return;
                        }
                    }
                    if (contentClickListener != null) {
                        contentClickListener.onImageClick(HtmlViewFixTouchConsume.this, new Image(info.getSrc(), 0, 0, TTL.TwoDays));
                    }
                }
            };
            output.setSpan(span, info.start, info.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        QuoteSpan[] quoteSpans = output.getSpans(info.start, info.end, QuoteSpan.class);
        if (quoteSpans != null && quoteSpans.length > 0) {
            Rect rect = dr.getBounds();
            //rect.right = rect.right * 3  / 4;
            //rect.bottom = rect.bottom * 3 / 4;
            int marginWidth = 0;
            for (int i = 0; i < quoteSpans.length; i++)
                marginWidth += quoteSpans[i].getLeadingMargin(i == 0);
            int newWidth = rect.right - marginWidth;
            int newHeight = rect.bottom * newWidth / rect.right;
            dr.setBounds(0, 0, newWidth, newHeight);
        }
        info.span = new FitImageSpan(dr);
        if (dr.getBounds().width() == maxImageWidth)
            info.span.fitWidth = true;
        output.setSpan(info.span, info.start, info.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    boolean parseIframe(Attributes attributes, Editable output) {
        if (iframeInfoMap == null)
            iframeInfoMap = new HashMap<String, UrlImgInfo>();

        if (attributes == null || attributes.getValue("", "src") == null)
            return false;

        String src = attributes.getValue("", "src");

        if (iframeInfoMap.containsKey(src)) {
            UrlImgInfo info = iframeInfoMap.get(src);
            info.pos = output.length();
            output.append("[video]");
            if (info.imageDrawable != null)
                addUrlSpan(info);
            return true;
        }

        return parseYoutubeVideo(src, output);
    }

    boolean parseYoutubeVideo(String src, Editable output) {
        String videoId = null;
        Matcher m = youtubePattern.matcher(src);
        if (m.find() && m.groupCount() >= 2) {
            videoId = m.group(2);
        }

        if (videoId != null) {
            final UrlImgInfo info = new UrlImgInfo();
            if (src.startsWith("//"))
                src = "http:" + src;
            info.id = videoCounter++;
            info.src = src;
            info.imageSrc = String.format("http://img.youtube.com/vi/%s/0.jpg", m.group(2));
            info.pos = output.length();
            output.append("[video]");
            iframeInfoMap.put(src, info);

            Drawable dr = getContext().getResources().getDrawable(R.drawable.empty);
            dr.setBounds(0, 0, maxImageWidth, maxImageWidth * 3 / 4);
            final Object dummySpan = replaceImageSpan(info.pos, info.len, dr, null, output);

            CachedFile cachedFile = ImageCache.loadImage(getContext(), info.imageSrc, TTL.Day, 0, new ImageCache.BitmapReceiver() {
                @Override
                public void onBitmapReceived(Bitmap bm, CachedFile downloader) {
                    synchronized (downloaders) {
                        downloaders.remove(downloader);
                    }
                    info.imageDrawable = new BitmapDrawable(bm);
                    int height = (int) (maxImageWidth / (float) bm.getWidth() * bm.getHeight());
                    info.imageDrawable.setBounds(0, 0, maxImageWidth, height);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            replaceUrlSpan(info.pos, info.len, info.imageDrawable, info.src, dummySpan);
                        }
                    });
                }

                @Override
                public void onFail(Throwable e, CachedFile downloader) {
                    synchronized (downloaders) {
                        downloaders.remove(downloader);
                    }
                }
            });
            synchronized (downloaders) {
                downloaders.add(cachedFile);
            }
            return true;
        }
        return false;
    }

    void addUrlSpan(final UrlImgInfo info) {
        if (iframeInfoMap == null || !iframeInfoMap.containsValue(info))
            return;

        replaceUrlSpan(info.pos, info.len, info.imageDrawable, info.src, null);
    }

    void replaceUrlSpan(int pos, int len, Drawable drawable, final String src, Object oldSpan) {
        synchronized (strBuilderSync) {
            if (strBuilder == null)
                return;
            if (pos < 0 || pos + len > strBuilder.length())
                return;

            if (oldSpan != null)
                strBuilder.removeSpan(oldSpan);

            FitImageSpan span = new FitImageSpan(drawable);
            span.fitWidth = true;
            strBuilder.setSpan(span, pos, pos + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //URLSpan uSpan = new URLSpan(src);

            ClickableSpan cSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (contentClickListener != null)
                        contentClickListener.onVideoClick(HtmlViewFixTouchConsume.this, src);
                    //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(src));
                    //getContext().startActivity(intent);
                }
            };
            strBuilder.setSpan(cSpan, pos, pos + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //e.setSpan(uSpan, pos, pos + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            setText(strBuilder);
        }
    }

    ImageSpan replaceImageSpan(int pos, int len, Drawable drawable, Object oldSpan, Editable e) {
        if (e == null) {
            CharSequence text = getText();
            e = (text instanceof Editable) ? (Editable) text : Editable.Factory.getInstance().newEditable(text);
        }

        if (oldSpan != null)
            e.removeSpan(oldSpan);
        FitImageSpan span = new FitImageSpan(drawable);
        span.fitWidth = true;
        e.setSpan(span, pos, pos + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        setText(e);
        return span;
    }

    public static class LocalLinkMovementMethod extends LinkMovementMethod {
        static LocalLinkMovementMethod sInstance;

        public static LocalLinkMovementMethod getInstance() {
            if (sInstance == null)
                sInstance = new LocalLinkMovementMethod();
            return sInstance;
        }

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(widget);
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]));
                    }

                    if (widget instanceof HtmlViewFixTouchConsume) {
                        ((HtmlViewFixTouchConsume) widget).linkHit = true;
                    }
                    return true;
                } else {
                    Selection.removeSelection(buffer);
                    Touch.onTouchEvent(widget, buffer, event);
                    return false;
                }
            } else
                return Touch.onTouchEvent(widget, buffer, event);
        }
    }

    public void setReplaceMore(boolean replaceMore) {
        this.replaceMore = replaceMore;
    }

    public void setDefImageServer(String defImageServer) {
        this.defImageServer = defImageServer;
    }

    private static class UrlImgInfo {
        int id;
        String src;
        String imageSrc;
        Drawable imageDrawable;
        int pos;
        int len = 7;
    }

    public static class ImgInfo {
        String src;
        Drawable imageDrawable;
        int pos;
        int len = 7;
        int start;
        int end;
        FitImageSpan span;

        public String getSrc() {
            return src;
        }

        public Drawable getImageDrawable() {
            return imageDrawable;
        }
    }

    private class FitImageSpan extends ImageSpan {
        boolean fitWidth;

        private FitImageSpan(Drawable d) {
            super(d);
        }
    }

    private static class AddSpanInfo {
        int start, end;
        Object span;

        private AddSpanInfo(int start, int end, Object span) {
            this.start = start;
            this.end = end;
            this.span = span;
        }
    }

    void addSpanToList(int start, int end, Object span) {
        if (addSpanInfo == null)
            addSpanInfo = new ArrayList<AddSpanInfo>();
        addSpanInfo.add(new AddSpanInfo(start, end, span));
    }

    ImgInfo findInList(String src, int pos) {
        for (ImgInfo info : imgInfoList)
            if (info.pos == pos && src.equals(info.src))
                return info;
        return null;
    }

    public void setContentClickListener(ContentClickListener contentClickListener) {
        this.contentClickListener = contentClickListener;
    }

    @Override
    protected void onDetachedFromWindow() {
        isAttachedToWindow = false;
        super.onDetachedFromWindow();
        postDelayedStopLoading();
    }

    @Override
    protected void onAttachedToWindow() {
        isAttachedToWindow = true;
        super.onAttachedToWindow();
    }

    public interface ContentClickListener {
        public void onImageClick(View view, Image image);

        public void onVideoClick(View view, String url);
    }
}
