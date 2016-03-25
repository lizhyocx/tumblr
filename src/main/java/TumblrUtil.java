import java.io.*;
import java.util.*;

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

    public static String getParentFile(String filePath, String userName) {
        if (!filePath.endsWith(File.separator)) {
            filePath += File.separator;
        }
        return filePath + userName;
    }

    public static String getFile(String parentFilePath,String yearMonth) {
        yearMonth = yearMonth.replace("/", "_");

        String fileName = parentFilePath+File.separator+yearMonth+".txt";
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

    public static void mergeFiles(String parentFilePath) {
        Set<String> set = new HashSet<String>();
        File parentFile = new File(parentFilePath);
        if(!parentFile.exists()) {
            System.out.println("parentFile ["+parentFilePath+"] not exist");
            return;
        }
        File[] files = parentFile.listFiles();
        BufferedReader br = null;
        for(File file : files) {
            try{
                br = new BufferedReader(new FileReader(file));
                String str = "";
                while((str = br.readLine()) != null) {
                    set.add(str);
                }
                br.close();
                br = null;
            } catch(IOException e) {
                System.out.println("mergeFiles exception,file="+file.getName());
            }

        }
        String fileName = parentFilePath+File.separator+parentFile.getName()+".txt";
        BufferedWriter bw = null;
        try{
            bw = new BufferedWriter(new FileWriter(fileName));
            for(String str : set) {
                bw.write(str);
                bw.write("\r\n");
                bw.flush();
            }
        } catch(IOException e) {
            System.out.println("merge WriteFile exception,fileName="+fileName);
        }
        if(bw != null) {
            try{
                bw.close();
            } catch(IOException e) {
            }
        }

    }
}
