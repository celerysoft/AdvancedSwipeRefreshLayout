package com.celerysoft.demo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Celery on 2017/1/25.
 *
 */

public class FakeBackend {
    public static List<String> getStringData(int page) {
        return getStringData(page, 10);
    }

    public static List<String> getStringData(int page, int sizePerPage) {
        if (page < 0) {
            return null;
        }

        ArrayList<String> data = new ArrayList<>();
        for (int i = 0; i < sizePerPage; ++i) {
            data.add(String.valueOf(sizePerPage * page + i));
        }
        return data;
    }
}
