import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.net.ssl.SSLContext;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tumblr {
    private static final String video = "/video/";
    private static final String download = "/video_file/";
    private static String homeUrl;
    private static String filePath;
    private static Integer monthNum;
    private static boolean proxy = false;
    public static void main(String[] args) throws IOException {
        System.out.println("请输入需要爬虫的tumblr博客名：");
        Scanner scanner = new Scanner(System.in);
        homeUrl = "https://".concat(scanner.next()).concat(".tumblr.com");
        System.out.println("请输入结果输出文件(默认E:\\文件\\tumblr\\，输入0)：");
        filePath = scanner.next();
        if("0".equals(filePath)) {
            filePath = "E:\\\\文件\\\\tumblr\\\\";
        }
        System.out.println("请输入爬取年月(yyyy/M)，多个用逗号分隔:");
        String monthStr = scanner.next();
        System.out.println("是否使用代理(127.0.0.1:1080)?y/n");
        String str = scanner.next();
        if("y".equalsIgnoreCase(str)) {
            proxy = true;
        }
        List<String> monthList = TumblrUtil.getAllDateByMonthString(monthStr);
        monthNum = monthList.size();
        final CountDownLatch countDownLatch = new CountDownLatch(monthNum);
        ExecutorService es = Executors.newFixedThreadPool(monthNum);
        String userName = TumblrUtil.getUsernameByUrl(homeUrl);
        String parentFileName = TumblrUtil.getParentFile(filePath, userName);
        for(String month : monthList) {
            final String url = TumblrUtil.getUrl(homeUrl)+"archive/"+month;
            final String fileName = TumblrUtil.getFile(parentFileName,month);
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
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //合并文件
        System.out.println("main merge files begin");
        TumblrUtil.mergeFiles(parentFileName);
        System.out.print("main execute end");
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
            String html = getHtml(url);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.getElementsByTag("a");
            for(int i=0;i<elements.size();i++) {
                Element e = elements.get(i);
                String aHref = e.attr("href");
                if(StringUtils.isNotEmpty(aHref) && aHref.startsWith(post)) {
                    urlPostList.add(aHref);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getAllPostByMonth exception:" + url+",error"+e.getMessage());
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
                    if(StringUtils.isNotEmpty(src) && src.contains(video)) {
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
            System.out.println("getAllDownload end empty");
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
                    if(StringUtils.isNotEmpty(src) && src.contains(download)) {
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
        System.out.println("getAllDownload end");
    }

    private static String getHtml(String strUrl) throws Exception {
        String str = null;
        if(proxy) {
            String proxyHost = "127.0.0.1";
            int proxyPort = 1080;

            SystemDefaultCredentialsProvider credentialsProvider = new SystemDefaultCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials("", ""));
            CloseableHttpClient httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setSSLSocketFactory(ssl())
                    .build();

            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
            HttpGet getMethod = new HttpGet(strUrl);
            getMethod.setConfig(config);
            CloseableHttpResponse rsp = httpClient.execute(getMethod);
            str = EntityUtils.toString(rsp.getEntity());
        } else {
            CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(ssl()).build();
            HttpGet getMethod = new HttpGet(strUrl);
            CloseableHttpResponse rsp = httpClient.execute(getMethod);
            str = EntityUtils.toString(rsp.getEntity());
        }

        return str;
    }

    private static SSLConnectionSocketFactory ssl() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            //信任所有
            public boolean isTrusted(X509Certificate[] chain,
                                     String authType) throws CertificateException {
                return true;
            }
        }).build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
        return sslsf;
    }
}
