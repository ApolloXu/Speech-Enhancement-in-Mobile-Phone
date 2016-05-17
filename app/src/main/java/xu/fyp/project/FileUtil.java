package xu.fyp.project;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {

    // 應用程式儲存檔案的目錄
    public static final String APP_DIR = "project";

    //
    public static boolean isExternalStorageWritable() {
        // get the external storage condition
        String state = Environment.getExternalStorageState();

        // if writable
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }

    // 外部儲存設備是否可讀取
    public static boolean isExternalStorageReadable() {
        // 取得目前外部儲存設備的狀態
        String state = Environment.getExternalStorageState();

        // 判斷是否可讀取
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }

        return false;
    }

    // 建立並傳回在公用相簿下參數指定的路徑


    // 建立並傳回在應用程式專用相簿下參數指定的路徑


    // create and get the external storage path
    public static File getExternalStorageDir(String dir) {
        File result = new File(
                Environment.getExternalStorageDirectory(), dir);

        if (!isExternalStorageWritable()) {
            return null;
        }

        if (!result.exists() && !result.mkdirs()) {
            return null;
        }

        return result;
    }

    // 讀取指定的照片檔案名稱設定給ImageView元件


    // create unique file name
    public static String getUniqueFileName() {
        // use the create time as the file name
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        return sdf.format(new Date());
    }

}
