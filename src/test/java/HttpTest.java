import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;
import org.apache.http.util.EntityUtils;

/**
 * Created by lizhiyang on 12/20 020.
 */
public class HttpTest {
    public static void main(String[] args) {
        String url = "https://www.tumblr.com";
        try {
            System.out.println(getHtml(url));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static String getHtml(String strUrl) throws Exception {
        String proxyHost = "127.0.0.1";
        int proxyPort = 1080;

        SystemDefaultCredentialsProvider credentialsProvider = new SystemDefaultCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(proxyHost, proxyPort), new UsernamePasswordCredentials("", ""));
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();

        HttpHost proxy = new HttpHost(proxyHost, proxyPort);
        RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        HttpGet getMethod = new HttpGet(strUrl);
        getMethod.setConfig(config);
        CloseableHttpResponse rsp = httpClient.execute(getMethod);
        String str = EntityUtils.toString(rsp.getEntity());
        return str;
    }
}
