package name.shipilov.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс инкапсулирует логику двух процессов - получение данных и инкрементальную индексацию.
 * Процессы выполняются в отдельных пулах потоков, что в конкретных реализациях может обеспечить независимость от каналов связи.
 *
 * Инкрементальное обновление основано на условии что данные поступают в порядке убывания актуальности,
 * и в момент когда будет обнаружен ранее проиндексированный элемент процессы завершаются.
 *
 * Для обеспечения баланса между загрузкой и индексацией используется очередь ограниченного резмера.
 *
 * @see Indexer
 * @see PagedDataProvider
 *
 * @param <T> конкретный тип данных
 *
 * Created by ilya on 02.02.2016.
 */
public class Crawler<T extends Identified> {

    /**
     * Конфигурация
     */
    public interface Configuration {
        /**
         * @return размер очереди в которую поступают резюме при загрузке и из которой они извлекаются на индексацию
         */
        int queueSize();

        /**
         * @return количество потоков загрузки страниц с резюме
         */
        int loaderPoolSize();

        /**
         * @return количество потоков индексации резюме
         */
        int indexerPoolSize();
    }

    private static Logger logger = LoggerFactory.getLogger(Crawler.class);

    private final PagedDataProvider<T> dataProvider;
    private final Indexer<T> indexer;
    private Configuration configuration;

    /**
     * @param dataProvider
     * @param indexer
     */
    public Crawler(PagedDataProvider<T> dataProvider, Indexer<T> indexer, Configuration configuration) {
        if (dataProvider == null) {
            throw new NullPointerException("no dataProvider");
        }
        if (indexer == null) {
            throw new NullPointerException("no indexer");
        }
        if (configuration == null) {
            throw new NullPointerException("no configuration");
        }

        this.dataProvider = dataProvider;
        this.indexer = indexer;
        this.configuration = configuration;
    }

    /**
     *
     * @throws InterruptedException
     */
    public void execute() throws InterruptedException {

        class Session {
            final AtomicInteger currentPage = new AtomicInteger(0);
            boolean oldDataFound = false;
            boolean loadingComplete = false;
        }

        final Session session = new Session();

        final BlockingQueue<T> queue = new ArrayBlockingQueue<T>(configuration.queueSize());
        final ExecutorService loaderExecutor = Executors.newFixedThreadPool(configuration.loaderPoolSize());
        final ExecutorService indexerExecutor = Executors.newFixedThreadPool(configuration.indexerPoolSize());

        final int pageCount = dataProvider.getPageCount();

        //загрузка
        for (int thread = 0; thread < configuration.loaderPoolSize(); thread++) {
            loaderExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        int targetPage;
                        while (!session.oldDataFound && (targetPage = session.currentPage.getAndIncrement()) < pageCount) {
                            // если выставлен флаг, значит уже была загружена страница с проиндесированным ранее элементом
                            // и дальше загружать не нужно
                            for (T item : dataProvider.getPage(targetPage)) {
                                queue.put(item);
                            }
                        }
                    } catch (InterruptedException ie) {
                        logger.warn("worker interrupted", ie);
                    }
                }
            });
        }

        //индексация
        for (int thread = 0; thread < configuration.indexerPoolSize(); thread++) {
            indexerExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!(queue.isEmpty() && session.loadingComplete)) {
                            try {
                                // нельзя сделать просто take(), т.к. с ним не отличить ситуацию когда загрузка завершена
                                // от ситуации когда данные еще не готовы
                                final T item = queue.poll(1, TimeUnit.SECONDS);
                                if (item != null) {
                                    indexer.prepare(item);
                                }
                            } catch (Indexer.AlreadyIndexedException e) {
                                //дальнейшая загрузка страниц не нужна
                                session.oldDataFound = true;
                            }
                        }
                    } catch (InterruptedException ie) {
                        logger.warn("worker interrupted", ie);
                    }
                }
            });
        }

        loaderExecutor.shutdown();
        loaderExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        logger.info("loading complete");

        session.loadingComplete = true; //после "мягкого" выключения loaderExecutor гарантируется что здесь это будет действительно так
        indexerExecutor.shutdown();
        indexerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        logger.info("indexation complete");
    }
}
