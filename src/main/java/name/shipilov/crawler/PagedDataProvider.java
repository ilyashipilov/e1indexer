package name.shipilov.crawler;

/**
 * Интерфейс доступа к паджинированным данным.
 *
 * Created by ilya on 02.02.2016.
 */
public interface PagedDataProvider<T extends Identified> {

    /**
     * @return количество страниц
     */
    int getPageCount();

    /**
     * @return данные со страницы
     */
    Iterable<T> getPage(int page);

}
