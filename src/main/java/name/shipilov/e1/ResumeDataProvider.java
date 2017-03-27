package name.shipilov.e1;

import name.shipilov.crawler.PagedDataProvider;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ilya on 02.02.2016.
 */
public class ResumeDataProvider implements PagedDataProvider<Resume> {

    public static final String PLACEHOLDER_OFFSET = "#OFFSET#";
    public static final String PLACEHOLDER_LIMIT = "#LIMIT#";

    private String urlTemplate;
    private int pageSize;

    /**
     * @param urlTemplate шаблон адреса c #OFFSET# и #LIMIT#
     * @param pageSize размер страницы
     */
    public ResumeDataProvider(String urlTemplate, int pageSize) {
        this.urlTemplate = urlTemplate;
        this.pageSize = pageSize;
    }

    @Override
    public int getPageCount() {
        final int count = new JSONObject(readPage(0)).getJSONObject("metadata").getJSONObject("resultset").getInt("count");
        return Double.valueOf(Math.floor(((double) count) / pageSize)).intValue();
    }

    @Override
    public Iterable<Resume> getPage(int page) {
        JSONObject resumesPage = new JSONObject(readPage(page));

        final JSONArray resumesArray = resumesPage.getJSONArray("resumes");
        final List<Resume> result = new LinkedList<Resume>();

        for (int i = 0; i < resumesArray.length(); i++) {
            result.add(new Resume(resumesArray.getJSONObject(i)));
        }
        return result;
    }

    private String readPage(int page) {
        InputStream in = null;
        try {
            in = new URL(urlTemplate
                .replace(PLACEHOLDER_LIMIT, Integer.toString(pageSize))
                .replace(PLACEHOLDER_OFFSET, Integer.toString(pageSize * page))).openStream();
            return IOUtils.toString( in );
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}
