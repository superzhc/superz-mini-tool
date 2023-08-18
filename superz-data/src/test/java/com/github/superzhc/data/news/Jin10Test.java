package com.github.superzhc.data.news;

import com.github.superzhc.core.utils.PrintUtils;

import static org.junit.Assert.*;

public class Jin10Test {

    @org.junit.Before
    public void setUp() throws Exception {
    }

    @org.junit.Test
    public void news() {
        PrintUtils.print(Jin10.news());
    }
}