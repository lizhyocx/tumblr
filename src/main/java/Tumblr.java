import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tumblr {
    private static final String video = "https://www.tumblr.com/video/";
    private static final String download = "https://www.tumblr.com/video_file/";
    private static String homeUrl;
    private static String filePath;
    private static int monthNum;
    public static void main(String[] args) throws IOException {
        //args[0]:爬虫的网址：http://xxx.tumblr.com/
        homeUrl = args[0];
        //args[1]:输出结果的文件
        filePath = args[1];
        //args[2]:获取几个月的数据
        monthNum = Integer.parseInt(args[2]);
        final CountDownLatch countDownLatch = new CountDownLatch(monthNum);
        ExecutorService es = Executors.newFixedThreadPool(monthNum);
        List<String> monthList = TumblrUtil.getAllDateByMonth(monthNum);
        for(String month : monthList) {
            final String url = TumblrUtil.getUrl(homeUrl)+"archive/"+month;
            final String fileName = TumblrUtil.getFile(filePath,TumblrUtil.getUsernameByUrl(homeUrl),month);
            es.submit(new Runnable() {
                public void run() {
                    try {
                        Set<String> postList = getAllPostByMonth(url);
                        Set<String> videoList = getAllVideoByMonth(postList);
                        getAllDownload(videoList, fileName);
                        countDownLatch.countDown();
                    } catch(IOException e) {
                        System.out.println("main execute exception url="+url+",error"+e.getMessage());
                    }
                }
            });
        }
        try {
            countDownLatch.await();
            es.shutdown();
            System.out.print("main execute end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 获取其中一个月的所有Post
     * @param url
     */
    private static Set<String> getAllPostByMonth(String url) {
        System.out.println("getAllPostByUrl begin :"+url);
        Set<String> urlPostList = new HashSet<String>();
        String post = TumblrUtil.getUrl(homeUrl)+"post/";
        try {
            Document doc = Jsoup.connect(url).get();
            Elements elements = doc.getElementsByTag("a");
            for(int i=0;i<elements.size();i++) {
                Element e = elements.get(i);
                String aHref = e.attr("href");
                if(StringUtils.isNotEmpty(aHref) && aHref.startsWith(post)) {
                    urlPostList.add(aHref);
                }
            }
        } catch (IOException e) {
            System.out.println("getAllPostByUrl exception:" + url+",error"+e.getMessage());
        }
        System.out.println("getAllPostByUrl end size=:" + urlPostList.size());
        return urlPostList;
    }

    /**
     * 获取一个月的video页面
     * @param urlPostList
     * @return
     */
    private static Set<String> getAllVideoByMonth(Set<String> urlPostList) {
        Set<String> urlVideoList = new HashSet<String>();
        System.out.println("getAllVideoByMonth begin");
        for(String urlPost : urlPostList) {
            try {
                String html = getHtml(urlPost);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.getElementsByTag("iframe");
                for(int i=0;i<elements.size();i++) {
                    Element e = elements.get(i);
                    String src = e.attr("src");
                    if(StringUtils.isNotEmpty(src) && src.startsWith(video)) {
                        urlVideoList.add(src);
                    }
                }
            } catch (Exception e) {
                System.out.println("getAllVideoByMonth exception:"+urlPost+",error"+e.getMessage());
            }
        }
        System.out.println("getAllVideoByMonth end size=" + urlVideoList.size());
        return urlVideoList;
    }

    private static void getAllDownload(Set<String> urlVideoList, String fileName) throws IOException {
        if(urlVideoList.size() == 0) {
            System.out.println("getLAllDownload end empty");
            return;
        }
        File file = new File(fileName);
        TumblrUtil.createNewFile(file);
        BufferedWriter br = new BufferedWriter(new FileWriter(file));
        for(String urlVideo : urlVideoList) {
            try {
                String html = getHtml(urlVideo);
                Document doc = Jsoup.parse(html);
                Elements elements = doc.getElementsByTag("source");
                for(int i=0;i<elements.size();i++) {
                    Element e = elements.get(i);
                    String src = e.attr("src");
                    if(StringUtils.isNotEmpty(src) && src.startsWith(download)) {
                        System.out.println(src);
                        br.write(src);
                        br.write("\r\n");
                        br.flush();
                    }
                }
            } catch (Exception e) {
                System.out.println("getAllDownload exception:"+urlVideo+",error"+e.getMessage());
            }
        }
        br.close();
        System.out.println("getLAllDownload end");
    }

    private static String getHtml(String strUrl) throws Exception {
        CloseableHttpClient httpClient = HttpClients.custom().build();
        HttpGet getMethod = new HttpGet(strUrl);
        CloseableHttpResponse rsp = httpClient.execute(getMethod);
        String str = EntityUtils.toString(rsp.getEntity());
        return str;
    }
}
