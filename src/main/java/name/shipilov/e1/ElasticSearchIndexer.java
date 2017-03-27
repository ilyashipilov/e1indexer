package name.shipilov.e1;

import name.shipilov.crawler.Indexer;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Базовая реализация интерфеса индексатора для Elasticsearch
 *
 * Created by ilya on 02.02.2016.
 */
public abstract class ElasticSearchIndexer implements Indexer<Resume> {

    /**
     * Индексатор без проверки что элемент уже проиндексирован
     */
    public static class Full extends ElasticSearchIndexer {
        public Full(Client client) {
            super(client);
        }

        @Override
        protected void checks(Resume item) throws AlreadyIndexedException {
            //do nothing
        }
    }

    /**
     * Индексатор который умеет сигнализировать о том что полученный элемент уже был проиндексирован ранее.
     * Базовый механизм (Crawler) интеллектуально обработает этот сигнал (завершит процесс)
     */
    public static class Incremental extends ElasticSearchIndexer {
        private static Logger logger = LoggerFactory.getLogger(Incremental.class);

        public static final String MODIFY_DATE_FIELD = "mod_date";

        private final Date lastModifiedDate;

        public Incremental(Client client) throws Exception {
            super(client);

            //определяем дату модификации самого свежего из проиндексированных резюме
            final SearchResponse searchResponse = client.prepareSearch(INDEX)
                .setTypes(TYPE)
                .setQuery(QueryBuilders.matchAllQuery())
                .addAggregation(
                    AggregationBuilders.max("lastModifiedDate").field(MODIFY_DATE_FIELD)
                ).execute().get();

            lastModifiedDate = new Date((long)searchResponse.getAggregations().<Max>get("lastModifiedDate").getValue());
            logger.info("last modified [" + lastModifiedDate + "]");

        }

        @Override
        protected void checks(Resume item) throws AlreadyIndexedException {
            // дату "поднятого" не проверяем, т.к. они находятся сверху независимо от сортировки
            if (item.getValue().getBoolean("is_upped"))
                return;

            //дата моификации резюме меньше или равна последней проиндексированной
            if (!ISODateTimeFormat.dateTimeParser().parseDateTime(item.getValue().getString("mod_date")).toDate().after(lastModifiedDate)) {
                throw new AlreadyIndexedException(item);
            }

        }
    }

    private static Logger logger = LoggerFactory.getLogger(ElasticSearchIndexer.class);

    public static final String INDEX = "worksix";
    public static final String TYPE = "resume";

    protected Client client;

    public ElasticSearchIndexer(Client client) {
        if (client == null)
            throw new NullPointerException("no client");

        this.client = client;
    }

    @Override
    public void prepare(Resume item) throws AlreadyIndexedException {
        checks(item);
        try {
            client.prepareIndex(INDEX, TYPE, item.getId()).setSource(item.getValue().toString()).get();
            logger.info("indexed [" + item.getId() + "]");
        } catch (Exception exc) {
            logger.warn("problem with indexation [" + item.getId() + "]: " + exc.getMessage());
        }
    }

    abstract protected void checks(Resume item) throws AlreadyIndexedException;
}
