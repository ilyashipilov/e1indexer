package name.shipilov.e1;

import name.shipilov.crawler.Identified;
import org.json.JSONObject;

/**
 * Обертка дял JSON объекта, для использования как элемента данных в Crawler
 *
 * @see name.shipilov.crawler.Crawler
 * @see Identified
 *
 * Created by ilya on 02.02.2016.
 */
public class Resume implements Identified {

    private final JSONObject value;

    public Resume(JSONObject value) {
        if (value == null)
            throw new NullPointerException("no value");

        this.value = value;
    }

    public JSONObject getValue() {
        return value;
    }

    @Override
    public String getId() {
        return value.get("id").toString();
    }
}
