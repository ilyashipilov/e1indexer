package name.shipilov.crawler;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by ilya on 02.02.2016.
 */
public class CrawlerTest {

    public static class Item implements Identified {
        private Integer value;

        public Item(Integer value) {
            this.value = value;
        }

        @Override
        public String getId() {
            return value.toString();
        }
    }

    @Test
    public void executeTest() throws InterruptedException {

        final ConcurrentLinkedQueue<Item> processed = new ConcurrentLinkedQueue<Item>();

        final Crawler<Item> crawler = new Crawler<Item>(new PagedDataProvider<Item>() {
            @Override
            public int getPageCount() {
                return 100;
            }

            @Override
            public Iterable<Item> getPage(int page) {
                List<Item> result = new LinkedList<Item>();
                for (int i = 0; i < 10; i++) {
                    result.add(new Item(10 * page + i));
                }
                return result;
            }
        }, new Indexer<Item>() {
            @Override
            public void prepare(Item item) throws AlreadyIndexedException {
                if (item.value >= 555)
                    throw new AlreadyIndexedException(item);
                processed.add(item);
            }
        }, new Crawler.Configuration() {
            @Override
            public int queueSize() {
                return 10;
            }

            @Override
            public int loaderPoolSize() {
                return 10;
            }

            @Override
            public int indexerPoolSize() {
                return 10;
            }
        });

        crawler.execute();

        Assert.assertEquals(processed.size(), 555);
    }
}
