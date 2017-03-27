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
 * Режим индексации по рубрикам.
 *
 * Для указанных в конфигурации рубрик последовательно запускается индексация.
 * Учитывается ограничение сайта E1 - смещение страницы запрашиваемых данных не превышает 10000
 * (т.е. из рубрик загружаются только первые 10000/10 = 1000 страниц, но это почти все данные по категориям)
 *
 * @see StarterIncremental
 *
 * Created by ilya on 03.02.2016.
 */
public class StarterFull {
    private static Logger logger = LoggerFactory.getLogger(StarterFull.class);
    private static final int E1_MAX_REQUEST_OFFSET = 10000;

    public static void main(String[] args) throws Exception {

        final Properties config = new Properties();
        config.load(StarterIncremental.class.getResourceAsStream("/config.properties"));

        final Client client = TransportClient.builder().build()
            .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.getProperty("es.host")), Integer.valueOf(config.getProperty("es.port"))));

        final ElasticSearchIndexer indexer = new ElasticSearchIndexer.Full(client);

        for (String rubricId : config.getProperty("rubrics.for.full.indexation").split(",")) {
            final ResumeDataProvider dataProvider = new ResumeDataProvider(config.getProperty("dataProvider.urlTemplate.full").replace("#RUBRIC_ID#", rubricId), Integer.valueOf(config.getProperty("dataProvider.pageSize"))) {
                @Override
                public int getPageCount() {
                    return Math.min(super.getPageCount(), E1_MAX_REQUEST_OFFSET / Integer.valueOf(config.getProperty("dataProvider.pageSize")));
                }
            };

            logger.info("process rubric ["+rubricId+"] started");

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
            logger.info("process rubric ["+rubricId+"] complete");
        }

    }

}
