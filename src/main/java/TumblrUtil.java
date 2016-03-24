import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Administrator on 2016/3/24.
 */
public class TumblrUtil {
    /**
     * 获取month个月的日期格式：yyyy/M
     * @return
     */
    public static List<String> getAllDateByMonth(int num) {
        List<String> list = new ArrayList<String>();
        Calendar calendar = Calendar.getInstance();
        for(int i=0;i<num;i++) {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH)+1;
            calendar.add(Calendar.MONTH, -1);
            list.add(year+"/"+month);
        }
        return list;
    }

    public static String getUsernameByUrl(String url) {
        if(url.startsWith("http://")) {
            url = url.substring(7);
        } else if(url.startsWith("https://")) {
            url = url.substring(8);
        }
        int index = url.indexOf(".");
        String username = url.substring(0, index);
        return username;
    }

    public static String getUrl(String url) {
        if(!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    public static String getFile(String filePath, String userName, String yearMonth) {
        yearMonth = yearMonth.replace("/", "_");
        if (!filePath.endsWith(File.separator)) {
            filePath += File.separator;
        }
        String fileName = filePath+userName+File.separator+yearMonth+".txt";
        return fileName;
    }

    public static boolean createNewFile(File file) throws IOException {
        if(! file.exists()) {
            makeDir(file.getParentFile());
        }
        return file.createNewFile();
    }

    private static void makeDir(File dir) {
        if(! dir.getParentFile().exists()) {
            makeDir(dir.getParentFile());
        }
        dir.mkdir();
    }
}
