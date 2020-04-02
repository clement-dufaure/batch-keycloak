package re.dufau.keycloak;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@EnableConfigurationProperties
@ConfigurationProperties("re.dufau")
public class App {

        private String proxy;

        public static void main(String[] args) throws IOException {
                SpringApplication.run(App.class, args);
        }

        @Bean
        public OkHttpClient httpClient() {
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                if (proxy != null && !proxy.isEmpty()) {
                        HttpUrl proxyUrl = HttpUrl.parse(proxy);
                        builder = builder.proxy(new Proxy(Proxy.Type.HTTP,
                                        new InetSocketAddress(proxyUrl.host(), proxyUrl.port())));
                }
                return builder.build();
        }

        public String getProxy() {
                return proxy;
        }

        public void setProxy(String proxy) {
                this.proxy = proxy;
        }

}
