package name.shipilov.crawler;

/**
 * Интерфейс для выполнения индексации данных.
 *
 * Created by ilya on 02.02.2016.
 */
public interface Indexer<T extends Identified> {

    /**
     * Исключение сигнализирующее о том что элемент уже был проиндексирован
     */
    class AlreadyIndexedException extends Exception {
        public AlreadyIndexedException(Identified element) {
            super("Element [" + element.getId() + "] already indexed");
        }
    }

    /**
     * Индексирует элемент данных
     * @param item
     * @throws AlreadyIndexedException элемент уже был проиндексирован
     */
    void prepare(T item) throws AlreadyIndexedException;
}
