package com.github.superzhc.jdbc.jdbi;

import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class Sqlite3Test {
    String path="D:\\notebook\\notebook.db";

    Jdbi jdbi;

    @Before
    public void setUp(){
        jdbi=Jdbi.create(String.format("jdbc:sqlite:%s",path));
    }

    @Test
    public void testInsert(){
        final String sql="INSERT INTO h_report (report_day, building, content, schedule) VALUES(:date, :building, :content, :schedule)";

        final Map<String,Object> data=new HashMap<>();
        data.put("date","2023-08-17");
        data.put("building","");
        data.put("content","");
        data.put("schedule","");

        jdbi.useHandle(handle -> {
            handle.createUpdate(sql).bindMap(data).execute();
        });
    }
}
