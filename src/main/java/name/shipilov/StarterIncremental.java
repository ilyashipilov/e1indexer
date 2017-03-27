package name.shipilov;

import name.shipilov.crawler.Crawler;
import name.shipilov.e1.ElasticSearchIndexer;
import name.shipilov.e1.Resume;
import name.shipilov.e1.ResumeDataProvider;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Properties;

/**
 * Инкрементальный режим индексации.
 *
 * Резюме запрашиваются без фильтрации и с сортировкой по убыванию даты изменения.
 * Такой порядок позволяет определить момент когда данные полностью синхронизированы, и остановить дальнейшую загрузку.
 *
 * Этот режим мог бы быть единственным, но оказалось что на E1 есть ограничение по запросу страницы разюме -
 * смещение не должно превышать 10000 записей.
 *
 * Можно было бы разделить по рубрикам и инкрементально индексировать по каждой рубрике, но резюме может быть включено
 * более чем в однй рубрику.
 *
 * По этим причинам реализовано 2 режима индексации - настоящий и режим индексации по рубрикам.
 *
 * @see Crawler
 * @see StarterFull
 *
 * Created by ilya on 03.02.2016.
 */
public class StarterIncremental {

    public static void main(String[] args) throws Exception {

        final Properties config = new Properties();
        config.load(StarterIncremental.class.getResourceAsStream("/config.properties"));

        final Client client = TransportClient.builder().build()
            .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.getProperty("es.host")), Integer.valueOf(config.getProperty("es.port"))));

        final ElasticSearchIndexer indexer = new ElasticSearchIndexer.Incremental(client);

        final ResumeDataProvider dataProvider = new ResumeDataProvider(config.getProperty("dataProvider.urlTemplate.incremental"), Integer.valueOf(config.getProperty("dataProvider.pageSize")));

        final Crawler<Resume> crawler = new Crawler<Resume>(dataProvider, indexer, new Crawler.Configuration() {
            @Override
            public int queueSize() {
                return Integer.valueOf(config.getProperty("crawler.queueSize"));
            }
            @Override
            public int loaderPoolSize() {
                return Integer.valueOf(config.getProperty("crawler.loaderPoolSize"));
            }
            @Override
            public int indexerPoolSize() {
                return Integer.valueOf(config.getProperty("crawler.indexerPoolSize"));
            }
        });

        crawler.execute();

    }
}

