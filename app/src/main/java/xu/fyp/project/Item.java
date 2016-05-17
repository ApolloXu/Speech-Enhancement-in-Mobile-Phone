package xu.fyp.project;

import java.util.Date;
import java.util.Locale;

public class Item implements java.io.Serializable {

    // 編號、日期時間、顏色、標題、內容、檔案名稱、經緯度、修改、已選擇
    private long id;
    private long datetime;

    private String title;
    private String content;
    private String fileName;
    private String recFileName;

    private long lastModify;
    private boolean selected;
    private String timer;


    public Item() {
        title = "";
        content = "";

    }

    public Item(long id, long datetime, String title,
                String content, String fileName, String recFileName,
                double latitude, double longitude, long lastModify) {
        this.id = id;
        this.datetime = datetime;
        this.title = title;
        this.content = content;
        this.fileName = fileName;
        this.recFileName = recFileName;


        this.lastModify = lastModify;

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDatetime() {
        return datetime;
    }

    // 裝置區域的日期時間
    public String getLocaleDatetime() {
        return String.format(Locale.getDefault(), "%tF  %<tR", new Date(datetime));
    }

    // 裝置區域的日期
    public String getLocaleDate() {
        return String.format(Locale.getDefault(), "%tF", new Date(datetime));
    }

    // 裝置區域的時間
    public String getLocaleTime() {
        return String.format(Locale.getDefault(), "%tR", new Date(datetime));
    }

    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }



    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRecFileName() {
        return recFileName;
    }

    public void setRecFileName(String recFileName) {
        this.recFileName = recFileName;
    }


    public long getLastModify() {
        return lastModify;
    }

    public void setLastModify(long lastModify) {
        this.lastModify = lastModify;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }


}