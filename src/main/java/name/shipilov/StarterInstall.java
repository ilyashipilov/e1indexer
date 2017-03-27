package name.shipilov;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;

/**
 * Создает или пересоздает индекс со всеми настройками
 *
 * Created by ilya on 03.02.2016.
 */
public class StarterInstall {
    private static Logger logger = LoggerFactory.getLogger(StarterInstall.class);

    public static final String INDEX = "worksix";

    public static void main(String[] argv) throws IOException {
        final Properties config = new Properties();
        config.load(StarterInstall.class.getResourceAsStream("/config.properties"));

        final Client client = TransportClient.builder().build()
            .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(config.getProperty("es.host")), Integer.valueOf(config.getProperty("es.port"))));

        final IndicesExistsResponse res = client.admin().indices().prepareExists(INDEX).execute().actionGet();
        if (res.isExists()) {
            logger.info("delete existed index");
            final DeleteIndexRequestBuilder delIdx = client.admin().indices().prepareDelete(INDEX);
            delIdx.execute().actionGet();
        }

        client.admin().indices().prepareCreate(INDEX).setSource(IOUtils.toString(StarterIncremental.class.getResourceAsStream("/install.json"))).execute().actionGet();
        logger.info("created [" + INDEX + "]");
        logger.info("open http://localhost:9200/_cat/indices?v t monitoring");

    }
}
